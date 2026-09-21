package ba.sake.squery.sqlite

import ba.sake.squery.{*, given}
import org.sqlite.SQLiteDataSource

class NativeSqliteSuite extends munit.FunSuite:
  test("SQLite round trip") {
    val ds = SQLiteDataSource()
    ds.setUrl("jdbc:sqlite::memory:")

    val ctx = SqueryContext(ds)
    ctx.run {
      sql"CREATE TABLE customers(id INTEGER PRIMARY KEY, name TEXT NOT NULL)".update()
      val name = "Scala Native"
      sql"INSERT INTO customers(id, name) VALUES (1, $name)".update()

      assertEquals(
        sql"SELECT name FROM customers WHERE id = 1".readValue[String](),
        name
      )
    }
  }
