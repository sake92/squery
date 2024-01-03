package ba.sake.squery

import java.util.UUID
import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.collection.decorators._
import org.testcontainers.containers.PostgreSQLContainer
import ba.sake.squery.write.SqlArgument

class PostgresSuite extends munit.FunSuite {

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
    private var container: PostgreSQLContainer[?] = null

    def apply() = ctx

    override def beforeAll(): Unit = {
      container = new PostgreSQLContainer("postgres:9.6.12")
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
            name VARCHAR NOT NULL,
            street VARCHAR(20)
          )
        """.update()

        sql"""
          CREATE TABLE phones(
            id SERIAL PRIMARY KEY,
            customer_id INTEGER REFERENCES customers(id),
            number VARCHAR
          )
        """.update()

        sql"""
          CREATE TABLE addresses(
            id SERIAL PRIMARY KEY,
            name VARCHAR
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
          INSERT INTO phones(customer_id, number) VALUES
            (${customer1.id}, ${phone1.number}),
            (${customer1.id}, ${phone2.number})
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
        sql"SELECT number FROM phones WHERE customer_id = ${customer1.id}"
          .readValues[String](),
        phones.map(_.number)
      )

      val q1 = sql""
      val q2 = sql" ${q1} "
    }
  }

  test("SELECT rows") {
    val ctx = initDb()
    ctx.run {
      // full join
      assertEquals(
        sql"""
          SELECT c.id, c.name, c.street,
            p.id, p.number
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
            p.id, p.number
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

  test("INSERT returning columns") {
    val ctx = initDb()
    ctx.run {
      val customers = sql"""
        INSERT INTO customers(name)
        VALUES ('abc'), ('def'), ('ghi')
        RETURNING id, name, street
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
      // enum
      sql"""
        CREATE TYPE color AS ENUM ('red', 'green', 'blue')
      """.update()
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
          tstz TIMESTAMPTZ,
          clr color
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
        .map(dt => sql"(${dt.int}, ${dt.long}, ${dt.double}, ${dt.boolean}, ${dt.string}, ${dt.uuid}, ${dt.tstz}, ${dt.clr})")
        .intersperse(sql",")
        .reduce(_ ++ _)
      sql"""
        INSERT INTO datatypes(int, long, double, boolean, string, uuid, tstz, clr)
        VALUES ${values}
      """.insert()

      val storedRows = sql"""
        SELECT int, long, double, boolean, string, uuid, tstz, clr
        FROM datatypes
      """.readRows[Datatypes]()
      assertEquals(
        storedRows,
        Seq(dt1)
      )
    }
  }

  // TODO transactions

  test("Log warnings") {
    val ctx = initDb()
    ctx.run {
      // custom squery warnings
      sql"UPDATE customers SET name='bla'".update()

      intercept[Exception] {
        sql"DELETE FROM customers".update()
      }

      // JDBC warning
      // TODO not triggering...
      sql"""
        CREATE OR REPLACE FUNCTION test_fun_warn() RETURNS integer AS $$$$
        BEGIN
          RAISE NOTICE 'this is a warningue!';
          RETURN 42;
        END;
        $$$$ LANGUAGE plpgsql;
      """.update()
      sql"SELECT * FROM test_fun_warn()".readValue[Int]()
    }
  }

}
