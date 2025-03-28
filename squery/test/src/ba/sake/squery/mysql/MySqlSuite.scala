package ba.sake.squery
package mysql

import java.util.UUID
import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.collection.decorators._
import org.testcontainers.containers.MySQLContainer

// UUID, enum.. MySql specific
case class Datatypes(
    d_int: Option[Int],
    d_long: Option[Long],
    d_double: Option[Double],
    d_boolean: Option[Boolean],
    d_string: Option[String],
    d_uuid: Option[UUID],
    d_tstz: Option[Instant],
    d_clr: Option[Color]
) derives SqlReadRow:
  def insertTuple = sql"(${d_int}, ${d_long}, ${d_double}, ${d_boolean}, ${d_string}, ${d_uuid}, ${d_tstz}, ${d_clr})"

object Datatypes:
  inline val allCols = "d_int, d_long, d_double, d_boolean, d_string, d_uuid, d_tstz, d_clr"

enum Color derives SqlRead, SqlWrite:
  case red, green, blue

class MySqlSuite extends munit.FunSuite {

  var customer1 = Customer(1, "a_customer", None)
  var customer2 = Customer(1, "b_customer", Some("str1"))
  val customers = Seq(customer1, customer2)

  var phone1 = Phone(1, "061 123 456")
  var phone2 = Phone(1, "062 225 883")
  val phones = Seq(phone1, phone2)

  var address1 = Address(1, Some("a1"))
  var address2 = Address(1, None)
  val addresses = Seq(address1, address2)

  val initDb = new Fixture[SqueryContext]("database") {
    private var ctx: SqueryContext = null
    private var container: MySQLContainer[?] = null

    def apply() = ctx

    override def beforeAll(): Unit = {
      container = MySQLContainer("mysql:8.2.0")
      container.start()

      val ds = com.zaxxer.hikari.HikariDataSource()
      ds.setJdbcUrl(container.getJdbcUrl())
      ds.setUsername(container.getUsername())
      ds.setPassword(container.getPassword())

      ctx = SqueryContext(ds)

      ctx.run {
        sql"""
          CREATE TABLE customers(
            id SERIAL PRIMARY KEY,
            name TEXT NOT NULL,
            street VARCHAR(20)
          )
        """.update()

        sql"""
          CREATE TABLE phones(
            id SERIAL PRIMARY KEY,
            customer_id INTEGER REFERENCES customers(id),
            numbr TEXT
          )
        """.update()

        sql"""
          CREATE TABLE addresses(
            id SERIAL PRIMARY KEY,
            name TEXT
          )
        """.update()

        sql"""
          CREATE TABLE customer_address(
            customer_id INTEGER REFERENCES customers(id),
            address_id INTEGER REFERENCES addresses(id),
            PRIMARY KEY (customer_id, address_id)
          )
        """.update()

        val customerIds = sql"""
          INSERT INTO customers(name, street)
          VALUES (${customer1.name}, ${customer1.street}),
                 (${customer2.name}, ${customer2.street})
        """.insertReturningGenKeys[Int]()
        customer1 = customer1.copy(id = customerIds(0))
        customer2 = customer2.copy(id = customerIds(1))

        val phoneIds = sql"""
          INSERT INTO phones(customer_id, numbr) VALUES
            (${customer1.id}, ${phone1.numbr}),
            (${customer1.id}, ${phone2.numbr})
        """.insertReturningGenKeys[Int]()
        phone1 = phone1.copy(id = phoneIds(0))
        phone2 = phone2.copy(id = phoneIds(1))

        val addressIds = sql"""
          INSERT INTO addresses(name) VALUES
            (${address1.name}),
            (${address2.name})
        """.insertReturningGenKeys[Int]()
        address1 = address1.copy(id = addressIds(0))
        address2 = address2.copy(id = addressIds(1))

        sql"""
          INSERT INTO customer_address(customer_id, address_id)
          VALUES
            (${customer1.id}, ${address1.id}),
            (${customer1.id}, ${address2.id})
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
        customers.map(_.name)
      )

      assertEquals(
        sql"SELECT numbr FROM phones WHERE customer_id = ${customer1.id}"
          .readValues[String](),
        phones.map(_.numbr)
      )
    }
  }

  test("SELECT rows") {
    val ctx = initDb()
    ctx.run {
      // full join
      assertEquals(
        sql"""
          SELECT c.id, c.name, c.street,
            p.id, p.numbr
          FROM customers c
          JOIN phones p ON p.customer_id = c.id
          WHERE c.id = ${customer1.id}
          ORDER BY c.id ASC, p.id ASC
        """.readRows[CustomerWithPhone](),
        Seq(
          CustomerWithPhone(customer1, phone1),
          CustomerWithPhone(customer1, phone2)
        )
      )

      // outer/optional join
      assertEquals(
        sql"""
          SELECT c.id, c.name, c.street,
            p.id, p.numbr
          FROM customers c
          LEFT JOIN phones p ON p.customer_id = c.id
          ORDER BY c.id ASC, p.id ASC
        """.readRows[CustomerWithPhoneOpt](),
        Seq(
          CustomerWithPhoneOpt(customer1, Some(phone1)),
          CustomerWithPhoneOpt(customer1, Some(phone2)),
          CustomerWithPhoneOpt(customer2, None)
        )
      )

      // outer/optional join with many-to-many
      assertEquals(
        sql"""
          SELECT c.id, c.name, c.street,
            a.id, a.name
          FROM customers c
          LEFT JOIN customer_address ca ON ca.customer_id = c.id
          LEFT JOIN addresses a ON a.id = ca.address_id
        """.readRows[CustomerWithAddressOpt](),
        Seq(
          CustomerWithAddressOpt(customer1, Some(address1)),
          CustomerWithAddressOpt(customer1, Some(address2)),
          CustomerWithAddressOpt(customer2, None)
        )
      )

    }
  }

  test("BAD SELECT throws") {
    val ctx = initDb()
    intercept[SqueryException] {
      // street is nullable, but CustomerBad says it's mandatory
      ctx.run {
        sql"""
          SELECT id, name, street
          FROM customers
        """.insertReturningRows[CustomerBad]()
      }
    }
  }

  test("INSERT returning generated keys") {
    val ctx = initDb()
    ctx.run {
      val customerIds = sql"""
        INSERT INTO customers(name)
        VALUES ('abc'), ('def'), ('ghi')
      """.insertReturningGenKeys[Int]()
      assertEquals(
        customerIds.toSet,
        (customer2.id + 1 to customer2.id + 3).toSet
      )
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
      sql"""
        CREATE TABLE datatypes(
          d_int INT,
          d_long BIGINT,
          d_double DOUBLE PRECISION,
          d_boolean BOOLEAN,
          d_string VARCHAR(255),
          d_uuid VARCHAR(36),
          d_tstz TIMESTAMP,
          d_clr ENUM('red', 'green', 'blue')
        )
      """.update()
      val dt1 = Datatypes(
        Some(123),
        Some(Int.MaxValue + 100),
        Some(0.54543),
        Some(true),
        Some("abc"),
        Some(UUID.randomUUID),
        Some(Instant.now.truncatedTo(ChronoUnit.SECONDS)),
        Some(Color.red)
      )
      val dt2 = Datatypes(None, None, None, None, None, None, None, None)

      val values = Seq(dt1, dt2)
        .map(_.insertTuple)
        .intersperse(sql",")
        .reduce(_ ++ _)
      sql"""
        INSERT INTO datatypes(${Datatypes.allCols})
        VALUES ${values}
      """.insert()

      val storedRows = sql"""
        SELECT ${Datatypes.allCols}
        FROM datatypes
      """.readRows[Datatypes]()
      assertEquals(
        storedRows,
        Seq(dt1)
      )
    }
  }

  test("Transaction") {
    val ctx = initDb()
    // create table normally
    ctx.run {
      sql"""
        CREATE TABLE test_transactions(
          name VARCHAR(32),
          UNIQUE(name)
        )
      """.update()
    }
    // all succeeds,
    // or nothing succeeds!
    intercept[Exception] {
      ctx.runTransaction {
        sql"""
          INSERT INTO test_transactions(name)
          VALUES ('abc')
        """.insert()
        // fail coz unique name
        sql"""
          INSERT INTO test_transactions(name)
          VALUES ('abc')
        """.insert()
      }
    }
    intercept[Exception] {
      ctx.runTransactionWithIsolation(TransactionIsolation.Serializable) {
        sql"""
          INSERT INTO test_transactions(name)
          VALUES ('abc')
        """.insert()
        // fail coz unique name
        sql"""
          INSERT INTO test_transactions(name)
          VALUES ('abc')
        """.insert()
      }
    }
    // check there is NO ENTRIES, coz transaction failed
    ctx.run {
      val values = sql"SELECT name FROM test_transactions".readValues[String]()
      assertEquals(values, Seq.empty)
    }
  }

  test("Log warnings") {
    val ctx = initDb()
    ctx.run {
      // custom squery warnings
      // no WHERE .. conditions
      sql"UPDATE customers SET name='bla'".update()

      sql"DELETE FROM customers".update()
    }
  }

}
