---
title: Generating Code
description: Squery Generating Code Tutorial
---

# {{ page.title }}



Squery has a code generator that can generate code for various databases:  
Postgres, MySQL, MariaDB, Oracle, H2, SQLite, etc.

It generates models for table rows and DAOs with various utility methods:

- `countAll`, `countWhere`
- `findAll`, `findWhere`, `findWhereOpt`, `findAllWhere`, `findById`, `findByIdOpt`, `findByIds`
- `insert`, `updateById`
- `deleteWhere`, `deleteById`, `deleteIds`

Squery codegen is a bit special since it is using [Regenesca library](https://github.com/sake92/regenesca).  
When you add a new column for example, it will **refactor** the `*Row` and `*Dao` code in place!  
This means **you can add your own methods/vals to the generated code**, without fear that the codegen will remove it.



## CLI

The simplest way to generate sources is to launch `squery-cli` with Coursier. Add your
JDBC driver as another dependency because the CLI does not bundle database drivers.
For SQLite, create a database with the SQLite CLI:

```shell
sqlite3 database.db <<'SQL'
CREATE TABLE users (
  id INTEGER NOT NULL PRIMARY KEY,
  enabled BOOLEAN NOT NULL DEFAULT 1,
  birth_date TEXT,
  created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);
CREATE TABLE orders (
  id INTEGER NOT NULL PRIMARY KEY,
  user_id INTEGER NOT NULL REFERENCES users(id),
  placed_at TEXT
);
SQL
```

Add the application dependencies (Scala CLI syntax shown):

```scala
//> using scala "3.9.0"
//> using dep "ba.sake::squery:{{site.data.project.artifact.version}}"
//> using dep "ba.sake::squery-iron:{{site.data.project.artifact.version}}"
//> using dep "org.xerial:sqlite-jdbc:3.46.1.0"
```

Generate models and DAOs, including Iron key newtypes:

```shell
cs launch org.xerial:sqlite-jdbc:3.46.1.0 \
  ba.sake:squery-cli_2.13:{{site.data.project.artifact.version}} \
  -M ba.sake.squery.cli.SqueryMain -- \
  --jdbc-url jdbc:sqlite:database.db \
  --base-folder src/main/scala \
  --schema-mappings main:com.example.db \
  --newtypes iron \
  --excludeTables 'main\.flyway_schema_history' \
  --excludeTables 'main\.sqlite_.*'
```

Run this from the directory containing `database.db`; a relative SQLite JDBC URL
can create an empty database if the path is wrong. The exclusions keep Flyway's
history and any SQLite internal tables out of generated sources. Add exclusions
for other migration or application-internal tables as needed.

`--newtypes iron` generates per-row `PK` types and reuses the referenced PK type
for foreign keys. Keep `squery-iron` at the same version as `squery`; the current
Iron dependency requires Scala 3.9.0 or newer. To generate primitive keys, omit
`--newtypes iron` (or use `--newtypes none`) and omit `squery-iron`.

Import `ba.sake.squery.sqlite.{*, given}` wherever application queries bind or
read SQLite values; generated DAOs already include this import. Use `Boolean`
values for `enabled`, `java.time.LocalDate` for `birth_date`, and
`java.time.Instant` for `created_at` and `placed_at`.

For a `STRICT` table, replace `enabled BOOLEAN` with
`is_enabled INTEGER`: the generator recognizes the integer name convention and
SQLite stores Boolean values as `0`/`1`. For an integer Boolean without that
convention, pass an explicit mapping:

```shell
--typeMappingRule 'enabled|(?i:INTEGER)|Boolean'
```

For older generators that leave declared `BOOLEAN` unknown, the equivalent
explicit rule is `--typeMappingRule '.*|(?i:BOOLEAN|BOOL)|Boolean'`.

Store `birth_date` as ISO date text (`2024-02-29`) and timestamps as ISO instants
(`2024-02-29T12:34:56.123Z`). SQLite's `CURRENT_TIMESTAMP` produces
`YYYY-MM-DD HH:MM:SS`, which the `Instant` codec cannot parse. Use the `strftime`
default above or bind an `Instant` from Scala. See the
[SQLite mapping reference](../reference/sqlite.html) for other storage choices
and the [CLI reference](../reference/codegen-cli.html) for all options.

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


## Build plugins

Build integration is available through
[mill-squery](https://github.com/sake92/mill-squery),
[sbt-squery](https://github.com/sake92/sbt-squery), and
[deder-squery](https://github.com/sake92/deder-plugins/tree/main/deder-squery).
See the [plugin configuration reference](../reference/codegen-plugins.html) for
setting names, generation commands, and output locations.


## Generator library

For full programmatic control, use `squery-generator` directly. The following Scala CLI
script connects to a data source and writes generated source to a file:

```scala
//> using scala "2.13.15"
//> using dep "ba.sake::squery-generator:{{site.data.project.artifact.version}}"
//> using dep "your.jdbc:driver:version"

import java.nio.file.{Files, Paths}
import scala.util.Using
import ba.sake.squery.generator._

val dataSource = ...
Using.resource(dataSource.getConnection) { connection =>
  val generator = SqueryGenerator(connection)
  val generatedCode = generator.generateString(Seq("myschema"))
  Files.writeString(Paths.get("Generated.scala"), generatedCode)
  println("Generated source written to Generated.scala")
}
```

Run it with `scala-cli run generate.scala`, then include `Generated.scala` in your project.
The generator is published for Scala 2.13; its generated source targets Scala 3 and
uses the `squery` dependency from your application.

For SQLite, generate the `main` schema with `generateString(Seq("main"))`.
To enable typed keys programmatically, pass
`SqueryGeneratorConfig.Default.copy(newtypes = Newtypes.Iron)` to
`new SqueryGenerator(connection, config)` and add `squery-iron` to the consuming
application. Ordered `TypeMappingRule`s override the built-in mappings.
