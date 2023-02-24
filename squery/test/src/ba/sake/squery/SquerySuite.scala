package ba.sake.squery

import javax.sql.DataSource

import ba.sake.squery.read.*

class SquerySuite extends munit.FunSuite {

  def initDb(): DataSource = {
    val ds = com.zaxxer.hikari.HikariDataSource()
    ds.setJdbcUrl("jdbc:sqlite::memory:")

    val conn = ds.getConnection()
    List(
      "PRAGMA foreign_keys = ON;",

      // customers
      """CREATE TABLE customers(
          id INTEGER PRIMARY KEY,
          name VARCHAR
      )""",
      "INSERT INTO customers VALUES(123, 'a_customer')",

      // phones
      """CREATE TABLE phones(
        id INTEGER PRIMARY KEY,
        customer_id INTEGER REFERENCES customers(id),
        number VARCHAR
      )""",
      "INSERT INTO phones VALUES(1, 123, '061 123 456')",
      "INSERT INTO phones VALUES(2, 123, '062 225 883')"
    ).foreach { stmtString =>
      val stmt = conn.prepareStatement(stmtString)
      stmt.execute()
      stmt.close()
    }
    conn.close()

    ds
  }

  test("SELECT simple types") {
    val ds = initDb()
    
    run(ds) {
      assertEquals(
        read[Int](Query("SELECT id FROM customers")),
        List(123)
      )

      assertEquals(
        read[Int](Query("SELECT id FROM phones")),
        List(1, 2)
      )

      // TODO parse + inject these aliases into query!
      assertEquals(
        read.rows[CustomerWithPhone](Query("""
          SELECT c.id "c.id", c.name "c.name",
            p.id "p.id", p.number "p.number"
          FROM customers c
          JOIN phones p on p.customer_id = c.id
        """)),
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
