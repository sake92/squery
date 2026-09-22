---
title: Squery
description: Plain SQL and type-safe JDBC access for Scala 3
pagination:
  enabled: false
---

# {{ page.title }}

Squery is a small Scala 3 library for type-safe JDBC access with plain SQL:
no query DSL, no generated runtime layer, and no hidden connection management.

```scala
val customerId = 42
val customers = ctx.run {
  sql"SELECT id, name FROM customers WHERE id = $customerId"
    .readRows[Customer]()
}
```

Interpolated values become prepared-statement parameters. Results are decoded into
Scala scalar values, case classes, or named tuples.

## Features

- Any JDBC driver, with database-specific codecs for PostgreSQL, MySQL, MariaDB,
  Oracle, H2, and SQLite
- JVM and Scala Native support (with a Scala Native-compatible JDBC driver)
- Case-class and named-tuple row decoding, including nested and optional joined rows
- Safe dynamic SQL with `Query.join`, `Query.when`, `Query.in`, and `Query.values`
- Transactions and configurable transaction isolation
- Generated keys, returning rows, JDBC batch updates, and generic statement execution
- Per-query fetch size, timeout, row limit, result-set, and generated-key options
- Database-to-Scala model and DAO generation

## Supported values

The core module reads and writes:

- `String`, `Boolean`, `Byte`, `Short`, `Int`, `Long`, and `Double`
- `Instant`, `OffsetDateTime`, `LocalDate`, and `LocalDateTime`
- `Option[T]` for nullable columns
- `Array[T]` and `Vector[T]` for SQL arrays, up to three dimensions
- `Array[Byte]` and `Vector[Byte]` for binary data
- singleton enums with `derives SqlRead, SqlWrite`
- case classes with `derives SqlReadRow` for multi-column rows
- named tuples for multi-column rows

Database-specific imports provide the appropriate `UUID` codec, while the optional
`squery-postgres-jawn` module provides PostgreSQL JSON/JSONB support using Jawn.

Start with [Setup](/tutorials/setup.html), then continue to the
[Quickstart](/tutorials/quickstart.html). The [How-Tos](/howtos/) cover queries,
updates, interpolation, transactions, and custom types.
