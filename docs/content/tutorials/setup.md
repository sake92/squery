---
title: Setup
description: Squery Setup Tutorial
---


# {{ page.title }}

Squery is published for Scala 3. Add the core module and the JDBC driver for your
database; JDBC drivers are not bundled.

## Mill

```scala
def mvnDeps = super.mvnDeps() ++ Seq(
  mvn"{{site.data.project.artifact.org}}::{{site.data.project.artifact.name}}:{{site.data.project.artifact.version}}"
)
```


## sbt

```scala
libraryDependencies ++= Seq(
  "{{site.data.project.artifact.org}}" %% "{{site.data.project.artifact.name}}" % "{{site.data.project.artifact.version}}"
)
```

## Scala CLI

```scala
//> using dep "{{site.data.project.artifact.org}}::{{site.data.project.artifact.name}}:{{site.data.project.artifact.version}}"
```

## Optional modules

PostgreSQL applications using the Jawn AST can add JSON/JSONB codecs:

```scala
// Mill
mvn"{{site.data.project.artifact.org}}::squery-postgres-jawn:{{site.data.project.artifact.version}}"

// sbt
"{{site.data.project.artifact.org}}" %% "squery-postgres-jawn" % "{{site.data.project.artifact.version}}"

// Scala CLI
//> using dep "{{site.data.project.artifact.org}}::squery-postgres-jawn:{{site.data.project.artifact.version}}"
```

The code generator is a Scala 2.13 artifact because it is also used by the CLI. For a
standalone Scala CLI generator script:

```scala
//> using scala "2.13.15"
//> using dep "{{site.data.project.artifact.org}}::squery-generator:{{site.data.project.artifact.version}}"
```

Scala Native is supported with a Scala Native-compatible JDBC driver. See the
[`examples/cli/native.sc`]({{site.data.project.gh.sourcesUrl}}/examples/cli/native.sc)
example for a working SQLite setup.

## Scastie

You can also use this [Scastie example](https://scastie.scala-lang.org/39YRVAiHToGTPNE6RcWQ9Q) to try Squery online.
