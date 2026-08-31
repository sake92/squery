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

SQLite support requires SQLite JDBC 3.46.1 or newer and Squery 0.9.0 or newer. Generated
`RETURNING` SQL requires SQLite 3.35 or newer; `STRICT` tables require SQLite 3.37 or newer.



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

`ctx.run*` functions provide an implicit JDBC connection under the cover,  
thanks to scala3's context functions! <3








## Examples

You can find examples:
- in the [examples]({{site.data.project.gh.sourcesUrl}}/examples) folder
- in the [sharaf-petclinic demo](https://github.com/sake92/sharaf-petclinic/tree/main/app/src/ba/sake/sharaf/petclinic/db/daos)
- in the [squery tests]({{site.data.project.gh.sourcesUrl}}/squery/test)
