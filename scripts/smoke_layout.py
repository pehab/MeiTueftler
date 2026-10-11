"""Scene gestures, undo, custom terrain and persistence on a real Android emulator."""
import json
import re
import time
import xml.etree.ElementTree as ET
from android_ui import PACKAGE, adb, node, tap, tree, screenshot


def saved(key):
    root = ET.fromstring(adb("shell", "run-as", PACKAGE, "cat", "shared_prefs/workshop.xml"))
    return json.loads(next(i.text for i in root.findall("string") if i.get("name") == key))


def drag(desc, start, end):
    item = node(desc, "content-desc")
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", item.get("bounds")))
    scale = min((x2-x1-12)/1000, (y2-y1-12)/600)
    ox, oy = x1+(x2-x1-1000*scale)/2, y1+(y2-y1-600*scale)/2
    points = [str(round(ox+start[0]*scale)), str(round(oy+start[1]*scale)),
              str(round(ox+end[0]*scale)), str(round(oy+end[1]*scale))]
    adb("shell", "input", "swipe", *points, "500")
    time.sleep(0.4)


def near(value, expected):
    assert abs(value-expected) < 5, (value, expected)


def launch():
    adb("shell", "am", "force-stop", PACKAGE)
    adb("shell", "am", "start", "-W", "-n", f"{PACKAGE}/.MainActivity")
    node("MeiTüftler")


adb("shell", "wm", "size", "1280x800")
adb("shell", "wm", "density", "160")
adb("shell", "pm", "clear", PACKAGE)
adb("logcat", "-c")
# Every category edits separate scene markers, persists them, and undoes them.
for category, free, desc, key, start, end in [
    ("1 · Kugelbahnen", "Freier Bauplatz", "Baufläche", "build_-1", (110,60), (240,120)),
    ("2 · Fahrzeuge", "Freie Maschinenwerkstatt", "Fahrzeug-Baufläche", "vehicle_build_-1_v1", (170,445), (700,400)),
    ("3 · Maschinen & Mechanik", "Freie Mechanikwerkstatt", "Mechanik-Baufläche", "mechanics_build_-1_v1", (620,400), (710,440)),
]:
    launch()
    tap(category)
    tap(free)
    tap("Level bearbeiten")
    drag(desc, start, end)
    state = saved(key)
    near(state["parts"][0]["x"] if category.startswith("2") else state["layout"]["sx"], end[0])
    if category.startswith("1"):
        drag(desc, (850,520), (760,430))
        near(saved(key)["layout"]["gx"], 760)
    elif category.startswith("2"):
        before = len(state["layout"]["surfaces"])
        tap("＋ Strecke")
        assert len(saved(key)["layout"]["surfaces"]) == before+1
        drag(desc, (600,450), (750,360))
        near(saved(key)["layout"]["surfaces"][-1][2], 750)
        tap("Strecke löschen")
        assert len(saved(key)["layout"]["surfaces"]) == before
        tap("↶ Zurück")
        assert len(saved(key)["layout"]["surfaces"]) == before+1
    else:
        tap("Fertig · Bauteile bauen")
        tap("＋ Seilwinde")
        drag(desc, (500,260), (300,160))
        tap("Level bearbeiten")
        drag(desc, (300,160), (350,220))
        state = saved(key)
        near(state["layout"]["mounts"][1][0], 350)
        near(state["parts"][0]["x"], 350)
        screenshot("mechanics-custom-mount")
        tap("↶ Zurück")
        state = saved(key)
        near(state["layout"]["mounts"][1][0], 300)
        near(state["parts"][0]["x"], 300)
    screenshot("layout-"+key)
    expected = saved(key)
    # Rotation recreates the toolbar; the scene remains part of the saved build.
    adb("shell", "wm", "size", "800x1280")
    node(desc, "content-desc")
    screenshot("layout-portrait-"+key)
    adb("shell", "wm", "size", "1280x800")
    node(desc, "content-desc")
    assert saved(key) == expected
    launch()
    tap(category)
    tap(free)
    assert saved(key) == expected

# Real menu reaches the new final task; campaign geometry has no editing controls.
launch()
tap("3 · Maschinen & Mechanik")
tap("24 Mechanik-Aufgaben")
tap("24 · Die große Hebemaschine")
node("Mechanik-Baufläche", "content-desc")
assert not any(i.get("text") == "Level bearbeiten" for i in tree().iter("node"))
screenshot("mechanics-final-task")
crashes = adb("logcat", "-d", "-b", "crash")
assert "FATAL EXCEPTION" not in crashes, crashes
print("Free scene editing, terrain, mounted parts, undo, rotation and reload passed", flush=True)
