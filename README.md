# SnaKt: Kotlin Formal Verification Plugin

[SnaKt](https://github.com/jesyspa/SnaKt) is a plugin for `kotlinc`
that performs formal verification of Kotlin code by translating it to
[Viper](https://www.pm.inf.ethz.ch/research/viper.html).

The plugin is still in early development and large parts of Kotlin
syntax are not supported.

## Structure

This repository consists of three published parts:

- `formver.compiler-plugin`: a K2 compiler plugin that performs formal verification.
- `formver.gradle-plugin`: a Gradle plugin that loads the compiler plugin.
- `formver.annotations`: definitions that are used for adding specifications
  to your code.

Additionally, `formver.common` contains some code shared between these parts.

At present, we do not distribute any part of the plugin through a central repository.
If you would like to use the plugin, clone it and use the `publishToMavenLocal`
task to put it in your local repository.

## Running the plugin

Once you've published to your local Maven repository, you can use the Gradle
plugin to enable verification of your project.
You can see an example setup at [jesyspa/snakt-usage-example](https://github.com/jesyspa/snakt-usage-example).

### JDK

Build and run with a JDK that your Gradle version supports; JDK 21 is a safe
choice. The Gradle version this repository builds with (8.14.3) does not support
JDK 25, and the failure is easy to misread: the build aborts with a message that
is only the version number,

```
* What went wrong:
25.0.2
```

which comes from Gradle's own build-script compiler rejecting the version string
(`IllegalArgumentException` in `JavaVersion.parse`, visible under
`--stacktrace`). It happens before any project code runs, so neither this
repository nor your own build can catch it and report something friendlier. If
you see it, point `JAVA_HOME` at an older JDK and stop the daemons
(`./gradlew --stop`).

### Setup

In your `settings.gradle.kts`, configure your Gradle plugin repositories to allow local plugins:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
    }
}
```

Keep `gradlePluginPortal()` in the list: it is where the Kotlin Gradle plugin
itself comes from, and replacing it outright leaves `kotlin("jvm")` unresolvable.

Then in `build.gradle.kts`, enable the plugin. Make sure that you also enable the Maven
local repository here: it's necessary to find the compiler plugin for the plugin.

```kotlin
plugins {
    kotlin("jvm") version "2.3.0"
    id("org.jetbrains.kotlin.formver") version "0.1.0-SNAPSHOT"
}

repositories {
    mavenCentral()
    mavenLocal()
}
```

Additionally, increase the stack size and metaspace of the Kotlin Daemon: the shaded plugin
jar bundles Silicon and the Scala runtime (~100 MB), and without a larger metaspace the daemon
dies with `OutOfMemoryError: Metaspace` after roughly a dozen compiles.

```kotlin
kotlin {
    // Set stack size to 30mb and raise the metaspace limit
    kotlinDaemonJvmArgs = listOf("-Xss30m", "-XX:MaxMetaspaceSize=1g")
}
```

### Plugin configuration

Plugin options can be enabled using the `formver` configuration block:

```kotlin
formver {
    logLevel("full_viper_dump")
}
```

However, keep in mind that the Viper is dump is provided as an info message: this message will not be shown
unless you run `gradle` with the `--info` flag.

### Annotations

The plugin provides a number of annotations to add specifications to your code.
Applying the Gradle plugin automatically adds a dependency on `formver.annotations`.

### Running from the command line

To execute the plugin directly, build the plugin and then
specify the plugin `.jar` with `-Xplugin=`:

```sh
kotlinc -language-version 2.0 -Xplugin=path-to-plugin.jar myfile.kt
```

The plugin accepts a number of command line options which can be passed via
`-P plugin:org.jetbrains.kotlin.formver:OPTION=SETTING`:

- Option `log_level`: permitted values `only_warnings`, `short_viper_dump`, `full_viper_dump` (default:
  `only_warnings`).
- Option `error_style`: permitted values `user_friendly`, `original_viper` and `both` (default: `user_friendly`).
- Options `conversion_targets_selection` and `verification_targets_selection`: permitted values `no_targets`,
  `targets_with_contract`, `all_targets` (default: `targets_with_contract`).
- Option `unsupported_feature_behaviour`: permitted values `throw_exception`, `assume_unreachable` (default:
  `throw_exception`).

### Z3

The plugin relies on the SMT solver Z3 which needs to be installed manually.
To do so, download v4.8.7 from the [Releases page](https://github.com/Z3Prover/z3/releases/tag/z3-4.8.7).

Viper gives two ways of interfacing with Z3: text-based (using the `z3` binary)
or via the API (using a `.jar`).
At the moment we use the text-based interface, meaning you need to:

- Install the `z3` binary in your path
- Set the `Z3_EXE` environment variable correctly.

`Z3_EXE` has to be the path to the binary itself, not the directory holding it.
Any location works as long as it is on your `PATH`; if you can write to a system
directory, one way to do this is as follows:

```bash
export Z3_EXE=/usr/local/bin/z3
sudo cp z3-4.8.7-*/bin/z3 $Z3_EXE
echo "export Z3_EXE=$Z3_EXE" >> ~/.profile
```

Without root, install it under your home directory instead:

```bash
mkdir -p ~/.local/bin
cp z3-4.8.7-*/bin/z3 ~/.local/bin/z3
chmod +x ~/.local/bin/z3
export Z3_EXE=$HOME/.local/bin/z3
echo "export PATH=\$HOME/.local/bin:\$PATH" >> ~/.profile
echo "export Z3_EXE=\$HOME/.local/bin/z3" >> ~/.profile
```

Make sure that running `$Z3_EXE --version` gives `Z3 version 4.8.7`.
Check that this is the case when you open a new shell, too!
You need to (additionally) set `Z3_EXE` in `~/.xprofile` and/or
`~/.bash_profile` depending on your shell, window manager, display
manager, operating system, etc.

The Gradle and Kotlin daemons capture `Z3_EXE` at startup, so changing it
afterwards has no effect until both are stopped (`./gradlew --stop`, plus
killing the Kotlin daemon).

If `Z3_EXE` is unset or wrong, compilation fails with an internal error whose
details read `Cannot run prover at location 'z3': not a file`. That one is a
configuration problem rather than a bug: check `Z3_EXE` before reporting it.

## Contributing

See docs/developing.md for testing and the checks CI runs.

## Contact

Reach out to kameliya.golova@jetbrains.com if you'd like to use or contribute to the plugin!
We are open to supervising bachelor and master theses about this work.
