package ba.sake.squery.sqlite

import ba.sake.squery.{*, given}
import java.nio.file.Files
import java.time.Instant
import java.util.UUID
import com.zaxxer.hikari.HikariDataSource

case class SqliteRow(
    id: Int,
    uuid: UUID,
    enabled: Boolean,
    happenedAt: Instant,
    nullableUuid: Option[UUID],
    nullableEnabled: Option[Boolean],
    nullableHappenedAt: Option[Instant],
    color: Color
) derives SqlReadRow

enum Color derives SqlRead, SqlWrite:
  case red, green, blue

class SqliteSuite extends munit.FunSuite:
  test("SQLite codecs round-trip scalar, nullable, and enum values") {
    import ba.sake.squery.sqlite.{*, given}

    val dbPath = Files.createTempFile("squery-sqlite-", ".db")
    val ds = HikariDataSource()
    ds.setJdbcUrl(s"jdbc:sqlite:${dbPath.toAbsolutePath}")
    try
      val ctx = SqueryContext(ds)
      val expected = SqliteRow(
        1,
        UUID.randomUUID(),
        enabled = true,
        Instant.parse("2024-01-02T03:04:05.678Z"),
        Some(UUID.randomUUID()),
        Some(false),
        None,
        Color.green
      )
      ctx.run {
        sql"""
          CREATE TABLE rows(
            id INTEGER PRIMARY KEY,
            uuid TEXT NOT NULL,
            enabled INTEGER NOT NULL,
            happenedAt TEXT NOT NULL,
            nullableUuid TEXT,
            nullableEnabled INTEGER,
            nullableHappenedAt TEXT,
            color TEXT NOT NULL
          )
        """.update()
        sql"""
          INSERT INTO rows VALUES (
            ${expected.id}, ${expected.uuid}, ${expected.enabled}, ${expected.happenedAt},
            ${expected.nullableUuid}, ${expected.nullableEnabled}, ${expected.nullableHappenedAt}, ${expected.color}
          )
        """.update()
        assertEquals(
          sql"SELECT id, uuid, enabled, happenedAt, nullableUuid, nullableEnabled, nullableHappenedAt, color FROM rows"
            .readRow[SqliteRow](),
          expected
        )
      }
    finally
      ds.close()
      Files.deleteIfExists(dbPath)
  }

  test("SQLite returns one generated key for one inserted row") {
    val dbPath = Files.createTempFile("squery-sqlite-", ".db")
    val ds = HikariDataSource()
    ds.setJdbcUrl(s"jdbc:sqlite:${dbPath.toAbsolutePath}")
    try
      val ctx = SqueryContext(ds)
      ctx.run {
        sql"CREATE TABLE generated_rows(id INTEGER PRIMARY KEY, value TEXT NOT NULL)".update()
        val keys = sql"INSERT INTO generated_rows(value) VALUES ('value')".insertReturningGenKeys[Int]()
        assertEquals(keys.toSeq, Seq(1))
      }
    finally
      ds.close()
      Files.deleteIfExists(dbPath)
  }
