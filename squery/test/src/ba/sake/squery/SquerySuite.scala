package ba.sake.squery

import javax.sql.DataSource

import ba.sake.squery.read.*
import java.util.UUID
import java.time.Instant

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
      val customerIds = insertReturningKeys[Int](sql"""
        INSERT INTO customers(name) VALUES(${customer1.name})
      """)
      val customerId1 = customerIds.head
      insert(sql"""
        INSERT INTO phones(customer_id, number) VALUES($customerId1, ${phone1.number})
      """)
      insert(sql"""
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
      val customerIds = insertReturningKeys[Int](sql"""
        INSERT INTO customers(name) VALUES(${customer1.name})
      """)
      val customerId1 = customerIds.head
      insert(sql"""
        INSERT INTO phones(customer_id, number) VALUES($customerId1, ${phone1.number})
      """)
      insert(sql"""
        INSERT INTO phones(customer_id, number) VALUES($customerId1, ${phone2.number})
      """)

      assertEquals(
        readRows[CustomerWithPhone](sql"""
          SELECT c.id, c.name,
            p.id, p.number
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

  test("INSERT throws if statement is wrong") {
    val ctx = initDb()
    intercept[SqueryException] {
      ctx.run { insert(sql"") }
    }
  }

  test("INSERT returning generated keys") {
    val ctx = initDb()
    ctx.run {
      val customerIds = insertReturningKeys[Int](sql"""
        INSERT INTO customers(name)
        VALUES ('abc'), ('def'), ('ghi')
      """)
      assertEquals(customerIds.toSet, Set(1, 2, 3))
    }
  }

  test("UPDATE should return number of affected rows") {
    val ctx = initDb()
    ctx.run {
      val customerIds = insertReturningKeys[Int](sql"""
        INSERT INTO customers(name)
        VALUES ('a_1'), ('a_2'), ('b_1')
      """)
      val affected = update(sql"""
        UPDATE customers
        SET name = 'whatever'
        WHERE name LIKE 'a_%'
      """)
      assertEquals(affected, 2)
    }
  }

  test("Data types") {
    val ctx = initDb()
    ctx.run {
      // note that Insant has NANOseconds precision!
      update(sql"""
        CREATE TABLE datatypes(
          uuid UUID,
          tstz TIMESTAMP(9) WITH TIME ZONE
        )
      """)
      val uuid = UUID.randomUUID()
      val tstz = Instant.now()
      insert(sql"""
        INSERT INTO datatypes(uuid, tstz)
        VALUES ($uuid, $tstz),  ($uuid, NULL)
      """)

      val storedRows = readRows[Datatypes](sql"""
        SELECT uuid, tstz
        FROM datatypes
      """)
      assertEquals(
        storedRows,
        List(
          Datatypes(uuid, Some(tstz)),
          Datatypes(uuid, None)
        )
      )
    }
  }

}

case class Customer(id: Int, name: String) derives SqlReadRow

case class Phone(id: Int, number: String) derives SqlReadRow

case class CustomerWithPhone(c: Customer, p: Phone) derives SqlReadRow

case class Datatypes(
    uuid: UUID,
    tstz: Option[Instant]
) derives SqlReadRow
