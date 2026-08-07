# Publishing Silicon

Use this to publish a new version of the Silicon library. Consuming it needs
nothing beyond the dependency: Silicon and its dependencies come from
[our Maven Space repository](https://jetbrains.team/p/kotlin-formver/packages/maven/maven).

Prerequisites: JDK 17, [Maven](https://maven.apache.org/index.html),
[SBT](https://www.scala-sbt.org/).

Clone and build Silicon:

```bash
# Recursive cloning pulls `silver` as well.
git clone --recursive https://github.com/viperproject/silicon.git
cd silicon
sbt compile
# Builds the fat JAR containing Silicon, the Scala library and the rest.
sbt assembly
```

Apply [the patch](resources/patches/silicon-publish-maven.patch) to Silicon's
`build.sbt`.

Create `~/.sbt/space-maven.credentials` with a write-access token. The
repository page has the instructions under `Connect -> Publish` with `sbt`
selected; the drop-down there creates the token.

```bash
sbt publish
```

A 401 while publishing means the Space repository is set to public access; set
it to private.

`sbt publishM2` installs Silicon into the local Maven repository for local
experiments.
