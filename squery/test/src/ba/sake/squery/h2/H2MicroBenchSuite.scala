package ba.sake.squery.h2

import ba.sake.squery.{*, given}

import java.time.Duration

// just to make sure queries run in a reasonable time
class H2MicroBenchSuite extends munit.FunSuite {
  val initDb = new Fixture[SqueryContext]("database") {
    private var ctx: SqueryContext = null

    def apply() = ctx

    override def beforeAll(): Unit = {

      val ds = com.zaxxer.hikari.HikariDataSource()
      ds.setJdbcUrl("jdbc:h2:mem:test_squery_micro_bench")

      ctx = SqueryContext(ds)

      ctx.run {
        sql"""
            CREATE TABLE customers(
              id SERIAL PRIMARY KEY,
              name VARCHAR NOT NULL,
              street VARCHAR(20)
            )
          """.update()
      }
    }
  }

  override def munitFixtures = List(initDb)

  test("Run 1000 INSERTs microbench") {
    val ctx = initDb()
    val start = System.nanoTime()
    for (i <- 1 to 1000) {
      ctx.run {
        sql"""
          INSERT INTO customers(name)
          VALUES ('abc')
        """.insert()
      }
    }
    val end = System.nanoTime()
    val total = Duration.ofNanos(end - start)
    assert(total.toMillis < 500, total)
  }

  test("Run 10000 SELECTs microbench") {
    val ctx = initDb()
    for (i <- 1 to 1000) {
      ctx.run {
        sql"""
          INSERT INTO customers(name)
          VALUES ('abc')
        """.insert()
      }
    }
    val start = System.nanoTime()
    for (i <- 1 to 1000) {
      ctx.run {
        sql"""
          SELECT id, name, street FROM customers WHERE id = ${i}
        """.readRow[Customer]()
      }
    }
    val end = System.nanoTime()
    val total = Duration.ofNanos(end - start)
    assert(total.toMillis < 500, total)
  }
  
}
