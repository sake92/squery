package ba.sake.squery

import ba.sake.squery.read.*


class SquerySuite extends munit.FunSuite {

  test("SELECT simple types") {
    val ds = com.zaxxer.hikari.HikariDataSource()
    ds.setJdbcUrl("jdbc:sqlite::memory:")

    val conn = ds.getConnection()
    List(
      "PRAGMA foreign_keys = ON;",

      // customers
      "CREATE TABLE customers(id INTEGER PRIMARY KEY, name VARCHAR)",
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

    run(ds) {
      assertEquals(
        read[Int](Query("SELECT id FROM customers")),
        List(123)
      )
     /* 
      assertEquals(
        read[String](Query("SELECT name FROM customers")),
        List("a_customer")
      )

      
*/

      val res2 = read.rows[CustomerWithPhone](Query("""
        SELECT c.id "c.id", c.name "c.name",
          p.id "p.id", p.number "p.number"
        FROM customers c
        JOIN phones p on p.customer_id = c.id
      """))
      println(res2.mkString("\n"))
    }

  }

}

case class Customer(id: Int, name: String)
object Customer {
  given SqlReadRow[Customer] = new {
    def readRow(jRes: java.sql.ResultSet, prefix: Option[String]): Customer = {
      Customer(
        SqlRead[Int].readByName(jRes, prefix.map(_ + ".").getOrElse("") + "id"),
        SqlRead[String].readByName(jRes, prefix.map(_ + ".").getOrElse("") +"name")
      )
    }
  }
}


case class Phone(id: Int, number: String)

object Phone {
  given SqlReadRow[Phone] = new {
    def readRow(jRes: java.sql.ResultSet, prefix: Option[String]): Phone = {
      Phone(
        SqlRead[Int].readByName(jRes, prefix.map(_ + ".").getOrElse("") +"id"),
        SqlRead[String].readByName(jRes, prefix.map(_ + ".").getOrElse("") +"number")
      )
    }
  }
}


case class CustomerWithPhone(c: Customer, p: Phone)
object CustomerWithPhone {
  given SqlReadRow[CustomerWithPhone] = new {
    def readRow(jRes: java.sql.ResultSet, prefix: Option[String]): CustomerWithPhone = {
      CustomerWithPhone(
        SqlReadRow[Customer].readRow(jRes, Some("c")),
        SqlReadRow[Phone].readRow(jRes, Some("p"))
      )
    }
  }
}
