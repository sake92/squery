---
title: How To Custom Types
description: Squery How To Custom Types
---

# {{ page.title }}

The core module supports strings, booleans, numeric primitives, Java time values,
nullable values, binary data, and SQL arrays. Import the package for your database to
use its `UUID` codec and any storage-specific overrides:

```scala
import ba.sake.squery.{*, given}
import ba.sake.squery.postgres.{*, given} // or sqlite, mysql, mariadb, oracle, h2
```

For PostgreSQL JSON/JSONB with Jawn, add the `squery-postgres-jawn` module and import:

```scala
import ba.sake.squery.postgres.jawn.{*, given}
```

## How to Read Custom Type Column?

You can read any column that has an instance of the `SqlRead` type class.
To support your custom type, make a `given SqlRead` with proper implementation:

```scala
given SqlRead[MyType] with {
  def readByName(jRes: jsql.ResultSet, colName: String): Option[MyType] =
    Option(jRes.getString(colName)).map(parseMyCustomType(...))
  def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[MyType] =
    Option(jRes.getString(colIdx)).map(parseMyCustomType(...))
}
```

Writing uses the matching `SqlWrite[T]` type class:

```scala
given SqlWrite[MyType] with {
  def write(ps: jsql.PreparedStatement, idx: Int, value: Option[MyType]): Unit =
    ps.setString(idx, value.map(renderMyCustomType).orNull)
}
```

## How to Read and Write a Singleton Enum?

Say you have a column with fixed set of values:
```
enum Color:
  case red, blue, green
```

Squery can automatically derive instances of `SqlRead` and `SqlWrite`:

```scala
enum Color derives SqlRead, SqlWrite
```

It uses stringified values of the enum cases: `red`, `green` and `blue`.

