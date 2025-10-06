# SnaKt: Kotlin Formal Verification Plugin

SnaKt is a plugin for [`kotlinc`](https://github.com/JetBrains/kotlin)
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

### Setup

In your `settings.gradle.kts`, configure your Gradle plugin repositories to allow local plugins:

```kotlin
pluginManagement {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}
```

Then in `build.gradle.kts`, enable the plugin. Make sure that you also enable the Maven
local repository here: it's necessary to find the compiler plugin for the plugin.

```kotlin
plugins {
    kotlin("jvm") version "2.2.0"
    id("org.jetbrains.kotlin.formver") version "0.1.0-SNAPSHOT"
}

repositories {
    mavenCentral()
    mavenLocal()
}
```

Additionally, make sure you set Kotlin to use K2 and increase the stack size of the Kotlin Daemon:

```kotlin
kotlin {
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
    }
    // Set stack size to 30mb
    kotlinDaemonJvmArgs = listOf("-Xss30m")
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
To access these, add a dependency to `formver.annotations`:

```kotlin
dependencies {
    implementation("org.jetbrains.kotlin.formver:formver.annotations:0.1.0-SNAPSHOT")
}
```

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

One way to do this is as follows:

```bash
export Z3_EXE=/usr/bin/z3 # or a different directory in $PATH
sudo cp z3-4.8.7-*/bin/z3 $Z3_EXE
echo "export Z3_EXE=$Z3_EXE" >> ~/.profile
```

Make sure that running `$Z3_EXE --version` gives `Z3 version 4.8.7`.
Check that this is the case when you open a new shell, too!
You need to (additionally) set `Z3_EXE` in `~/.xprofile` and/or
`~/.bash_profile` depending on your shell, window manager, display
manager, operating system, etc.

## Contact

Reach out to komi.golov@jetbrains.com if you'd like to use or contribute to the plugin!
We are open to supervising bachelor and master theses about this work.