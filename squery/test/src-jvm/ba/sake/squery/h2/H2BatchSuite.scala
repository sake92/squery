package ba.sake.squery
package h2

import java.sql.BatchUpdateException
import com.zaxxer.hikari.HikariDataSource

class H2BatchSuite extends munit.FunSuite:

  private val dataSource = HikariDataSource()
  dataSource.setJdbcUrl("jdbc:h2:mem:test_squery_batch;DB_CLOSE_DELAY=-1")
  private val ctx = SqueryContext(dataSource)

  override def afterAll(): Unit =
    dataSource.close()

  test("batch insert and update") {
    ctx.run {
      sql"CREATE TABLE customers(id INTEGER PRIMARY KEY, name VARCHAR NOT NULL)".update()

      val insertCounts = Seq(1 -> "Alice", 2 -> "Bob", 3 -> "Carol")
        .map { (id, name) =>
          sql"INSERT INTO customers(id, name) VALUES ($id, $name)"
        }
        .batchUpdate()
      assertEquals(insertCounts, Seq(1, 1, 1))

      val updateCounts = Seq(1 -> "Alicia", 2 -> "Robert")
        .map { (id, name) =>
          sql"UPDATE customers SET name = $name WHERE id = $id"
        }
        .batchUpdate()
      assertEquals(updateCounts, Seq(1, 1))

      assertEquals(
        sql"SELECT name FROM customers ORDER BY id".readValues[String](),
        Seq("Alicia", "Robert", "Carol")
      )
    }
  }

  test("empty batch") {
    ctx.run {
      assertEquals(Seq.empty[Query].batchUpdate(), Seq.empty)
    }
  }

  test("reject queries with different SQL before executing the batch") {
    ctx.run {
      sql"CREATE TABLE mismatched_batch(id INTEGER PRIMARY KEY, name VARCHAR NOT NULL)".update()

      val error = intercept[SqueryException] {
        Seq(
          sql"INSERT INTO mismatched_batch(id, name) VALUES (${1}, ${"Alice"})",
          sql"UPDATE mismatched_batch SET name = ${"Robert"} WHERE id = ${1}"
        ).batchUpdate()
      }

      assert(error.getMessage.contains("query at index 1 differs"))
      assertEquals(sql"SELECT COUNT(*) FROM mismatched_batch".readValue[Int](), 0)
    }
  }

  test("transaction rolls back a failed batch") {
    ctx.run {
      sql"CREATE TABLE transactional_batch(id INTEGER PRIMARY KEY, name VARCHAR NOT NULL)".update()
    }

    intercept[BatchUpdateException] {
      ctx.runTransaction {
        Seq(1 -> "Alice", 1 -> "Duplicate")
          .map { (id, name) =>
            sql"INSERT INTO transactional_batch(id, name) VALUES ($id, $name)"
          }
          .batchUpdate()
      }
    }

    ctx.run {
      assertEquals(sql"SELECT COUNT(*) FROM transactional_batch".readValue[Int](), 0)
    }
  }
