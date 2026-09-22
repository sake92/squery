---
title: Quickstart
description: Squery Quickstart Tutorial
---

# {{ page.title }}

First, we need to initialize a `SqueryContext` with a standard JDBC `DataSource`.  
You will probably want to use a connection pool for performance (like HikariCP).

```scala
import ba.sake.squery.{*, given}
// import one of these if needed:
// import ba.sake.squery.sqlite.{*, given}
// import ba.sake.squery.postgres.{*, given}
// import ba.sake.squery.mysql.{*, given}
// import ba.sake.squery.mariadb.{*, given}
// import ba.sake.squery.oracle.{*, given}
// import ba.sake.squery.h2.{*, given}

val ds = com.zaxxer.hikari.HikariDataSource()
ds.setJdbcUrl(..)
ds.setUsername(..)
ds.setPassword(..)

val ctx = SqueryContext(ds)
```

Use the database-specific import when you need its codecs. For example, SQLite stores
`Boolean` as an integer and `UUID` and `Instant` as text, while PostgreSQL uses its
native UUID representation.

Generated SQLite `RETURNING` SQL requires SQLite 3.35 or newer; `STRICT` tables require
SQLite 3.37 or newer.



Now we can run queries inside the context:
```scala
ctx.run {
// queries go here!
}
```


or if you want to run them transactionally:
```scala
ctx.runTransaction {
// queries go here!
}
```

The `ctx.run*` methods provide a scoped `SqueryConnection` using Scala 3 context
functions. Connections are acquired from the data source and closed after the block.
Transactions commit on success and roll back when the block throws.

## Logging and update linting

On the JVM, Squery logs through SLF4J. Executed SQL is logged at `DEBUG`; bound values
are not included. JDBC warnings and SQL parsing failures are logged at `WARN`.

You can also warn when an `UPDATE` or `DELETE` has no `WHERE` clause:

```scala
val ctx = SqueryContext(ds, lintUpdates = true)
```

The linter is a safety warning, not a SQL validator: the statement still executes.
Keep database permissions, transactions, and application-level safeguards in place.








## Examples

You can find examples:
- in the [examples]({{site.data.project.gh.sourcesUrl}}/examples) folder
- in the [sharaf-petclinic demo](https://github.com/sake92/sharaf-petclinic/tree/main/app/src/ba/sake/sharaf/petclinic/db/daos)
- in the [squery tests]({{site.data.project.gh.sourcesUrl}}/squery/test)
