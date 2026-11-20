---
title: Setup
description: Squery Setup Tutorial
---


# Setup

Squery is published for Scala 3. Add the core module and the JDBC driver for your
database; JDBC drivers are not bundled.

## Mill

```scala
def mvnDeps = super.mvnDeps() ++ Seq(
  mvn"ba.sake::squery:0.13.0"
)
```


## sbt

```scala
libraryDependencies ++= Seq(
  "ba.sake" %% "squery" % "0.13.0"
)
```

## Deder

```pkl
modules {
  new ScalaModule {
    deps {
      "ba.sake::squery:0.13.0"
    }
  }
}
```

## Scala CLI

```scala
//> using dep "ba.sake::squery:0.13.0"
```

## Optional modules

PostgreSQL applications using the Jawn AST can add JSON/JSONB codecs:

```scala
// Mill
mvn"ba.sake::squery-postgres-jawn:0.13.0"

// sbt
"ba.sake" %% "squery-postgres-jawn" % "0.13.0"

// Deder
"ba.sake::squery-postgres-jawn:0.13.0"

// Scala CLI
//> using dep "ba.sake::squery-postgres-jawn:0.13.0"
```

The code generator is a Scala 2.13 artifact because it is also used by the CLI. For a
standalone Scala CLI generator script:

```scala
//> using scala "2.13.15"
//> using dep "ba.sake::squery-generator:0.13.0"
```

Scala Native is supported with a Scala Native-compatible JDBC driver. See the
[`examples/cli/native.sc`](https://github.com/sake92/squery/tree/main/examples/cli/native.sc)
example for a working SQLite setup.

## Scastie

You can also use this [Scastie example](https://scastie.scala-lang.org/39YRVAiHToGTPNE6RcWQ9Q) to try Squery online.
