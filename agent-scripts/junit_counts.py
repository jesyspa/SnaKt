import sys
import xml.etree.ElementTree as ET

# Counted from the <testcase> elements rather than the <testsuite> attributes:
# one module can emit several suites, and a suite that died early still carries
# a count that its cases do not back.
#
# Golden-file mismatches are counted apart from the rest, because under
# --update-goldens they are the expected outcome and other failures are not.
# The types are the ones is_assertion_failure_type accepts.
ASSERTION_TYPES = ("org.opentest4j.AssertionFailedError",)
ASSERTION_SUFFIX = "ComparisonFailure"

total = assertion_failed = other_failed = skipped = unreadable = 0

for path in sys.argv[1:]:
    try:
        root = ET.parse(path).getroot()
    except ET.ParseError:
        unreadable += 1
        continue
    for testcase in root.iter("testcase"):
        total += 1
        node = testcase.find("failure")
        if node is None:
            node = testcase.find("error")
        if node is None:
            if testcase.find("skipped") is not None:
                skipped += 1
            continue
        failure_type = node.get("type", "")
        if failure_type in ASSERTION_TYPES or failure_type.endswith(ASSERTION_SUFFIX):
            assertion_failed += 1
        else:
            other_failed += 1

print(f"{total} {assertion_failed} {other_failed} {skipped} {unreadable}")
