---
title: Squery
description: Squery - simple SQL queries in Scala
pagination:
  enabled: false
---

# {{ page.title }}

Simple SQL queries in Scala 3.

No DSLs, no fuss, just plain SQL.

Scala Native is supported since 0.11.0 with a Scala Native-compatible JDBC driver.


Squery supports basic scala/java types out of the box:
- `String`, `Int`, `Double` etc
- `Option[T]` for nullable columns
- `Vector[T]` for SQL arrays, up to 3 dimensions
- semiauto derivation for singleton (java-esque) enums, just add `derives SqlRead, SqlWrite`
