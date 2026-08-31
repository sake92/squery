# squery

Simple SQL queries in Scala 3.

No DSLs, no fuss, just plain SQL.

Supports *any* JDBC driver.  
Additional support for Postgres, MySql, MariaDb, Oracle, H2, and SQLite.

Scastie example: https://scastie.scala-lang.org/JArud6GGSLOmYyxCNsNdNw


> See also https://github.com/sake92/mill-squery for generating boilerplate models and DAOs automatically from db.

## Hello world:
```scala

// table rows
case class Customer(id: Int, name: String) derives SqlReadRow
case class Phone(id: Int, number: String) derives SqlReadRow

// joined row
case class CustomerWithPhone(c: Customer, p: Phone) derives SqlReadRow

val ds = JdbcDataSource()
ds.setURL("jdbc:h2:mem:")

val ctx = SqueryContext(ds)

ctx.run {
  val res: Seq[CustomerWithPhone] = sql"""
    SELECT c.id, c.name,
           p.id, p.number
    FROM customers c
    JOIN phones p ON p.customer_id = c.id
  """.readRows[CustomerWithPhone]()
}
```

---

## Generator

You can generate boilerplate code for `Row`s and `DAO`s.

### Mill plugin

See https://github.com/sake92/mill-squery

### CLI

You can use `squery-cli` with Coursier launcher to generate your sources:

```shell
cs launch ba.sake::squery-cli:0.8.1 -M ba.sake.squery.cli.SqueryMain -- \
  --jdbcUrl jdbc:h2:... \
  --baseFolder src \
  --schemaMappings public:com.example.public \
  --schemaMappings myschema:com.example.myschema \ # this is a repeatable argument
  # these are optional:
  -- colNameIdentifierMapper camelcase \ # or noop
  -- typeNameMapper camelcase \ # or noop
  -- rowTypeSuffix Row \
  -- daoTypeSuffix Dao 
```


### Code

You can use `squery-generator` library to generate code directly.  
This is handy when using Ammonite to explore a database structure and its contents.  
It can also be used to generate source code manually in scala-cli or in your project (if you dont like CLI or mill plugin).

SQLite code generation reads only the `main` schema. It maps `INTEGER`, `REAL`, `TEXT`, and
`BLOB` to `Long`, `Double`, `String`, and `Array[Byte]`; ambiguous declarations such as
`NUMERIC` remain unknown. `is_*`, `has_*`, and `can_*` integer columns map to `Boolean`,
`*_at` text columns to `Instant`, and `*_date` text columns to `LocalDate`. Ordered
`SqliteTypeMappingRule`s override these conventions and require both column-name and
declared-type regex matches. Generated `RETURNING` SQL requires SQLite 3.35 or newer;
if using SQLite `STRICT` tables, use SQLite 3.37 or newer.
