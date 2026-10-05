"""Exercise the release APK using Android's accessibility tree, not fixed screen coordinates."""
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

PACKAGE = "de.haberland.meitueftler"
OUTPUT = Path("screenshots")
OUTPUT.mkdir(exist_ok=True)


def adb(*args):
    return subprocess.check_output(["adb", *args], text=True)


def tree():
    adb("shell", "uiautomator", "dump", "/sdcard/workshop-ui.xml")
    return ET.fromstring(adb("shell", "cat", "/sdcard/workshop-ui.xml"))


def node(prefix, attr="text", timeout=15):
    end = time.monotonic() + timeout
    while time.monotonic() < end:
        for item in tree().iter("node"):
            if item.get(attr, "").strip().casefold().startswith(prefix.strip().casefold()):
                return item
        time.sleep(0.5)
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
    tap("10 Murmel-Aufgaben")
    node("  Deine Murmel-Aufgaben")
    tap("1 · Die erste Rampe")
    node("Baufläche", "content-desc")


adb("shell", "wm", "size", "1280x800")
adb("shell", "wm", "density", "160")
adb("logcat", "-c")
adb("install", "-r", "apk/app-debug.apk")
adb("shell", "pm", "clear", PACKAGE)
adb("shell", "am", "start", "-W", "-n", f"{PACKAGE}/.MainActivity")
node("MeiTüftler")
screenshot("menu")
tap("10 Murmel-Aufgaben")
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
adb("shell", "am", "start", "-W", "-n", f"{PACKAGE}/.MainActivity")
node("Ohne Zeitdruck. Jede Idee darf ausprobiert werden.   ·   3 / 30 Sterne")
open_first_level()
node("Bretter: 1 / 3")
screenshot("restored")
adb("shell", "wm", "size", "800x1280")
node("Baufläche", "content-desc")
time.sleep(1)
screenshot("portrait")
# The system Back action must return to the menu, not exit from the workshop.
adb("shell", "input", "keyevent", "4")
node("MeiTüftler")
assert adb("shell", "pidof", PACKAGE).strip()
log = adb("logcat", "-d", "-b", "crash")
(OUTPUT / "crash.log").write_text(log)
assert "FATAL EXCEPTION" not in log, log
print("PASS: menus, drag, resize, solve, persistence, portrait rotation and system Back")
