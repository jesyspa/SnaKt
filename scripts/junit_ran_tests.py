import sys
import xml.etree.ElementTree as ET
from collections import defaultdict

pairs = set()
for path in sys.argv[1:]:
    try:
        root = ET.parse(path).getroot()
    except ET.ParseError:
        continue
    for testcase in root.findall("testcase"):
        classname = testcase.get("classname", root.get("name", "?"))
        name = testcase.get("name", "?")
        if name.endswith("()"):
            name = name[:-2]
        pairs.add((classname, name))

classes_by_name = defaultdict(set)
for classname, name in pairs:
    classes_by_name[name].add(classname)

labels = set()
for classname, name in pairs:
    if len(classes_by_name[name]) > 1:
        labels.add(f"{classname}.{name}")
    else:
        labels.add(name)

for label in sorted(labels):
    print(label)
