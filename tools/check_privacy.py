"""Check runtime permissions, private providers, and Android backup exclusions."""
import argparse
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = "{http://schemas.android.com/apk/res/android}"
FORBIDDEN = {
    "android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE",
    "android.permission.READ_USER_DICTIONARY", "android.permission.WRITE_USER_DICTIONARY",
    "android.permission.READ_CONTACTS",
    "android.permission.RECORD_AUDIO",  # Voice input is delegated to Google's separate IME.
}
DOMAINS = {
    "root", "file", "database", "sharedpref", "external", "device_root",
    "device_file", "device_database", "device_sharedpref",
}


def check_manifest(path):
    manifest = ET.parse(path).getroot()
    permissions = {e.get(ANDROID + "name") for e in manifest if e.tag.startswith("uses-permission")}
    assert not permissions & FORBIDDEN, f"Forbidden permissions in {path}: {permissions & FORBIDDEN}"
    app = manifest.find("application")
    assert app is not None
    assert app.get(ANDROID + "allowBackup") == "false", path
    assert app.get(ANDROID + "fullBackupContent") == "@xml/backup_rules", path
    assert app.get(ANDROID + "dataExtractionRules") == "@xml/data_extraction_rules", path
    providers = app.findall("provider")
    local = [p for p in providers if p.get(ANDROID + "name", "").endswith("LocalDictionaryProvider")]
    assert len(local) == 1, "Private dictionary provider missing"
    assert local[0].get(ANDROID + "exported") == "false"
    assert local[0].get(ANDROID + "grantUriPermissions") == "false"
    assert not any("GestureFileProvider" in p.get(ANDROID + "name", "") for p in providers)
    ime = next(s for s in app.findall("service") if s.get(ANDROID + "name", "").endswith("LatinIME"))
    assert ime.get(ANDROID + "permission") == "android.permission.BIND_INPUT_METHOD"


def check_exclusions(node):
    assert node is not None
    excluded = {e.get("domain") for e in node.findall("exclude") if e.get("path") == "."}
    assert DOMAINS <= excluded, f"Backup domains not excluded: {DOMAINS - excluded}"
    assert not node.findall("include"), "Unexpected backup inclusion"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--merged", action="store_true")
    args = parser.parse_args()
    manifests = [ROOT / "app/src/main/AndroidManifest.xml"]
    if args.merged:
        merged = list((ROOT / "app/build/intermediates/merged_manifests").glob("debugNoMinify/**/AndroidManifest.xml"))
        assert merged, "Build the debugNoMinify variant before checking its merged manifest"
        manifests += merged
    for manifest in manifests:
        check_manifest(manifest)
    xml = ROOT / "app/src/main/res/xml"
    check_exclusions(ET.parse(xml / "backup_rules.xml").getroot())
    rules = ET.parse(xml / "data_extraction_rules.xml").getroot()
    check_exclusions(rules.find("cloud-backup"))
    check_exclusions(rules.find("device-transfer"))
    for source in (ROOT / "app/src/main/java").rglob("*"):
        if source.suffix in {".java", ".kt"}:
            assert "import android.provider.UserDictionary" not in source.read_text(encoding="utf-8"), source
    for locale in ("ru", "en-US"):
        assert (ROOT / f"app/src/main/assets/dicts/main_{locale}.dict").is_file()
    print(f"Privacy checks passed ({len(manifests)} manifest(s)); RU/EN dictionaries present.")


if __name__ == "__main__":
    main()
