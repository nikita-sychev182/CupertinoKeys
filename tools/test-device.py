"""Run debug-only IME smoke checks on an explicitly selected Android device.

Install debugNoMinify and manually select Cupertino Keys before running this script.
MIUI may block instrumentation's background Activity launch; ADB brings only our
disposable test Activity forward. Touches are dispatched to our own IME View,
not injected through Android's system input dispatcher.
"""
import argparse
from pathlib import Path
import subprocess
import time
import queue
import threading

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--serial", required=True)
parser.add_argument("--adb", default="adb")
parser.add_argument("--output", type=Path, default=Path(".tools/test-results"))
parser.add_argument("--screenshots", action="store_true", help="Capture light EN and dark RU previews in our disposable screen")
parser.add_argument("--phase", choices=("geometry", "navigation", "voice", "clipboard", "clipboard_access", "suggestions", "blur_warm", "blur_cool", "blur_fallback", "emoji_light", "emoji_dark", "work_setup", "work_cleanup"),
                    help="Run a targeted check; work_setup deliberately retains one disposable dictionary word")
args = parser.parse_args()
if args.screenshots and args.phase:
    parser.error("Use either --screenshots or --phase")
capture_preview = args.screenshots or bool(args.phase and (args.phase.startswith(("emoji_", "blur_")) or args.phase == "clipboard_access"))
adb = [args.adb, "-s", args.serial]
package = "dev.cupertinokeys.keyboard.debug"
activity = f"{package}/helium314.keyboard.latin.KeyboardTestActivity"
runner = f"{package}/helium314.keyboard.latin.KeyboardSmokeInstrumentation"
args.output.mkdir(parents=True, exist_ok=True)
current = subprocess.check_output(adb + ["shell", "settings", "get", "secure", "default_input_method"], text=True, timeout=15).strip()
if not current.startswith(package + "/"):
    raise SystemExit("Select Cupertino Keys manually as the current keyboard first.")

for phase in ((args.phase,) if args.phase else (("preview_light", "preview_dark") if args.screenshots else ("input", "persistence"))):
    process = subprocess.Popen(adb + ["shell", "am", "instrument", "-w", "-r", "-e", "phase", phase, runner],
                               stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding="utf-8")
    lines = []
    try:
        time.sleep(3)
        subprocess.run(adb + ["shell", "am", "start", "-W", "-n", activity], check=True,
                       stdout=subprocess.DEVNULL, timeout=20)
        if capture_preview:
            ready = queue.Queue()
            def read_preview():
                for line in process.stdout:
                    lines.append(line)
                    if "PREVIEW_READY" in line:
                        ready.put(True)
            reader = threading.Thread(target=read_preview, daemon=True)
            reader.start()
            try:
                ready.get(timeout=10)
            except queue.Empty:
                # MIUI can drop an Activity launch while the IME process is restarting.
                # One bounded retry brings our test Activity forward; never retry forever.
                subprocess.run(adb + ["shell", "am", "start", "-W", "-n", activity], check=True,
                               stdout=subprocess.DEVNULL, timeout=20)
                ready.get(timeout=20)
            time.sleep(1)
            png = subprocess.check_output(adb + ["exec-out", "screencap", "-p"], timeout=15)
            (args.output / f"{phase}.png").write_bytes(png)
            process.wait(timeout=25)
            reader.join(timeout=5)
            output = "".join(lines)
        else:
            output, _ = process.communicate(timeout=60)
    except BaseException:
        if capture_preview:
            (args.output / f"device-{phase}-failure.txt").write_text("".join(lines), encoding="utf-8")
        # Finish only our test process; do not leave a hung instrumentation session.
        subprocess.run(adb + ["shell", "am", "force-stop", package], check=False, timeout=15)
        process.kill()
        process.communicate()
        raise
    (args.output / f"device-{phase}.txt").write_text(output, encoding="utf-8")
    print(output)
    if process.returncode or "INSTRUMENTATION_CODE: -1" not in output or "FAILED:" in output:
        raise SystemExit(f"Device phase {phase} failed; see saved output.")

subprocess.run(adb + ["shell", "am", "start", "-W", "-n", activity], check=True, stdout=subprocess.DEVNULL, timeout=20)
print(f"Phase {args.phase} passed." if args.phase else "Previews captured; system theme restored." if args.screenshots else
      "Device checks passed. Test word removed; Cupertino Keys remains selected.")
