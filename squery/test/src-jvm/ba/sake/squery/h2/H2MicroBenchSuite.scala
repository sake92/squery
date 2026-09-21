package ba.sake.squery.h2

import ba.sake.squery.{*, given}

import java.time.Duration

// just to make sure queries run in a reasonable time
class H2MicroBenchSuite extends munit.FunSuite {
  def initDb(name: String) = {
    val ds = com.zaxxer.hikari.HikariDataSource()
    val dbName = "jdbc:h2:mem:test_squery_" + name.replaceAll("\\s", "_")
    ds.setJdbcUrl(dbName)
    val ctx = SqueryContext(ds)
    ctx.run {
      sql"""
            CREATE TABLE customers(
              id SERIAL PRIMARY KEY,
              name VARCHAR NOT NULL,
              street VARCHAR(20)
            )
          """.update()
    }
    ctx
  }

  test("Run 1000 INSERTs microbench") {
    val ctx = initDb("1000 INSERTs")
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
    assert(total.toMillis < 1000, total)
  }

  test("Run 10000 SELECTs microbench") {
    val ctx = initDb("10000 SELECTs")
    val totalItems = 10_000
    for (i <- 1 to totalItems) {
      ctx.run {
        sql"""
          INSERT INTO customers(name)
          VALUES ('abc')
        """.insert()
      }
    }
    val start = System.nanoTime()
    for (i <- 1 to totalItems) {
      ctx.run {
        sql"""
          SELECT id, name, street FROM customers WHERE id = ${i}
        """.readRow[Customer]()
      }
    }
    val end = System.nanoTime()
    val total = Duration.ofNanos(end - start)
    assert(total.toMillis < 2000, total)
  }

}
