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

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--serial", required=True)
parser.add_argument("--adb", default="adb")
parser.add_argument("--output", type=Path, default=Path(".tools/test-results"))
args = parser.parse_args()
adb = [args.adb, "-s", args.serial]
package = "dev.cupertinokeys.keyboard.debug"
activity = f"{package}/helium314.keyboard.latin.KeyboardTestActivity"
runner = f"{package}/helium314.keyboard.latin.KeyboardSmokeInstrumentation"
args.output.mkdir(parents=True, exist_ok=True)
current = subprocess.check_output(adb + ["shell", "settings", "get", "secure", "default_input_method"], text=True).strip()
if not current.startswith(package + "/"):
    raise SystemExit("Select Cupertino Keys manually as the current keyboard first.")

for phase in ("input", "persistence"):
    process = subprocess.Popen(adb + ["shell", "am", "instrument", "-w", "-r", "-e", "phase", phase, runner],
                               stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding="utf-8")
    try:
        time.sleep(2)
        subprocess.run(adb + ["shell", "am", "start", "-W", "-n", activity], check=True,
                       stdout=subprocess.DEVNULL, timeout=20)
        output, _ = process.communicate(timeout=60)
    except BaseException:
        # Finish only our test process; do not leave a hung instrumentation session.
        subprocess.run(adb + ["shell", "am", "force-stop", package], check=False)
        process.kill()
        process.communicate()
        raise
    (args.output / f"device-{phase}.txt").write_text(output, encoding="utf-8")
    print(output)
    if process.returncode or "INSTRUMENTATION_CODE: -1" not in output or "FAILED:" in output:
        raise SystemExit(f"Device phase {phase} failed; see saved output.")

subprocess.run(adb + ["shell", "am", "start", "-W", "-n", activity], check=True, stdout=subprocess.DEVNULL)
print("Device checks passed. Test word removed; Cupertino Keys remains selected.")
