#!/usr/bin/env bash

set -euo pipefail
"$(dirname "$0")/run-bounded-test.sh" existential-translation 3 exists.kt
