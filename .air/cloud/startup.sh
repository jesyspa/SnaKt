#!/usr/bin/env bash
#
# Prepares a SnaKt build environment on an Air cloud agent box.
#
# Air runs this on every agent launch and on every resume, so everything here
# is idempotent: work already done is detected and skipped, and re-running
# changes nothing. The expensive steps (two downloads) only happen on a box
# that does not have them yet.
#
# What it deals with, all of which otherwise breaks a build here:
#
#   - The only preinstalled JDK is JBR 25, which Gradle 8.14.3 does not
#     support. It fails with a message that is only the version number.
#   - Z3 is not installed, and SnaKt cannot verify anything without it.
#   - The agent's shells are neither interactive nor login shells, so they
#     read neither ~/.bashrc nor ~/.profile. Exporting variables cannot
#     reach a later command; configuration has to go somewhere Gradle reads
#     by itself. That is what the files under ~/.gradle are for.
#
# Override the install location with AIR_TOOLCHAIN_ROOT. Set
# AIR_SKIP_GRADLE_PREWARM=1 to leave the Gradle distribution undownloaded.

set -euo pipefail

readonly JDK_VERSION="21.0.12.1+1"
readonly JDK_ARCHIVE="OpenJDK21U-jdk_x64_linux_hotspot_21.0.12.1_1.tar.gz"
readonly JDK_URL="https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/${JDK_ARCHIVE}"
# Published by Adoptium alongside the release, as ${JDK_ARCHIVE}.sha256.txt.
readonly JDK_SHA256="ce79869e1307ed8ee1e2baa86a412b1eb5b75d10a01006d788a6f968bcfaee94"

# 4.8.7 is the version SnaKt is developed against; newer Z3 releases change
# results. The Ubuntu 16.04 build is the oldest-glibc one offered and runs here.
readonly Z3_VERSION="4.8.7"
readonly Z3_ARCHIVE="z3-4.8.7-x64-ubuntu-16.04.zip"
readonly Z3_URL="https://github.com/Z3Prover/z3/releases/download/z3-4.8.7/${Z3_ARCHIVE}"
# Z3 publishes no checksums, so this is pinned from the release artifact.
readonly Z3_SHA256="fcde3273ba88e291fe93db4b9d39957274700caeebba8aefbae28796da0dc0b7"

readonly TOOLCHAIN_ROOT="${AIR_TOOLCHAIN_ROOT:-$HOME/.air/toolchain}"
readonly JDK_HOME="$TOOLCHAIN_ROOT/jdk-$JDK_VERSION"
readonly Z3_HOME="$TOOLCHAIN_ROOT/z3-$Z3_VERSION"
readonly Z3_EXE="$Z3_HOME/bin/z3"
readonly ENV_FILE="$HOME/.air/env.sh"
readonly GRADLE_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
readonly REPO_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"

readonly MARKER_BEGIN="# >>> snakt air setup >>>"
readonly MARKER_END="# <<< snakt air setup <<<"

log() { printf '[air-setup] %s\n' "$*"; }
warn() { printf '[air-setup] warning: %s\n' "$*" >&2; }
die() { printf '[air-setup] error: %s\n' "$*" >&2; exit 1; }

# Two runs at once would fight over the same directories. Whoever gets here
# second waits and then finds the work already done.
acquire_lock() {
    command -v flock >/dev/null 2>&1 || return 0
    mkdir -p "$(dirname -- "$ENV_FILE")"
    exec 9>"$HOME/.air/.startup.lock"
    flock 9 || warn "could not take the setup lock; continuing anyway"
}

# Downloads to $2 and checks it against $3 before the caller unpacks it. A
# truncated or replaced archive stops the script rather than being installed.
fetch_verified() {
    local url="$1" dest="$2" want="$3" got
    curl --silent --show-error --location --fail \
        --retry 3 --retry-delay 2 --retry-all-errors \
        --max-time 600 --output "$dest" "$url"
    got="$(sha256sum -- "$dest" | cut -d' ' -f1)"
    [ "$got" = "$want" ] || die "checksum mismatch for $url: expected $want, got $got"
}

install_jdk() {
    if [ -x "$JDK_HOME/bin/javac" ]; then
        log "JDK $JDK_VERSION already installed"
        return
    fi
    log "installing JDK $JDK_VERSION (Gradle 8.14.3 cannot use the preinstalled JBR 25)"
    local tmp
    tmp="$(mktemp -d)"
    # shellcheck disable=SC2064  # $tmp is fixed now, which is what we want.
    trap "rm -rf -- '$tmp'" RETURN
    fetch_verified "$JDK_URL" "$tmp/jdk.tar.gz" "$JDK_SHA256"
    mkdir -p "$tmp/unpacked"
    tar -xzf "$tmp/jdk.tar.gz" -C "$tmp/unpacked"
    local unpacked
    unpacked="$(find "$tmp/unpacked" -mindepth 1 -maxdepth 1 -type d | head -1)"
    [ -x "$unpacked/bin/javac" ] || die "unpacked JDK has no bin/javac"
    mkdir -p "$TOOLCHAIN_ROOT"
    rm -rf -- "$JDK_HOME.partial"
    mv -- "$unpacked" "$JDK_HOME.partial"
    mv -- "$JDK_HOME.partial" "$JDK_HOME"
    log "JDK installed at $JDK_HOME"
}

install_z3() {
    if [ -x "$Z3_EXE" ] && "$Z3_EXE" --version 2>/dev/null | grep -q "$Z3_VERSION"; then
        log "Z3 $Z3_VERSION already installed"
        return
    fi
    log "installing Z3 $Z3_VERSION"
    local tmp
    tmp="$(mktemp -d)"
    # shellcheck disable=SC2064
    trap "rm -rf -- '$tmp'" RETURN
    fetch_verified "$Z3_URL" "$tmp/z3.zip" "$Z3_SHA256"
    # No unzip on this image, but python3 is present.
    python3 -c 'import sys, zipfile; zipfile.ZipFile(sys.argv[1]).extractall(sys.argv[2])' \
        "$tmp/z3.zip" "$tmp/unpacked"
    local unpacked
    unpacked="$(find "$tmp/unpacked" -mindepth 1 -maxdepth 1 -type d | head -1)"
    [ -f "$unpacked/bin/z3" ] || die "unpacked Z3 has no bin/z3"
    chmod +x "$unpacked/bin/z3"
    mkdir -p "$TOOLCHAIN_ROOT"
    rm -rf -- "$Z3_HOME.partial"
    mv -- "$unpacked" "$Z3_HOME.partial"
    mv -- "$Z3_HOME.partial" "$Z3_HOME"
    "$Z3_EXE" --version | grep -q "$Z3_VERSION" || die "installed Z3 is not $Z3_VERSION"
    log "Z3 installed at $Z3_EXE"
}

# Replaces the block between our markers, leaving anything else in the file
# alone. Creating the file if absent, so this works on a bare box too.
write_managed_block() {
    local file="$1" body="$2"
    mkdir -p "$(dirname -- "$file")"
    [ -e "$file" ] || : >"$file"
    AIR_BEGIN="$MARKER_BEGIN" AIR_END="$MARKER_END" AIR_BODY="$body" \
        python3 - "$file" <<'PY'
import os, sys
path, begin, end = sys.argv[1], os.environ["AIR_BEGIN"], os.environ["AIR_END"]
block = begin + "\n" + os.environ["AIR_BODY"].rstrip("\n") + "\n" + end + "\n"
lines = open(path).read().splitlines(True)
out, skipping, replaced = [], False, False
for line in lines:
    if line.strip() == begin:
        skipping, replaced = True, True
        out.append(block)
        continue
    if skipping:
        if line.strip() == end:
            skipping = False
        continue
    out.append(line)
if not replaced:
    if out and not out[-1].endswith("\n"):
        out.append("\n")
    out.append(block)
open(path, "w").writelines(out)
PY
}

# Gradle reads these on its own, which is the only thing that works here:
# the agent's shells inherit no exports from this script.
configure_gradle() {
    # org.gradle.java.home points the daemon itself at the JDK. The
    # installations path lets `jvmToolchain(21)` resolve locally, which
    # matters because the proxy blocks the foojay resolver's api.adoptium.net.
    write_managed_block "$GRADLE_HOME/gradle.properties" "$(cat <<EOF
# Managed by .air/cloud/startup.sh. Edits inside this block are overwritten.
org.gradle.java.home=$JDK_HOME
org.gradle.java.installations.paths=$JDK_HOME
EOF
)"
    log "wrote $GRADLE_HOME/gradle.properties"

    # Z3 is read from the environment by Silicon, inside the forked test JVMs.
    # An init script is how we get it there without any shell involvement.
    mkdir -p "$GRADLE_HOME/init.d"
    cat >"$GRADLE_HOME/init.d/air-z3.gradle" <<EOF
// Managed by .air/cloud/startup.sh. Regenerated on every agent launch.
//
// SnaKt's tests reach Z3 through the Z3_EXE environment variable. Agent
// shells here are non-interactive and non-login, so they pick up nothing
// from ~/.bashrc or ~/.profile and cannot pass it down themselves. Setting
// it on the forked test JVMs makes ./gradlew test and agent-scripts work
// with no environment setup at all. A Z3_EXE inherited from the environment
// still wins, so this does not override a deliberate choice.
gradle.allprojects { project ->
    project.tasks.withType(Test).configureEach { task ->
        if (!System.getenv().containsKey('Z3_EXE')) {
            task.environment('Z3_EXE', '$Z3_EXE')
        }
    }
}
EOF
    log "wrote $GRADLE_HOME/init.d/air-z3.gradle"
}

# For humans and for anything that does read a shell profile. The agent's own
# shells do not, which is why this is a convenience rather than the mechanism.
write_env_file() {
    mkdir -p "$(dirname -- "$ENV_FILE")"
    cat >"$ENV_FILE" <<EOF
# Generated by .air/cloud/startup.sh. Source this in a shell that needs the
# toolchain on its PATH, e.g. to run kotlinc or z3 by hand:
#
#     source ${ENV_FILE/#$HOME/\$HOME}
#
export JAVA_HOME="$JDK_HOME"
export Z3_EXE="$Z3_EXE"
case ":\$PATH:" in
    *":$JDK_HOME/bin:"*) ;;
    *) PATH="$JDK_HOME/bin:\$PATH" ;;
esac
case ":\$PATH:" in
    *":$Z3_HOME/bin:"*) ;;
    *) PATH="$Z3_HOME/bin:\$PATH" ;;
esac
export PATH
EOF
    log "wrote $ENV_FILE"

    local body="[ -f \"$ENV_FILE\" ] && . \"$ENV_FILE\""
    write_managed_block "$HOME/.bashrc" "$body"
    write_managed_block "$HOME/.profile" "$body"
}

# Downloading the ~130 MB Gradle distribution here rather than inside the
# first build, so that build is not mysteriously slow.
prewarm_gradle() {
    if [ "${AIR_SKIP_GRADLE_PREWARM:-0}" = "1" ]; then
        log "skipping Gradle prewarm (AIR_SKIP_GRADLE_PREWARM=1)"
        return
    fi
    [ -x "$REPO_ROOT/gradlew" ] || return 0
    if JAVA_HOME="$JDK_HOME" "$REPO_ROOT/gradlew" --version >/dev/null 2>&1; then
        log "Gradle distribution ready"
    else
        warn "could not prewarm the Gradle distribution; the first build will do it"
    fi
}

# check-all.sh runs pre-commit and reports exit 2 when it is missing. It
# cannot be installed here, so say why rather than leaving that unexplained.
report_pre_commit() {
    if command -v pre-commit >/dev/null 2>&1; then
        log "pre-commit available"
        return
    fi
    warn "pre-commit is not installed and cannot be installed on this box: the
          proxy blocks files.pythonhosted.org, so pip cannot fetch it.
          ./agent-scripts/check-all.sh will report it as skipped and exit 2.
          Its hooks can be run directly instead:
            ./agent-scripts/check-testdata.sh
            ./agent-scripts/tests/run.sh"
}

main() {
    case "$(uname -s)/$(uname -m)" in
        Linux/x86_64) ;;
        *)
            warn "unsupported platform $(uname -s)/$(uname -m); skipping setup"
            return 0
            ;;
    esac

    acquire_lock
    install_jdk
    install_z3
    configure_gradle
    write_env_file
    prewarm_gradle
    report_pre_commit

    log "ready: JDK $JDK_VERSION, Z3 $Z3_VERSION; ./gradlew needs no further setup"
}

main "$@"
