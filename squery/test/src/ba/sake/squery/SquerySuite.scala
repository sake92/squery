package ba.sake.squery

import javax.sql.DataSource

import ba.sake.squery.read.*
import java.util.UUID
import java.time.Instant
import ba.sake.squery.write.SqlArgument

class SquerySuite extends munit.FunSuite {

  val customer1 = Customer(1, "a_customer")
  val phone1 = Phone(1, "061 123 456")
  val phone2 = Phone(2, "062 225 883")

  def initDb(): SqueryContext = {
    val ds = com.zaxxer.hikari.HikariDataSource()
    ds.setJdbcUrl("jdbc:h2:mem:")
    val ctx = new SqueryContext(ds)

    ctx.run {
      sql"""
        CREATE TABLE customers(
          id INTEGER PRIMARY KEY AUTO_INCREMENT,
          name VARCHAR
        )
      """.update()

      sql"""
        CREATE TABLE phones(
          id INTEGER PRIMARY KEY AUTO_INCREMENT,
          customer_id INTEGER REFERENCES customers(id),
          number VARCHAR
        )
      """.update()
    }

    ctx
  }

  test("SELECT plain values") {
    val ctx = initDb()
    ctx.run {
      val customerIds = sql"""
        INSERT INTO customers(name) VALUES(${customer1.name})
      """.insertReturningValues[Int]()
      val customerId1 = customerIds.head
      sql"""
        INSERT INTO phones(customer_id, number) VALUES($customerId1, ${phone1.number})
      """.insert()
      sql"""
        INSERT INTO phones(customer_id, number) VALUES($customerId1, ${phone2.number})
      """.insert()

      assertEquals(
        sql"SELECT name FROM customers".readValues[String](),
        List(customer1.name)
      )

      assertEquals(
        sql"SELECT number FROM phones WHERE customer_id = ${customer1.id}"
          .readValues[String](),
        List(phone1.number, phone2.number)
      )
    }
  }

  test("SELECT rows") {
    val ctx = initDb()
    ctx.run {
      val customerIds = sql"""
        INSERT INTO customers(name) VALUES(${customer1.name})
      """.insertReturningValues[Int]()
      val customerId1 = customerIds.head
      sql"""
        INSERT INTO phones(customer_id, number) VALUES($customerId1, ${phone1.number})
      """.insert()
      sql"""
        INSERT INTO phones(customer_id, number) VALUES($customerId1, ${phone2.number})
      """.insert()

      assertEquals(
        sql"""
          SELECT c.id, c.name,
            p.id, p.number
          FROM customers c
          JOIN phones p on p.customer_id = c.id
        """.readRows[CustomerWithPhone](),
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
      ctx.run { sql"".insert() }
    }
  }

  test("INSERT returning generated keys") {
    val ctx = initDb()
    ctx.run {
      val customerIds = sql"""
        INSERT INTO customers(name)
        VALUES ('abc'), ('def'), ('ghi')
      """.insertReturningValues[Int]()
      assertEquals(customerIds.toSet, Set(1, 2, 3))
    }
  }

  test("UPDATE should return number of affected rows") {
    val ctx = initDb()
    ctx.run {
      val customerIds = sql"""
        INSERT INTO customers(name)
        VALUES ('a_1'), ('a_2'), ('b_1')
      """.insertReturningValues[Int]()
      val affected = sql"""
        UPDATE customers
        SET name = 'whatever'
        WHERE name LIKE 'a_%'
      """.update()
      assertEquals(affected, 2)
    }
  }

  test("Data types") {
    val ctx = initDb()
    ctx.run {
      // note that Insant has NANOseconds precision!
      sql"""
        CREATE TABLE datatypes(
          uuid UUID,
          tstz TIMESTAMP(9) WITH TIME ZONE
        )
      """.update()
      val uuid = UUID.randomUUID()
      val tstz = Instant.now()
      sql"""
        INSERT INTO datatypes(uuid, tstz)
        VALUES ($uuid, $tstz),  ($uuid, NULL)
      """.insert()

      val storedRows = sql"""
        SELECT uuid, tstz
        FROM datatypes
      """.readRows[Datatypes]()
      assertEquals(
        storedRows,
        List(
          Datatypes(uuid, Some(tstz)),
          Datatypes(uuid, None)
        )
      )
    }
  }

  test("Query concat") {
    val ctx = initDb()
    ctx.run {

      val p1 = "a_customer"
      val q1 = sql"""SELECT id FROM customers WHERE name = $p1"""

      val p2 = "a_customer2"
      val q2 = sql"""OR name = ${p2}"""

      val q = q1 ++ q2
      assertEquals(
        q.sqlString,
        """SELECT id FROM customers WHERE name = ? OR name = ?"""
      )
      assertEquals(q.arguments, Seq(p1, p2).map(SqlArgument(_)))
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
