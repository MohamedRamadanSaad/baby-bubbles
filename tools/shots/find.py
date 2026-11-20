"""Prints the centre "x y" of the first UI node whose text equals argv[2]."""
import re
import sys
import xml.etree.ElementTree as ET

try:
    root = ET.parse(sys.argv[1]).getroot()
except Exception:
    sys.exit(0)
want = sys.argv[2]
for node in root.iter("node"):
    text = (node.get("text") or "") + "|" + (node.get("content-desc") or "")
    if want in text.split("|"):
        m = re.findall(r"\d+", node.get("bounds", ""))
        if len(m) == 4:
            x1, y1, x2, y2 = map(int, m)
            print((x1 + x2) // 2, (y1 + y2) // 2)
            break
