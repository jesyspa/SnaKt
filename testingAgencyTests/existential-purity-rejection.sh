#!/usr/bin/env bash

set -euo pipefail
"$(dirname "$0")/run-bounded-test.sh" existential-purity-rejection 3 exists_list_get_crash.kt
