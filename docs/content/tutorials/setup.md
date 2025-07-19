---
title: Setup
description: Squery Setup Tutorial
---


# {{ page.title }}

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
//> using dep {{site.data.project.artifact.org}}::{{site.data.project.artifact.name}}:{{site.data.project.artifact.version}}
```

## Scastie

You can also use this [Scastie example](https://scastie.scala-lang.org/39YRVAiHToGTPNE6RcWQ9Q) to try Squery online.


