"""Real Android document export/import and the share sheet, isolated from the campaign tests."""
import copy
import json
import subprocess
import time
import xml.etree.ElementTree as ET
from android_ui import PACKAGE, OUTPUT, adb, node, screenshot, tap, tree

FIXTURES = [
    ("marbles", "inventions", "1 · Kugelbahnen", "Meine Erfindungen", "Tauschbahn", [
        dict(kind="FAN", x=500, y=300, length=480, angle=-170, power=3, channel=4),
        dict(kind="DOOR", x=500, y=300, length=320, angle=45, power=2, channel=4)]),
    ("vehicles", "vehicle_inventions", "2 · Fahrzeuge", "Meine Fahrzeuge", "Tauschauto", [
        dict(kind="MOTOR", x=170, y=445, size=24, angle=0, power=3, direction=-1),
        dict(kind="DRIVE", x=170, y=400, size=14, angle=0, power=1, direction=-1)]),
    ("mechanics", "mechanics_inventions", "3 · Maschinen & Mechanik", "Meine Mechanikmaschinen", "Tauschhebel", [
        dict(kind="WINCH", x=300, y=160, bx=420, by=260, size=22, angle=0, power=3, direction=-1, guide=-1),
        dict(kind="ROPE", x=300, y=160, bx=620, by=400, size=20, angle=0, power=2, direction=1, guide=2)])]


def write_private(path, data):
    subprocess.run(["adb", "shell", "run-as", PACKAGE, "tee", path],
                   input=data, stdout=subprocess.DEVNULL, check=True)


def prefs():
    return ET.fromstring(adb("shell", "run-as", PACKAGE, "cat", "shared_prefs/workshop.xml"))


def gallery(key):
    return json.loads(next(i.text for i in prefs().findall("string") if i.get("name") == key))


def launch():
    adb("shell", "am", "force-stop", PACKAGE)
    adb("shell", "am", "start", "-W", "-n", f"{PACKAGE}/.MainActivity")
    node("MeiTüftler")


def go_gallery(fixture):
    launch()
    tap(fixture[2])
    tap(fixture[3])
    node("Maschine importieren")


def incoming(filename, document):
    adb("shell", "run-as", PACKAGE, "mkdir", "-p", "cache/machine-exports")
    write_private("cache/machine-exports/"+filename, json.dumps(document, ensure_ascii=False).encode())
    adb("shell", "am", "start", "-W", "-n", f"{PACKAGE}/.MainActivity", "-a", "android.intent.action.VIEW",
        "-t", "application/json", "-d", f"content://{PACKAGE}.machines/machines/{filename}")


adb("shell", "wm", "size", "1280x800")
adb("shell", "wm", "density", "160")
adb("shell", "input", "keyevent", "224")
adb("shell", "wm", "dismiss-keyguard")
adb("install", "-r", "apk/app-debug.apk")
adb("shell", "pm", "clear", PACKAGE)
adb("shell", "am", "force-stop", "com.google.android.apps.nexuslauncher")
adb("shell", "run-as", PACKAGE, "mkdir", "-p", "shared_prefs")
root = ET.Element("map")
ET.SubElement(root, "boolean", name="sound", value="false")
ET.SubElement(root, "int", name="stars_0", value="3")
ET.SubElement(root, "int", name="vehicle_stars_0", value="2")
ET.SubElement(root, "int", name="mechanics_stars_0", value="1")
for category, key, _, _, name, parts in FIXTURES:
    ET.SubElement(root, "string", name=key).text = json.dumps([
        dict(id="original-"+category, name=name, build=json.dumps(parts))])
write_private("shared_prefs/workshop.xml", ET.tostring(root))
adb("logcat", "-c")

for fixture in FIXTURES:
    category, key, _, _, name, parts = fixture
    go_gallery(fixture)
    screenshot("exchange-"+category+"-gallery")
    # Exercise the actual Android share sheet and receive the file in MeiTüftler.
    tap("Exportieren")
    tap("Teilen")
    node("MeiTüftler")
    screenshot("exchange-"+category+"-share")
    assert "ChooserActivity" in adb("shell", "dumpsys", "activity", "activities")
    cached = adb("shell", "run-as", PACKAGE, "ls", "cache/machine-exports").splitlines()
    cached = next(f for f in cached if f.endswith(name+".meitueftler.json"))
    shared = json.loads(adb("shell", "run-as", PACKAGE, "cat", "cache/machine-exports/"+cached))
    assert shared["format"] == "MeiTueftler-Maschine" and shared["category"] == category
    assert shared["parts"] == parts, shared
    assert "id" not in shared and "stars" not in shared
    tap("MeiTüftler")
    node("Name der importierten Maschine", "content-desc")
    tap("Abbrechen")
    assert len(gallery(key)) == 1  # cancellation must not save anything

    go_gallery(fixture)
    tap("Exportieren")
    tap("Als Datei speichern")
    tap("Downloads")
    node("Save")
    screenshot("exchange-"+category+"-save-picker")
    tap("Save")
    path="/sdcard/Download/"+name+".meitueftler.json"
    end=time.monotonic()+15
    while time.monotonic()<end:
        result=subprocess.run(["adb", "shell", "cat", path], capture_output=True, text=True)
        if result.returncode==0 and result.stdout.strip():break
        time.sleep(0.5)
    exported=json.loads(result.stdout)
    assert exported["category"]==category and exported["parts"]==parts, exported
    assert len(gallery(key))==1
    # Open a real Downloads document through the system picker.
    tap("Maschine importieren")
    tap("Downloads")
    tap(name+".meitueftler.json")
    node("Name der importierten Maschine", "content-desc")
    node(name+" (Import 1)")
    screenshot("exchange-"+category+"-import-preview")
    tap("Importieren")
    node(name+" (Import 1)")
    items=gallery(key)
    assert len(items)==2 and items[0]["id"]=="original-"+category
    assert items[1]["id"]!=items[0]["id"] and json.loads(items[1]["build"])==parts, items
    launch()
    tap(fixture[2]);tap(fixture[3]);node(name+" (Import 1)")
    # Cancelling the document picker also leaves every invention intact.
    tap("Maschine importieren")
    adb("shell", "input", "keyevent", "4")
    node("Maschine importieren")
    assert len(gallery(key))==2
    print("PASS: "+category+" share sheet, receive/cancel, real Downloads export/import, settings, unique identity and restart",flush=True)

# Direct file opening works on a cold launch and routes to its own workshop.
launch()
file=dict(format="MeiTueftler-Maschine",version=1,workshopVersion=1,category="mechanics",name="Tauschhebel",parts=FIXTURES[2][5])
incoming("direct.meitueftler.json",file)
node("Name der importierten Maschine","content-desc")
tap("Importieren")
node("Tauschhebel (Import 2)")
assert len(gallery("mechanics_inventions"))==3
# Bad imports must not change existing machines or earned stars.
before={key:gallery(key) for _,key,*_ in FIXTURES}
bad=copy.deepcopy(file);bad["version"]=99
incoming("future.meitueftler.json",bad)
node("Diese Dateiversion");screenshot("exchange-unsupported-version");tap("OK")
bad=copy.deepcopy(file);bad["parts"][0]["kind"]="UNKNOWN"
incoming("broken.meitueftler.json",bad)
node("Die Datei enthält ungültige");tap("OK")
assert before=={key:gallery(key) for _,key,*_ in FIXTURES}
assert {i.get("name"):i.get("value") for i in prefs().findall("int")}==dict(stars_0="3",vehicle_stars_0="2",mechanics_stars_0="1")

adb("shell", "wm", "size", "390x844")
go_gallery(FIXTURES[2]);node("Exportieren");node("Maschine importieren")
screenshot("exchange-phone-gallery")
adb("shell", "wm", "size", "1280x800")
# The full gallery never silently replaces an invention.
adb("shell", "am", "force-stop", PACKAGE)
root=prefs();entry=next(i for i in root.findall("string") if i.get("name")=="mechanics_inventions")
full=[dict(id=str(i),name="Maschine "+str(i),build=json.dumps(FIXTURES[2][5])) for i in range(20)]
entry.text=json.dumps(full);write_private("shared_prefs/workshop.xml",ET.tostring(root))
launch();incoming("full.meitueftler.json",file)
node("In dieser Galerie sind bereits 20");screenshot("exchange-gallery-full");tap("OK")
assert gallery("mechanics_inventions")==full
crashes=adb("logcat", "-d", "-b", "crash");(OUTPUT/"exchange-crash.log").write_text(crashes)
assert "FATAL EXCEPTION" not in crashes, crashes
print("PASS: direct file opening, unknown versions, damaged parts, full gallery, phone layout, preserved stars and no crashes",flush=True)
