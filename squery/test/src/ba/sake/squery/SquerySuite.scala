package ba.sake.squery

import javax.sql.DataSource

import ba.sake.squery.read.*

class SquerySuite extends munit.FunSuite {

  def initDb(): SqueryContext = {
    val ds = com.zaxxer.hikari.HikariDataSource()
    ds.setJdbcUrl("jdbc:sqlite::memory:")
    val ctx = new SqueryContext(ds)

    ctx.run {
      update(sql"PRAGMA foreign_keys = ON;")

      // customers
      update(sql"""
        CREATE TABLE customers(
          id INTEGER PRIMARY KEY,
          name VARCHAR
        )
      """)
      update(sql"""
        INSERT INTO customers(id, name) VALUES(123, 'a_customer')
      """)

      // phones
      update(sql"""
        CREATE TABLE phones(
          id INTEGER PRIMARY KEY,
          customer_id INTEGER REFERENCES customers(id),
          number VARCHAR
        )
      """)
      update(sql"""
        INSERT INTO phones(id, customer_id, number) VALUES(1, 123, '061 123 456')
      """)
      update(sql"""
        INSERT INTO phones(id, customer_id, number) VALUES(2, 123, '062 225 883')
      """)
    }

    ctx
  }

  test("SELECT plain values") {
    val ctx = initDb()

    ctx.run {
      assertEquals(
        readValues[Int](sql"SELECT id FROM customers"),
        List(123)
      )

      val phoneNum = "061 123 456"
      assertEquals(
        readValues[Int](sql"SELECT id FROM phones WHERE number = ${phoneNum}"),
        List(1)
      )
    }
  }

  test("SELECT rows") {
    val ctx = initDb()

    ctx.run {

      // TODO parse + inject these aliases into query!
      assertEquals(
        readRows[CustomerWithPhone](sql"""
          SELECT c.id "c.id", c.name "c.name",
            p.id "p.id", p.number "p.number"
          FROM customers c
          JOIN phones p on p.customer_id = c.id
        """),
        List(
          CustomerWithPhone(
            Customer(123, "a_customer"),
            Phone(1, "061 123 456")
          ),
          CustomerWithPhone(
            Customer(123, "a_customer"),
            Phone(2, "062 225 883")
          )
        )
      )

    }
  }

}

case class Customer(id: Int, name: String) derives SqlReadRow

case class Phone(id: Int, number: String) derives SqlReadRow

case class CustomerWithPhone(c: Customer, p: Phone) derives SqlReadRow
