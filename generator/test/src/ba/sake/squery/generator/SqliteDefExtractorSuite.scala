package ba.sake.squery.generator

import java.sql.DriverManager
import munit.FunSuite
import scala.meta._

class SqliteDefExtractorSuite extends FunSuite {
  private def connection = {
    Class.forName("org.sqlite.JDBC")
    val connection = DriverManager.getConnection("jdbc:sqlite::memory:")
    val statement = connection.createStatement()
    statement.executeUpdate("""
      CREATE TABLE main.users (
        tenant_id INTEGER NOT NULL,
        id INTEGER NOT NULL,
        name TEXT,
        score REAL,
        payload BLOB,
        amount NUMERIC,
        is_active INTEGER,
        created_at TEXT,
        birth_date TEXT,
        custom_id TEXT,
        has_flag INT,
        any_value ANY,
        mystery CUSTOM,
        is_integer8 INTEGER(8),
        has_myint MYINT,
        can_flag UNSIGNED BIG INT,
        PRIMARY KEY (tenant_id, id)
      )
    """)
    statement.close()
    connection
  }

  test("extracts SQLite main schema with storage mappings and ordered primary key") {
    val conn = connection
    try {
      val dbDef = new SqliteDefExtractor(conn).extract()
      assertEquals(dbDef.tpe, DbType.SQLite)
      assertEquals(dbDef.schemas.map(_.name), Seq("main"))
      val table = dbDef.schemas.head.tables.find(_.name == "users").get
      assertEquals(table.pkColumns.map(_.metadata.name), Seq("tenant_id", "id"))
      val types = table.columnDefs.map(c => c.metadata.name -> c.scalaType.name).toMap
      assertEquals(types("id"), "Long")
      assertEquals(types("score"), "Double")
      assertEquals(types("name"), "String")
      assertEquals(types("payload"), "Array[Byte]")
      assertEquals(types("amount"), "NUMERIC")
      assertEquals(types("is_active"), "Boolean")
      assertEquals(types("created_at"), "Instant")
      assertEquals(types("birth_date"), "LocalDate")
      assertEquals(types("has_flag"), "Boolean")
      assertEquals(types("any_value"), "ANY")
      assertEquals(types("mystery"), "CUSTOM")
      assertEquals(types("is_integer8"), "Boolean")
      assertEquals(types("has_myint"), "Boolean")
      assertEquals(types("can_flag"), "Boolean")
    } finally conn.close()
  }

  test("custom SQLite rules take precedence and generated main code imports SQLite") {
    val conn = connection
    try {
      val config = SqueryGeneratorConfig.Default.copy(
        sqliteTypeMappingRules = Seq(
          SqliteTypeMappingRule("custom_id", "INTEGER", t"Long"),
          SqliteTypeMappingRule("custom_id", "TEXT", t"UUID", Seq("java.util.UUID")),
          SqliteTypeMappingRule("custom_id", "TEXT", t"String")
        )
      )
      val generated = new SqueryGenerator(conn, config).generateString(Seq("main"))
      assert(generated.contains("import ba.sake.squery.sqlite.{ *, given }"), generated)
      assert(generated.contains("main.users"))
      assert(generated.contains("custom_id: Option[UUID]"))
      assert(!generated.contains("custom_id: Option[String]"))
    } finally conn.close()
  }

  test("custom SQLite rule declared type guard rejects mismatches") {
    val conn = connection
    try {
      val dbDef = new SqliteDefExtractor(conn, Seq(SqliteTypeMappingRule("custom_id", "INTEGER", t"Long"))).extract()
      val custom = dbDef.schemas.head.tables.head.columnDefs.find(_.metadata.name == "custom_id").get
      assertEquals(custom.scalaType.name, "String")
    } finally conn.close()
  }

  test("SQLite extraction excludes temporary tables") {
    val conn = connection
    try {
      val statement = conn.createStatement()
      statement.executeUpdate("CREATE TEMP TABLE temp_only (id INTEGER)")
      statement.close()
      val tables = new SqliteDefExtractor(conn).extract().schemas.head.tables.map(_.name)
      assert(!tables.contains("temp_only"))
    } finally conn.close()
  }

  test("generated identity columns are omitted from generated inserts") {
    Class.forName("org.sqlite.JDBC")
    val conn = DriverManager.getConnection("jdbc:sqlite::memory:")
    try {
      val statement = conn.createStatement()
      statement.executeUpdate("""
        CREATE TABLE generated_rows (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          name TEXT NOT NULL
        )
      """)
      statement.close()

      val generated = new SqueryGenerator(conn).generateString(Seq("main"))
      assert(generated.contains("INSERT INTO main.generated_rows(name)"), generated)
      assert(!generated.contains("INSERT INTO main.generated_rows(id, name)"), generated)
    } finally conn.close()
  }
}
