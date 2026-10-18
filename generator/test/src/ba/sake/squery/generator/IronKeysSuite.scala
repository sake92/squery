package ba.sake.squery.generator

import java.sql.{Connection, DriverManager}
import scala.util.Using
import munit.FunSuite

class IronKeysSuite extends FunSuite {
  test("newtypes are opt-in and FK columns reuse the referenced PK") {
    withSchema(
      Seq(
        "CREATE TABLE users(id INTEGER PRIMARY KEY, name TEXT NOT NULL)",
        "CREATE TABLE orders(id INTEGER PRIMARY KEY, user_id INTEGER NOT NULL REFERENCES users(id))"
      )
    ) { conn =>
      val legacy = new SqueryGenerator(conn).generateString(Seq("main"))
      assert(legacy.contains("type PK = Long"), legacy)
      assert(!legacy.contains("squery.iron"), legacy)
      val typed = generate(conn)
      assert(typed.contains("id: UsersRow.PK"), typed)
      assert(typed.contains("user_id: UsersRow.PK"), typed)
      assert(typed.contains("id: OrdersRow.PK"), typed)
      assert(typed.contains("def findById(id: UsersRow.PK)"), typed)
      assert(typed.contains("import ba.sake.squery.iron.given"), typed)
      assert(typed.contains("import io.github.iltotore.iron.RefinedType"), typed)
      assert(typed.contains("import io.github.iltotore.iron.constraint.any.Pure"), typed)
      assert(typed.contains("object PK extends RefinedType[Long, Pure]"), typed)
      assert(!legacy.contains("io.github.iltotore.iron"), legacy)
    }
  }

  test("shared PK/FK chains and cycles have a single key owner") {
    withSchema(
      Seq(
        "CREATE TABLE users(id INTEGER PRIMARY KEY)",
        "CREATE TABLE profiles(id INTEGER PRIMARY KEY REFERENCES users(id))",
        "CREATE TABLE settings(id INTEGER PRIMARY KEY REFERENCES profiles(id))",
        "CREATE TABLE a(id INTEGER PRIMARY KEY REFERENCES b(id))",
        "CREATE TABLE b(id INTEGER PRIMARY KEY REFERENCES a(id))"
      )
    ) { conn =>
      val generated = generate(conn)
      assertEquals(generated.sliding("type PK = UsersRow.PK".length).count(_ == "type PK = UsersRow.PK"), 2)
      assert(generated.contains("val PK = UsersRow.PK"), generated)
      assert(generated.contains("type PK = ARow.PK"), generated)
      assert(!generated.contains("type PK = BRow.PK"), generated)
    }
  }

  test("SQLite implicit FK target columns resolve in primary key order") {
    withSchema(
      Seq(
        "CREATE TABLE parents(tenant_id INTEGER NOT NULL, id INTEGER NOT NULL, PRIMARY KEY(tenant_id, id))",
        "CREATE TABLE children(tenant INTEGER, parent INTEGER, FOREIGN KEY(tenant, parent) REFERENCES parents)"
      )
    ) { conn =>
      val generated = generate(conn)
      assert(generated.contains("tenant: Option[ParentsRow.TenantIdKey]"), generated)
      assert(generated.contains("parent: Option[ParentsRow.IdKey]"), generated)
    }
  }

  test("a column referencing distinct primary keys is rejected") {
    withSchema(
      Seq(
        "CREATE TABLE users(id INTEGER PRIMARY KEY)",
        "CREATE TABLE orders(id INTEGER PRIMARY KEY)",
        "CREATE TABLE ambiguous(id INTEGER REFERENCES users(id) REFERENCES orders(id))"
      )
    ) { conn =>
      val error = intercept[IllegalArgumentException](generate(conn))
      assert(error.getMessage.contains("main.ambiguous.id"), error.getMessage)
      assert(error.getMessage.contains("distinct primary keys"), error.getMessage)
    }
  }

  test("references to unique non-PK columns keep their mapped type") {
    withSchema(
      Seq(
        "CREATE TABLE users(id INTEGER PRIMARY KEY, code TEXT UNIQUE NOT NULL)",
        "CREATE TABLE refs(code TEXT REFERENCES users(code))"
      )
    ) { conn =>
      val generated = generate(conn)
      assert(generated.contains("case class RefsRow(code: Option[String])"), generated)
      assert(!generated.contains("def pk: RefsRow.PK"), generated)
    }
  }

  private def generate(conn: Connection): String =
    new SqueryGenerator(conn, SqueryGeneratorConfig.Default.copy(newtypes = Newtypes.Iron)).generateString(Seq("main"))

  private def withSchema(statements: Seq[String])(body: Connection => Unit): Unit =
    Using.resource(DriverManager.getConnection("jdbc:sqlite::memory:")) { conn =>
      Using.resource(conn.createStatement())(stmt => statements.foreach(stmt.execute))
      body(conn)
    }
}
