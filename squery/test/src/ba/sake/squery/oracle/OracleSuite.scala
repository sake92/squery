package ba.sake.squery
package oracle

import java.util.UUID
import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.collection.decorators._
import org.testcontainers.oracle.OracleContainer

// UUID, enum.. Postgres specific
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

class OracleSuite extends munit.FunSuite {

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
    private var container: OracleContainer = null

    def apply() = ctx

    override def beforeAll(): Unit = {
      container = OracleContainer("gvenzl/oracle-free:slim-faststart")
      container.start()

      val ds = com.zaxxer.hikari.HikariDataSource()
      ds.setJdbcUrl(container.getJdbcUrl())
      ds.setUsername(container.getUsername())
      ds.setPassword(container.getPassword())

      ctx = SqueryContext(ds)

      ctx.run {
        sql"""
          CREATE TABLE customers(
            id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
            name VARCHAR(32) NOT NULL,
            street VARCHAR(20)
          )
        """.update()

        sql"""
          CREATE TABLE phones(
            id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
            customer_id INTEGER REFERENCES customers(id),
            numbr VARCHAR(32)
          )
        """.update()

        sql"""
          CREATE TABLE addresses(
            id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
            name VARCHAR(32)
          )
        """.update()

        sql"""
          CREATE TABLE customer_address(
            customer_id INTEGER REFERENCES customers(id),
            address_id INTEGER REFERENCES addresses(id),
            PRIMARY KEY (customer_id, address_id)
          )
        """.update()

        // TODO document
        // oracle supports RETURNING, but only for single-row inserts..
        // https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/INSERT.html#GUID-903F8043-0254-4EE9-ACC1-CB8AC0AF3423

        // plus, you have to give it the autogenerated column name....
        // https://stackoverflow.com/a/55178347/4496364

        val customer1Id = sql"""
          INSERT INTO customers(name, street)
          VALUES ${customer1.insertTuple}
        """.insertReturningGenKey[Int](Some("id"))
        val customer2Id = sql"""
          INSERT INTO customers(name, street)
          VALUES ${customer2.insertTuple}
        """.insertReturningGenKey[Int](Some("id"))
        customer1 = customer1.copy(id = customer1Id)
        customer2 = customer2.copy(id = customer2Id)

        val phoneId1 = sql"""
          INSERT INTO phones(customer_id, numbr)
          VALUES ${phone1.insertTuple(customer1.id)}
        """.insertReturningGenKey[Int](Some("id"))
        val phoneId2 = sql"""
          INSERT INTO phones(customer_id, numbr)
          VALUES ${phone2.insertTuple(customer1.id)}
        """.insertReturningGenKey[Int](Some("id"))
        phone1 = phone1.copy(id = phoneId1)
        phone2 = phone2.copy(id = phoneId2)

        val addressId1 = sql"""
          INSERT INTO addresses(name)
          VALUES ${address1.insertTuple}
        """.insertReturningGenKey[Int](Some("id"))
        val addressId2 = sql"""
          INSERT INTO addresses(name)
          VALUES ${address2.insertTuple}
        """.insertReturningGenKey[Int](Some("id"))
        address1 = address1.copy(id = addressId1)
        address2 = address2.copy(id = addressId2)

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
      val customerId = sql"""
        INSERT INTO customers(name)
        VALUES ('abc')
      """.insertReturningGenKey[Int](Some("id"))
      assertEquals(
        customerId,
        customer2.id + 1
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
          d_int INTEGER,
          d_long INTEGER,
          d_double DOUBLE PRECISION,
          d_boolean BOOLEAN,
          d_string VARCHAR(255),
          d_uuid VARCHAR2(36),
          d_tstz TIMESTAMP WITH TIME ZONE,
          d_clr VARCHAR(10) CHECK (d_clr IN ('red', 'green', 'blue'))
        )
      """.update()

      val dt1 = Datatypes(
        Some(123),
        Some(Int.MaxValue + 100),
        Some(0.54543),
        Some(true),
        Some("abc"),
        Some(UUID.randomUUID),
        Some(Instant.now.truncatedTo(ChronoUnit.MICROS)),
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

}