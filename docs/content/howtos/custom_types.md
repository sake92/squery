---
title: How To Custom Types
description: Squery How To Custom Types
---

# {{ page.title }}

## How to Read Custom Type Column?

You can read any column that has an instance of `SqlRead`  typeclass.
To support your custom type, make a `given SqlRead` with proper implementation:

```scala
given SqlRead[MyType] with {
  def readByName(jRes: jsql.ResultSet, colName: String): Option[MyType] =
    Option(jRes.getString(colName)).map(parseMyCustomType(...))
  def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[MyType] =
    Option(jRes.getString(colIdx)).map(parseMyCustomType(...))
}
```

## How to Read Singleton enum?

Say you have a column with fixed set of values:
```
enum Color:
  case red, blue, green
```

Squery can automatically derive instances of `SqlRead` and`SqlWrite`:
```
enum Color derives SqlRead, SqlWrite
```

It uses stringified values of the enum cases: `red`, `green` and `blue`.


