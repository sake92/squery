package ba.sake.squery.sqlite

import ba.sake.squery.{*, given}
import java.time.{Instant, LocalDate, LocalDateTime}
import java.util.UUID
import org.sqlite.SQLiteDataSource

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

    withContext { ctx =>
      ctx.run {
        val expected = SqliteRow(
          1,
          UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
          enabled = true,
          Instant.parse("2024-01-02T03:04:05.678Z"),
          Some(UUID.fromString("123e4567-e89b-12d3-a456-426614174001")),
          Some(false),
          None,
          Color.green
        )
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
    }
  }

  test("SQLite dates and local date-times use ISO text and preserve nulls") {
    import ba.sake.squery.sqlite.{*, given}

    withContext { ctx =>
      ctx.run {
        val date = LocalDate.parse("2024-02-29")
        val dateTime = LocalDateTime.parse("2024-02-29T12:34:56.123456789")
        sql"CREATE TABLE dates(birth_date TEXT, local_time TEXT)".update()
        sql"INSERT INTO dates VALUES ($date, $dateTime)".update()
        assertEquals(sql"SELECT birth_date FROM dates".readValue[String](), "2024-02-29")
        assertEquals(sql"SELECT local_time FROM dates".readValue[String](), dateTime.toString)
        assertEquals(sql"SELECT birth_date FROM dates".readValue[LocalDate](), date)
        assertEquals(sql"SELECT local_time FROM dates".readValue[LocalDateTime](), dateTime)
        assertEquals(sql"SELECT '2024-02-29'".readValue[LocalDate](), date)
        assertEquals(sql"SELECT date('2024-02-29')".readValue[LocalDate](), date)
        sql"INSERT INTO dates VALUES (${Option.empty[LocalDate]}, ${Option.empty[LocalDateTime]})".update()
        assertEquals(sql"SELECT birth_date FROM dates WHERE birth_date IS NULL".readValue[Option[LocalDate]](), None)
        assertEquals(sql"SELECT local_time FROM dates WHERE local_time IS NULL".readValue[Option[LocalDateTime]](), None)
        val dateRead = summon[SqlRead[LocalDate]]
        val dateTimeRead = summon[SqlRead[LocalDateTime]]
        val statement = summon[SqueryConnection].underlying.createStatement()
        try {
          val rs = statement.executeQuery("SELECT birth_date, local_time FROM dates")
          try {
            assert(rs.next())
            assertEquals(dateRead.readByName(rs, "birth_date"), Some(date))
            assertEquals(dateTimeRead.readByName(rs, "local_time"), Some(dateTime))
            assert(rs.next())
            assertEquals(dateRead.readByName(rs, "birth_date"), None)
            assertEquals(dateTimeRead.readByName(rs, "local_time"), None)
          } finally rs.close()
        } finally statement.close()
      }
    }
  }

  test("SQLite returns one generated key for one inserted row") {
    withContext { ctx =>
      ctx.run {
        sql"CREATE TABLE generated_rows(id INTEGER PRIMARY KEY, value TEXT NOT NULL)".update()
        val keys = sql"INSERT INTO generated_rows(value) VALUES ('value')".insertReturningGenKeys[Int]()
        assertEquals(keys.toSeq, Seq(1))
      }
    }
  }

  test("SQLite executes batch updates") {
    withContext { ctx =>
      ctx.run {
        sql"CREATE TABLE batch_rows(id INTEGER PRIMARY KEY, value TEXT NOT NULL)".update()

        val counts = Seq(1 -> "one", 2 -> "two")
          .map { (id, value) =>
            sql"INSERT INTO batch_rows(id, value) VALUES ($id, $value)"
          }
          .batchUpdate()

        assertEquals(counts, Seq(1, 1))
        assertEquals(
          sql"SELECT value FROM batch_rows ORDER BY id".readValues[String](),
          Seq("one", "two")
        )
      }
    }
  }

  private def withContext(test: SqueryContext => Unit): Unit =
    val ds = SQLiteDataSource()
    ds.setUrl("jdbc:sqlite::memory:")
    test(SqueryContext(ds))
