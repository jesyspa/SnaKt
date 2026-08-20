# Air cloud agent setup

`cloud/startup.sh` prepares a SnaKt build environment on an Air agent box. Air
runs it on every agent launch and every resume, so it is idempotent: a box that
is already set up is left alone, and a repeat run takes well under a second.

After it runs, `./gradlew` and everything under `agent-scripts/` work with no
environment setup at all — no exports, no `source`, nothing to remember.

## What it does, and why each part is needed

**Installs Temurin JDK 21** into `~/.air/toolchain`. The only preinstalled JDK
is JBR 25, which Gradle 8.14.3 does not support: builds die with a message whose
entire text is the version number,

```
* What went wrong:
25.0.2
```

which is Gradle's own build-script compiler rejecting the version string, long
before any project code runs.

**Installs Z3 4.8.7** into `~/.air/toolchain`. SnaKt verifies nothing without
it, and it is not on the image. The download is unpacked with `python3 -m
zipfile` because there is no `unzip` here.

Both downloads are pinned by version and checked against a recorded SHA-256, so
a truncated or substituted archive stops the script instead of being installed.
They come from GitHub release assets because the proxy allows `github.com` but
blocks `api.adoptium.net`, which also rules out Gradle's foojay toolchain
resolver.

**Writes `~/.gradle/gradle.properties`** with `org.gradle.java.home` and
`org.gradle.java.installations.paths`, pointing Gradle and `jvmToolchain(21)` at
the installed JDK.

**Writes `~/.gradle/init.d/air-z3.gradle`**, which puts `Z3_EXE` into the forked
test JVMs where Silicon reads it.

Those last two exist because of the constraint that shapes this whole script:
**an agent's shells are neither interactive nor login shells**, so they read
neither `~/.bashrc` nor `~/.profile`, and `BASH_ENV` is unset. A setup script
cannot export a variable that any later command will see. Anything that has to
survive must live in a file that Gradle reads on its own. A `Z3_EXE` already
present in the environment still takes precedence, so this does not override a
deliberate choice.

**Writes `~/.air/env.sh`** and sources it from `~/.bashrc` and `~/.profile`, for
interactive shells and for the case below. Both edits are confined to a marked
block that is rewritten rather than appended, so repeated runs cannot pile up.

**Downloads the Gradle distribution**, so the first real build is not
mysteriously slow. Skip it with `AIR_SKIP_GRADLE_PREWARM=1`.

## Two things it does not solve

**A consumer project needs `Z3_EXE` in the environment.** Verification there
runs inside the Kotlin compile daemon rather than a Gradle `Test` task, and
nothing in Gradle's configuration files can set that process's environment. So
for something like
[snakt-usage-example](https://github.com/jesyspa/snakt-usage-example):

```sh
source ~/.air/env.sh
./gradlew build
```

Without it the build fails with `Cannot run prover at location 'z3': not a
file`. The JDK half needs nothing, since `org.gradle.java.home` is picked up
globally. Giving the compiler plugin an option for the solver path would remove
this wrinkle for every awkward environment, not just this one.

**`pre-commit` cannot be installed.** The proxy blocks
`files.pythonhosted.org`, so pip cannot fetch it or its dependencies.
`agent-scripts/check-all.sh` therefore reports it as skipped and exits 2, which
per `AGENTS.md` is not a pass. Its hooks can be run directly:

```sh
./agent-scripts/check-testdata.sh
./agent-scripts/tests/run.sh
```

The third hook, `end-of-file-fixer`, amounts to checking that changed files end
in exactly one newline.

## Overrides

| Variable                    | Effect                                          |
|:----------------------------|:------------------------------------------------|
| `AIR_TOOLCHAIN_ROOT`        | Where the JDK and Z3 go. Default `~/.air/toolchain`. |
| `AIR_SKIP_GRADLE_PREWARM=1` | Do not predownload the Gradle distribution.     |

## A note on caching

Everything lands under `$HOME`, which does not survive a fresh box: a previous
session's installs are gone even though notes about them persist. That is why
this script exists rather than a one-off setup. Re-running it on a box that
already has the toolchain is nearly free, and on a fresh one the proxy usually
has the archives cached.
