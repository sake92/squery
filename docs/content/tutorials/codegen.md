---
title: Generating Code
description: Squery Generating Code Tutorial
---

# {{ page.title }}



Squery has a code generator that can generate code for various databases:  
Postgres, MySQL, MariaDB, Oracle, H2, SQLite, etc.

It generates models for table rows and DAOs with various utility methods:
- countAll, countWhere
- findAll, findWhere, findWhereOpt, findAllWhere, findById, findByIdOpt, findByIds
- insert, updateById
- deleteWhere, deleteById, deleteIds

Squery codegen is a bit special since it is using [Regenesca library](https://github.com/sake92/regenesca).  
When you add a new column for example, it will **refactor** the `*Row` and `*Dao` code in place!  
This means you can add your own methods/vals to the *generated code*, without fear that the codegen will remove it.  
Of course, it is best to use `scalafmt` after codegen so that the diff is minimal.



## Standalone generator

You can use it with scala-cli to test the generator and write the generated source to a file:
```scala
//> using dep "ba.sake::squery-generator:{{site.data.project.artifact.version}}"
//> using dep "ba.sake::squery:{{site.data.project.artifact.version}}"
// If using Postgres JSONB, also add:
// //> using dep "ba.sake::squery-postgres-jawn:{{site.data.project.artifact.version}}"

import java.nio.file.{Files, Paths}
import scala.util.Using
import ba.sake.squery.generator.*

val dataSource = ...
Using.resource(dataSource.getConnection) { connection =>
  val generator = SqueryGenerator(connection)
  val generatedCode = generator.generateString(Seq("myschema"))
  Files.writeString(Paths.get("Generated.scala"), generatedCode)
  println("Generated source written to Generated.scala")
}
```

Run the script with `scala-cli run generate.scala`, then include `Generated.scala` in your project.

For SQLite, generate the `main` schema with `generateString(Seq("main"))`. The safe
storage mappings are `INTEGER` → `Long`, `REAL` → `Double`, `TEXT` → `String`, and
`BLOB` → `Array[Byte]`; ambiguous declarations such as `NUMERIC` remain unknown.
The default conventions map integer `is_*`/`has_*`/`can_*` columns to `Boolean`, text
`*_at` columns to `Instant`, and text `*_date` columns to `LocalDate`. Ordered
`TypeMappingRule`s take precedence over built-in mappings and match both column name and declared type.
Generated `RETURNING` SQL requires SQLite 3.35 or newer. If using `STRICT` tables, use
SQLite 3.37 or newer; the SQLite JDBC driver version alone does not set the engine version.

The CLI accepts ordered, repeatable type mapping rules for every supported database in the format
`column-name-regex|declared-type-regex|Scala-type`. The first matching rule overrides the
built-in mapping. Both regexes must match the complete column name and the JDBC driver's declared
type, so use `.*` when a partial match is intended. Fully qualify a type when it needs no
additional import:

```shell
--typeMappingRule '.*_id|UUID|java.util.UUID'
```

Use repeatable `--includeTables` and `--excludeTables` regexes to select generated tables.
They match fully qualified `schema.table` names; exclusions take precedence.

When launching the CLI with Coursier, add your JDBC driver as another dependency. For SQLite:

```shell
cs launch org.xerial:sqlite-jdbc:3.46.1.0 \
  ba.sake:squery-cli_2.13:0.8.1 \
  -M ba.sake.squery.cli.SqueryMain -- \
  --jdbcUrl jdbc:sqlite:database.db \
  --schemaMappings main:com.example
```


## Mill plugin

See how it works in the dedicated GitHub repo https://github.com/sake92/mill-squery
