package ba.sake.squery.cli

import java.nio.file.{Files, Path}
import java.sql.DriverManager
import scala.jdk.CollectionConverters._
import scala.util.Using
import mainargs.ParserForMethods
import munit.FunSuite

class SqueryMainSuite extends FunSuite {
  for (typed <- Seq(false, true)) {
    test(s"CLI generates sources with omitted table filters (ironNewtypes=$typed)") {
      withDatabase { (directory, jdbcUrl) =>
        val output = directory.resolve("generated")
        val args = Seq(
          "--jdbc-url",
          jdbcUrl,
          "--base-folder",
          output.toString,
          "--schema-mappings",
          "main:example.db"
        ) ++ (if (typed) Seq("--ironNewtypes") else Seq.empty)
        val result = ParserForMethods(SqueryMain).runEither(args)
        assert(result.isRight, result.toString)
        val row = Files.readString(output.resolve("example/db/models/OrdersRow.scala"))
        assertEquals(row.contains("user_id: UsersRow.PK"), typed)
        assertEquals(row.contains("squery.iron.given"), typed)
      }
    }
  }

  test("CLI honors explicit repeatable filters and rejects unknown options before output") {
    withDatabase { (directory, jdbcUrl) =>
      val output = directory.resolve("generated")
      val base = Seq("--jdbc-url", jdbcUrl, "--base-folder", output.toString, "--schema-mappings", "main:example.db")
      val bad = ParserForMethods(SqueryMain).runEither(base ++ Seq("--ironNewtypes", "--unknown-option"))
      assert(bad.isLeft)
      assert(!Files.exists(output))
      val result = ParserForMethods(SqueryMain).runEither(
        base ++ Seq("--includeTables", "main.users", "--includeTables", "main.orders", "--excludeTables", "main.orders")
      )
      assert(result.isRight, result.toString)
      assert(Files.exists(output.resolve("example/db/models/UsersRow.scala")))
      assert(!Files.exists(output.resolve("example/db/models/OrdersRow.scala")))
    }
  }

  private def withDatabase(body: (Path, String) => Unit): Unit = {
    val directory = Files.createTempDirectory("squery-cli-test-")
    val jdbcUrl = s"jdbc:sqlite:${directory.resolve("test.db")}"
    try {
      Using.resource(DriverManager.getConnection(jdbcUrl)) { conn =>
        Using.resource(conn.createStatement()) { stmt =>
          stmt.execute("CREATE TABLE users(id INTEGER PRIMARY KEY)")
          stmt.execute("CREATE TABLE orders(id INTEGER PRIMARY KEY, user_id INTEGER NOT NULL REFERENCES users(id))")
        }
      }
      body(directory, jdbcUrl)
    } finally {
      Using.resource(Files.walk(directory)) { files =>
        files.iterator().asScala.toSeq.sortBy(_.getNameCount).reverse.foreach(Files.delete(_))
      }
    }
  }
}
