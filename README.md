# squery

Simple SQL queries in Scala 3.

No DSLs, no fuss, just plain SQL.

Supports *any* JDBC driver.  
Additional support for Postgres, MySql, MariaDb, Oracle, H2.

Generate models and DAOs automatically from db.

---
Scastie example: https://scastie.scala-lang.org/JArud6GGSLOmYyxCNsNdNw


---
Hello world:
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