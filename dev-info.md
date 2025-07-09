# Info for plugin developers

## Testing

We use the test framework built for kotlinc for our tests.
A test consists of two files:

* A `.kt` file annotated with expected diagnostics.
* A `.fir.diag.txt` file with the diagnostic text.
    * In particular, this will include (a filtered version of) the generated Viper code.

There is a generated Java file that is the actual test runner;
you can regenerate it using the `generateTests` gradle action.

## Silicon

Since Silicon and its dependencies are not centrally published, we provide them in
[our Maven Space Repository](https://jetbrains.team/p/kotlin-formver/packages/maven/maven).
To use the plugin, nothing is required except importing the library from there.

However, if you want to publish a new version of the Silicon library,
here is some useful information about that:

As prerequisites, you will need:

* The Java Development Kit
* [Maven](https://maven.apache.org/index.html)
* [SBT](https://www.scala-sbt.org/)

After that clone and build Silicon:

```bash
# The recursive cloning pulls `silver` as well. 
git clone --recursive https://github.com/viperproject/silicon.git
cd silicon
# Compile Scala code into JVM bytecode.
sbt compile
# This command build the fat-JAR file containing all the dependencies
# required by Silver (Silicon, Scala Library, ...)
sbt assembly
```

To publish the Silicon jar to our Space repo we first need to modify the `built.sbt` script of silicon.
For that refer to the [patch file](resources/patches/silicon-publish-maven.patch) that include all necessary changes.

In addition, you need to generate an access token for write access to the repository.
For that you need to create a credential file at `~/.sbt/space-maven.credentials`.
For detailed information of how to do so see the instructions on
the [repository site](https://jetbrains.team/p/kotlin-formver/packages/maven/maven)
under `Connect -> Publish` with tool `sbt` selected.
With the drop-down menu you can create a write-access token.

After completing these steps, you will be able to publish the Silicon artifact with

```bash
sbt publish
```

Additional Info:

* If you get a 401 response code while publishing, set the Space repository to private access.
  Due to a bug, it is currently not possible to publish to public repos.
* If you want to experiment locally, you can install Silicon into the local maven repo with `sbt publishM2`

