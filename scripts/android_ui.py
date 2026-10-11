"""Shared accessibility-based Android emulator helpers."""
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
    for attempt in range(4):
        adb("shell", "rm", "-f", "/sdcard/workshop-ui.xml")
        dump = adb("shell", "uiautomator", "dump", "/sdcard/workshop-ui.xml")
        if "UI hierchary dumped to" in dump:
            root = ET.fromstring(adb("shell", "cat", "/sdcard/workshop-ui.xml"))
            break
        if attempt == 3:
            raise AssertionError(f"Accessibility dump failed after four attempts: {dump}")
        time.sleep(1)
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
    def matching(root):
        return [i for i in root.iter("node")
                if i.get("text", "").strip().casefold().startswith(prefix.strip().casefold())]

    root = tree()
    # Cold launches open the new category menu. Enter the marble category when needed.
    if prefix in ("24 Murmel-Aufgaben", "Freier Bauplatz", "Meine Erfindungen"):
        if any(i.get("text", "").startswith("1 · Kugelbahnen") for i in root.iter("node")):
            tap("1 · Kugelbahnen")
            root = tree()
    found = matching(root)
    # Toolbars can scroll on smaller screens as the construction kit grows.
    if not found:
        scrolls = [i for i in root.iter("node") if i.get("scrollable") == "true"]
        if scrolls:
            x1,y1,x2,y2=map(int,re.findall(r"\d+",scrolls[-1].get("bounds")))
            for _ in range(3):
                adb("shell","input","swipe",str((x1+x2)//2),str(y1+30),str((x1+x2)//2),str(y2-30),"250")
            root = tree()
    for _ in range(20):
        found = matching(root)
        if found:
            break
        scrolls = [i for i in root.iter("node") if i.get("scrollable") == "true"]
        if not scrolls:
            break
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", scrolls[-1].get("bounds")))
        # Overlapping slow steps keep list entries visible between snapshots.
        # A full-height fling can jump over task 10 in the 20-task campaign.
        height = y2-y1
        adb("shell", "input", "swipe", str((x1+x2)//2), str(y1+height*3//4),
            str((x1+x2)//2), str(y1+height//3), "650")
        root = tree()
    # Reuse the observed node instead of dumping the unchanged surface again.
    item = found[0] if found else node(prefix)
    assert item.get("enabled") == "true", f"Disabled: {prefix}"
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", item.get("bounds")))
    assert x2 > x1 and y2 > y1, f"Not visible: {prefix}"
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
    time.sleep(0.35)


def screenshot(name):
    with (OUTPUT / f"{name}.png").open("wb") as image:
        subprocess.run(["adb", "exec-out", "screencap", "-p"], stdout=image, check=True)


