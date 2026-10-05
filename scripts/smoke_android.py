"""Exercise the release APK using Android's accessibility tree, not fixed screen coordinates."""
import json
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

PACKAGE = "de.haberland.meitueftler"
OUTPUT = Path("screenshots")
OUTPUT.mkdir(exist_ok=True)
launcher_recoveries = 0


def adb(*args):
    return subprocess.check_output(["adb", *args], text=True)


def tree():
    global launcher_recoveries
    adb("shell", "uiautomator", "dump", "/sdcard/workshop-ui.xml")
    root = ET.fromstring(adb("shell", "cat", "/sdcard/workshop-ui.xml"))
    # Some cold emulator boots show an ANR from Pixel Launcher after wm resize.
    # Recover only this exact system app once. Never dismiss a MeiTüftler ANR.
    if any(i.get("package") == "android" and i.get("text") == "Pixel Launcher isn't responding"
           for i in root.iter("node")):
        screenshot("emulator-launcher-anr")
        if launcher_recoveries:
            raise AssertionError("Pixel Launcher failed repeatedly on the emulator")
        close = next(i for i in root.iter("node")
                     if i.get("resource-id") == "android:id/aerr_close")
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", close.get("bounds")))
        adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
        launcher_recoveries += 1
        print("Recovered the emulator's Pixel Launcher ANR; continuing app checks")
        adb("shell", "uiautomator", "dump", "/sdcard/workshop-ui.xml")
        root = ET.fromstring(adb("shell", "cat", "/sdcard/workshop-ui.xml"))
    return root


def node(prefix, attr="text", timeout=15):
    end = time.monotonic() + timeout
    while time.monotonic() < end:
        for item in tree().iter("node"):
            if item.get(attr, "").strip().casefold().startswith(prefix.strip().casefold()):
                return item
        time.sleep(0.5)
    screenshot("failure")
    (OUTPUT / "failure-crash.log").write_text(adb("logcat", "-d", "-b", "crash"))
    (OUTPUT / "failure-ui.xml").write_text(adb("shell", "cat", "/sdcard/workshop-ui.xml"))
    (OUTPUT / "failure-system.log").write_text(adb("logcat", "-d", "-t", "400"))
    (OUTPUT / "failure-activities.log").write_text(adb("shell", "dumpsys", "activity", "activities"))
    raise AssertionError(f"Missing {attr}: {prefix}")


def tap(prefix):
    item = node(prefix)
    assert item.get("enabled") == "true", f"Disabled: {prefix}"
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", item.get("bounds")))
    assert x2 > x1 and y2 > y1, f"Not visible: {prefix}"
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
    time.sleep(0.35)


def screenshot(name):
    with (OUTPUT / f"{name}.png").open("wb") as image:
        subprocess.run(["adb", "exec-out", "screencap", "-p"], stdout=image, check=True)


def open_first_level():
    tap("16 Murmel-Aufgaben")
    node("  Deine Murmel-Aufgaben")
    tap("1 · Die erste Rampe")
    node("Baufläche", "content-desc")


adb("shell", "wm", "size", "1280x800")
adb("shell", "wm", "density", "160")
adb("shell", "input", "keyevent", "224")
adb("shell", "wm", "dismiss-keyguard")
time.sleep(1)
adb("logcat", "-c")
adb("install", "-r", "apk/app-debug.apk")
adb("shell", "pm", "clear", PACKAGE)
# wm resize may leave Pixel Launcher busy or keep its menu above the launched app.
# This is a disposable emulator; stop only the observed launcher before starting the game.
adb("shell", "am", "force-stop", "com.google.android.apps.nexuslauncher")
launch = adb("shell", "am", "start", "-W", "-a", "android.intent.action.MAIN",
             "-c", "android.intent.category.LAUNCHER", "-n", f"{PACKAGE}/.MainActivity")
print(launch, flush=True)
assert "Error" not in launch and "Status: ok" in launch, launch
node("MeiTüftler", timeout=30)
screenshot("menu")
tap("16 Murmel-Aufgaben")
node("  Deine Murmel-Aufgaben")
screenshot("levels")
tap("1 · Die erste Rampe")
board = node("Baufläche", "content-desc")
screenshot("workshop")
tap("＋ Brett")
node("Brett 1 · 20° · 480")
x1, y1, x2, y2 = map(int, re.findall(r"\d+", board.get("bounds")))
scale = min((x2-x1-12)/1000, (y2-y1-12)/600)
ox = x1 + (x2-x1-1000*scale)/2
oy = y1 + (y2-y1-600*scale)/2
coords = [round(ox+500*scale), round(oy+300*scale), round(ox+300*scale), round(oy+230*scale)]
adb("shell", "input", "swipe", *map(str, coords), "650")
tap("＋ Länger")
node("Brett 1 · 20° · 520")
screenshot("built")
tap("▶ Ausprobieren")
node("Geschafft!", timeout=20)
screenshot("solved")
tap("Weiter tüfteln")
node("Brett 1 · 20° · 520")
time.sleep(1)
adb("shell", "am", "force-stop", PACKAGE)
# A 0.1.0 build has no kind field. Its construction and stars must survive the migration.
legacy = ET.fromstring(adb("shell", "run-as", PACKAGE, "cat", "shared_prefs/workshop.xml"))
for entry in legacy.findall("string"):
    if entry.get("name") == "build_0":
        old_build = json.loads(entry.text)
        for piece in old_build:
            piece.pop("kind", None)
        entry.text = json.dumps(old_build)
subprocess.run(["adb", "shell", "run-as", PACKAGE, "tee", "shared_prefs/workshop.xml"],
               input=ET.tostring(legacy), stdout=subprocess.DEVNULL, check=True)
adb("shell", "am", "start", "-W", "-n", f"{PACKAGE}/.MainActivity")
node("Ohne Zeitdruck. Jede Idee darf ausprobiert werden.   ·   3 / 48 Sterne")
open_first_level()
node("Bauteile: 1 / 3")
screenshot("restored")
adb("shell", "wm", "size", "800x1280")
node("Baufläche", "content-desc")
time.sleep(1)
screenshot("portrait")
# The system Back action must return to the menu, not exit from the workshop.
adb("shell", "input", "keyevent", "4")
node("MeiTüftler")
assert adb("shell", "pidof", PACKAGE).strip()
# The second toolbox must be usable, not merely drawn on screen.
adb("shell", "wm", "size", "1280x800")
tap("16 Murmel-Aufgaben")
for _ in range(5):
    if any(i.get("text", "").startswith("11 · Hoch hinaus") for i in tree().iter("node")):
        break
    adb("shell", "input", "swipe", "600", "660", "600", "250", "500")
tap("11 · Hoch hinaus")
board = node("Baufläche", "content-desc")
tap("＋ Trampolin")
node("Trampolin 1 · 0° · 160")
tap("＋ Länger")
tap("＋ Länger")
for _ in range(3):
    tap("Drehen ↷")
node("Trampolin 1 · 15° · 240")
x1, y1, x2, y2 = map(int, re.findall(r"\d+", board.get("bounds")))
scale = min((x2-x1-12)/1000, (y2-y1-12)/600)
ox = x1 + (x2-x1-1000*scale)/2
oy = y1 + (y2-y1-600*scale)/2
coords = [round(ox+500*scale), round(oy+300*scale), round(ox+220*scale), round(oy+400*scale)]
adb("shell", "input", "swipe", *map(str, coords), "650")
screenshot("spring-built")
tap("▶ Ausprobieren")
node("Geschafft!", timeout=20)
screenshot("spring-solved")
tap("Weiter tüfteln")
tap("‹ Menü")
tap("Freier Bauplatz")
tap("＋ Trampolin")
tap("＋ Block")
node("Block 2 · 0° · 160")
tap("Drehen ↷")
tap("＋ Länger")
node("Block 2 · 5° · 200")
screenshot("new-toolbox")
tap("↶ Zurück")
node("Bauteile: 2 / 20")
tap("‹ Menü")
time.sleep(1)
adb("shell", "am", "force-stop", PACKAGE)
adb("shell", "am", "start", "-W", "-n", f"{PACKAGE}/.MainActivity")
node("Ohne Zeitdruck. Jede Idee darf ausprobiert werden.   ·   6 / 48 Sterne")
tap("Freier Bauplatz")
node("Bauteile: 2 / 20")
board = node("Baufläche", "content-desc")
x1, y1, x2, y2 = map(int, re.findall(r"\d+", board.get("bounds")))
adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
node("Block 2 · 5° · 160")
screenshot("new-elements-restored")
log = adb("logcat", "-d", "-b", "crash")
(OUTPUT / "crash.log").write_text(log)
assert "FATAL EXCEPTION" not in log, log
print("PASS: original puzzle, trampoline puzzle, new toolbox, typed persistence, legacy builds, undo, rotation and Back")
