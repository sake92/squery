//> using scala 3.7.4
//> using platform native
//> using nativeVersion 0.5.12
//> using dep com.github.lolgab::scala-native-jdbc-sqlite::0.0.6
//> using dep ba.sake::squery::0.11.0

import ba.sake.squery.{*, given}
import org.sqlite.SQLiteDataSource

case class Customer(id: Int, name: String) derives SqlReadRow

val ds = SQLiteDataSource()
ds.setUrl("jdbc:sqlite:local_file.db")

val ctx = SqueryContext(ds)

ctx.run {
  sql"""
    CREATE TABLE IF NOT EXISTS customers(
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name VARCHAR
    )
  """.update()

  val c1 = Customer(1, "my_customer")
  val c2 = Customer(1, "other_customer")
  val insertedCustomersCount = sql"""
    INSERT INTO customers(name)
    VALUES (${c1.name}), (${c2.name})
  """.insert()
  println(s"Inserted # of customers: " + insertedCustomersCount)

  println(
    sql"SELECT id FROM customers".readValues[Int]()
  )
}
