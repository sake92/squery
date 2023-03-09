package ba.sake.squery

import javax.sql.DataSource

import ba.sake.squery.read.*

class SquerySuite extends munit.FunSuite {

  val customer1 = Customer(1, "a_customer")
  val phone1 = Phone(1, "061 123 456")
  val phone2 = Phone(2, "062 225 883")

  def initDb(): SqueryContext = {
    val ds = com.zaxxer.hikari.HikariDataSource()
    ds.setJdbcUrl("jdbc:h2:mem:")
    val ctx = new SqueryContext(ds)

    ctx.run {
      update(sql"""
        CREATE TABLE customers(
          id INTEGER PRIMARY KEY AUTO_INCREMENT,
          name VARCHAR
        )
      """)

      update(sql"""
        CREATE TABLE phones(
          id INTEGER PRIMARY KEY AUTO_INCREMENT,
          customer_id INTEGER REFERENCES customers(id),
          number VARCHAR
        )
      """)
    }

    ctx
  }

  test("SELECT plain values") {
    val ctx = initDb()

    ctx.run {
      val customerIds = insertReturningValues[Int](sql"""
        INSERT INTO customers(name) VALUES(${customer1.name})
      """)
      val customerId1 = customerIds.head
      update(sql"""
        INSERT INTO phones(customer_id, number) VALUES($customerId1, ${phone1.number})
      """)
      update(sql"""
        INSERT INTO phones(customer_id, number) VALUES($customerId1, ${phone2.number})
      """)

      assertEquals(
        readValues[String](sql"SELECT name FROM customers"),
        List(customer1.name)
      )

      assertEquals(
        readValues[String](
          sql"SELECT number FROM phones WHERE customer_id = ${customer1.id}"
        ),
        List(phone1.number, phone2.number)
      )
    }
  }

  test("SELECT rows") {
    val ctx = initDb()

    ctx.run {

      val customerIds = insertReturningValues[Int](sql"""
        INSERT INTO customers(name) VALUES(${customer1.name})
      """)
      val customerId1 = customerIds.head
      update(sql"""
        INSERT INTO phones(customer_id, number) VALUES($customerId1, ${phone1.number})
      """)
      update(sql"""
        INSERT INTO phones(customer_id, number) VALUES($customerId1, ${phone2.number})
      """)

      // TODO parse + inject these aliases into query!
      assertEquals(
        readRows[CustomerWithPhone](sql"""
          SELECT c.id "c.id", c.name "c.name",
            p.id "p.id", p.number "p.number"
          FROM customers c
          JOIN phones p on p.customer_id = c.id
        """),
        List(
          CustomerWithPhone(customer1, phone1),
          CustomerWithPhone(customer1, phone2)
        )
      )

    }
  }

  test("INSERT returning generated keys") {
    val ctx = initDb()

    ctx.run {

      val customerIds = insertReturningValues[Int](sql"""
        INSERT INTO customers(name)
        VALUES ('abc'), ('def'), ('ghi')
      """)

      assertEquals(customerIds.toSet, Set(1, 2, 3))
    }
  }

}

case class Customer(id: Int, name: String) derives SqlReadRow

case class Phone(id: Int, number: String) derives SqlReadRow

case class CustomerWithPhone(c: Customer, p: Phone) derives SqlReadRow
