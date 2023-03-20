package ba.sake.squery

import javax.sql.DataSource
import java.util.UUID
import java.time.Instant

import org.testcontainers.containers.PostgreSQLContainer

import ba.sake.squery.write.SqlArgument
import java.time.temporal.ChronoUnit

class SquerySuite extends munit.FunSuite {

  val customer1 = Customer(1, "a_customer")
  val phone1 = Phone(1, "061 123 456")
  val phone2 = Phone(2, "062 225 883")

  val initDb = new Fixture[SqueryContext]("database") {
    private var ctx: SqueryContext = null
    private var container: PostgreSQLContainer[?] = null

    def apply() = ctx

    override def beforeAll(): Unit = {
      val container = new PostgreSQLContainer()
      container.start()

      val ds = com.zaxxer.hikari.HikariDataSource()
      ds.setJdbcUrl(container.getJdbcUrl())
      ds.setUsername(container.getUsername())
      ds.setPassword(container.getPassword())

      ctx = new SqueryContext(ds)

      ctx.run {
        sql"""
          CREATE TABLE customers(
            id SERIAL PRIMARY KEY,
            name VARCHAR
          )
        """.update()

        sql"""
          CREATE TABLE phones(
            id SERIAL PRIMARY KEY,
            customer_id INTEGER REFERENCES customers(id),
            number VARCHAR
          )
        """.update()

        val customerIds = sql"""
          INSERT INTO customers(name) VALUES(${customer1.name})
        """.insertReturningGenKeys[Int]()
        val customerId1 = customerIds.head
        sql"""
          INSERT INTO phones(customer_id, number) VALUES($customerId1, ${phone1.number})
        """.insert()
        sql"""
          INSERT INTO phones(customer_id, number) VALUES($customerId1, ${phone2.number})
        """.insert()
      }
    }
    override def afterAll(): Unit =
      if container != null then container.close()

  }

  override def munitFixtures = List(initDb)

  /* TESTS */

  test("SELECT plain values") {
    val ctx = initDb()
    ctx.run {
      assertEquals(
        sql"SELECT name FROM customers".readValues[String](),
        Seq(customer1.name)
      )

      assertEquals(
        sql"SELECT number FROM phones WHERE customer_id = ${customer1.id}"
          .readValues[String](),
        Seq(phone1.number, phone2.number)
      )
    }
  }

  test("SELECT rows") {
    val ctx = initDb()
    ctx.run {

      assertEquals(
        sql"""
          SELECT c.id, c.name,
            p.id, p.number
          FROM customers c
          JOIN phones p on p.customer_id = c.id
        """.readRows[CustomerWithPhone](),
        Seq(
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
      """.insertReturningGenKeys[Int]()
      assertEquals(customerIds.toSet, Set(2, 3, 4))
    }
  }

  test("INSERT returning columns") {
    val ctx = initDb()
    ctx.run {
      val customers = sql"""
        INSERT INTO customers(name)
        VALUES ('abc'), ('def'), ('ghi')
        RETURNING id, name
      """.insertReturningRows[Customer]()
      assertEquals(customers.map(_.name).toSet, Set("abc", "def", "ghi"))
    }
  }

  test("UPDATE should return number of affected rows") {
    val ctx = initDb()
    ctx.run {
      sql"""
        INSERT INTO customers(name)
        VALUES ('xyz_1'), ('xyz_2'), ('b_1')
      """.insert()
      val affected = sql"""
        UPDATE customers
        SET name = 'whatever'
        WHERE name LIKE 'xyz_%'
      """.update()
      assertEquals(affected, 2)
    }
  }

  test("Data types") {
    val ctx = initDb()
    ctx.run {
      // note that Instant has NANOseconds precision!
      // postgres has MICROseconds precision
      sql"""
        CREATE TABLE datatypes(
          int INTEGER,
          long BIGINT,
          double DOUBLE PRECISION,
          boolean BOOLEAN,
          string VARCHAR(255),
          uuid UUID,
          tstz TIMESTAMPTZ
        )
      """.update()
      val dt1 = Datatypes(
        Some(123),
        Some(Int.MaxValue + 100),
        Some(0.54543),
        Some(true),
        Some("abc"),
        Some(UUID.randomUUID),
        Some(Instant.now.truncatedTo(ChronoUnit.MICROS))
      )
      val dt2 = Datatypes(None, None, None, None, None, None, None)
      sql"""
        INSERT INTO datatypes(int, long, double, boolean, string, uuid, tstz)
        VALUES (${dt1.int}, ${dt1.long}, ${dt1.double}, ${dt1.boolean}, ${dt1.string}, ${dt1.uuid}, ${dt1.tstz}), 
               (${dt2.int}, ${dt2.long}, ${dt2.double}, ${dt2.boolean}, ${dt2.string}, ${dt2.uuid}, ${dt2.tstz})
      """.insert()

      val storedRows = sql"""
        SELECT int, long, double, boolean, string, uuid, tstz
        FROM datatypes
      """.readRows[Datatypes]()
      assertEquals(
        storedRows,
        Seq(dt1, dt2)
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
    int: Option[Int],
    long: Option[Long],
    double: Option[Double],
    boolean: Option[Boolean],
    string: Option[String],
    uuid: Option[UUID],
    tstz: Option[Instant]
) derives SqlReadRow
