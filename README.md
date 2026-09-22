# Squery

Squery is a small Scala 3 library for type-safe JDBC access with plain SQL:
no query DSL, no generated runtime layer, and no hidden connection management.

```scala
import ba.sake.squery.{*, given}

case class Customer(id: Int, name: String) derives SqlReadRow

val customers = ctx.run {
  val customerId = 42
  sql"SELECT id, name FROM customers WHERE id = $customerId"
    .readRows[Customer]()
}
```

Interpolated values become prepared-statement parameters. Literal SQL remains readable,
and result rows are decoded into Scala values, case classes, or named tuples.

## Features

- Works with any JDBC driver; includes database-specific codecs for PostgreSQL, MySQL,
  MariaDB, Oracle, H2, and SQLite.
- Runs on the JVM and on Scala Native with a compatible JDBC driver.
- Reads scalar values, case classes, nested rows for joins, optional rows for outer joins,
  and named tuples.
- Supports nullable values, Java time types, UUIDs, SQL arrays up to three dimensions,
  byte arrays, and derivation for singleton enums.
- Composes dynamic SQL with `Query.join`, `Query.when`, `Query.in`, and `Query.values`
  while preserving prepared-statement arguments.
- Provides transactions with configurable isolation, generated keys, returning rows,
  JDBC batch updates, and generic statement execution.
- Configures fetch size, timeout, maximum rows, result-set behavior, and generated-key
  selection per query.
- Generates row models and DAOs from an existing database, either from the CLI or as a library.

## Installation

Add the core module and your JDBC driver. For Scala CLI:

```scala
//> using dep "ba.sake::squery:0.13.0"
//> using dep "com.h2database:h2:2.3.232"
```

For Mill:

```scala
def mvnDeps = super.mvnDeps() ++ Seq(
  mvn"ba.sake::squery:0.13.0"
)
```

For sbt:

```scala
libraryDependencies += "ba.sake" %% "squery" % "0.13.0"
```

See the [setup guide](https://sake92.github.io/squery/tutorials/setup.html) and
[quickstart](https://sake92.github.io/squery/tutorials/quickstart.html) for a complete example.

## Querying

Create a `SqueryContext` from any standard JDBC `DataSource` and run queries inside
`run` or `runTransaction`:

```scala
import org.h2.jdbcx.JdbcDataSource
import ba.sake.squery.{*, given}

case class Customer(id: Int, name: String) derives SqlReadRow
case class Phone(id: Int, number: String) derives SqlReadRow
case class CustomerWithPhone(c: Customer, p: Option[Phone]) derives SqlReadRow

val ds = JdbcDataSource()
ds.setURL("jdbc:h2:mem:example;DB_CLOSE_DELAY=-1")

val ctx = SqueryContext(ds)

val rows: Seq[CustomerWithPhone] = ctx.run {
  sql"""
    SELECT c.id, c.name,
           p.id, p.number
    FROM customers c
    LEFT JOIN phones p ON p.customer_id = c.id
  """.readRows[CustomerWithPhone]()
}
```

The nested field names (`c` and `p`) match the selected column prefixes. An optional
nested row becomes `None` when all of its columns are `NULL`.

More examples are available in the [query guide](https://sake92.github.io/squery/howtos/queries.html),
[update guide](https://sake92.github.io/squery/howtos/updates.html), and
[`examples`](examples) directory.

## Code generation

The generator creates row models and DAOs, then uses
[Regenesca](https://github.com/sake92/regenesca) to update generated definitions in place.
Custom members added to generated files are preserved.

Launch the CLI with Coursier and include the JDBC driver as another dependency:

```shell
cs launch com.h2database:h2:2.3.232 \
  ba.sake:squery-cli_2.13:0.13.0 \
  -M ba.sake.squery.cli.SqueryMain -- \
  --jdbcUrl jdbc:h2:./database \
  --baseFolder src/main/scala \
  --schemaMappings public:com.example.db \
  --includeTables 'public\.(users|orders)' \
  --excludeTables 'public\.audit_.*'
```

`--schemaMappings`, `--typeMappingRule`, `--includeTables`, and `--excludeTables` are
repeatable. Table patterns match `schema.table`, and exclusions take precedence.
Type mapping rules use
`column-name-regex|declared-type-regex|Scala-type`; the first complete regex match wins:

```shell
--typeMappingRule '.*_id|UUID|java.util.UUID'
```

The CLI does not bundle JDBC drivers. SQLite code generation uses the `main` schema;
generated `RETURNING` SQL needs SQLite 3.35+, and `STRICT` tables need SQLite 3.37+.

See the [code-generation guide](https://sake92.github.io/squery/tutorials/codegen.html)
for the library API, SQLite mappings, and all generator options. A Mill plugin is
available at [sake92/mill-squery](https://github.com/sake92/mill-squery).
