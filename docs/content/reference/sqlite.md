---
title: SQLite mappings
description: SQLite generator mappings and JDBC storage codecs
---

# {{ page.title }}

## Generator mappings

The SQLite extractor generates the `main` schema. Explicit type mapping rules
take precedence over these built-in mappings. Declared types are normalized to
uppercase for built-in matching.

| Declared type | Column name | Scala type |
|---|---|---|
| `BOOLEAN` or `BOOL` | Any | `Boolean` |
| Type containing `INT` | Starts with `is_`, `has_`, or `can_` | `Boolean` |
| Type containing `INT` | Other | `Long` |
| `REAL` | Any | `Double` |
| `TEXT` | Ends with `_at` | `Instant` |
| `TEXT` | Ends with `_date` | `LocalDate` |
| `TEXT` | Other | `String` |
| `BLOB` | Any | `Array[Byte]` |
| Other, including `NUMERIC`, `DATE`, `DATETIME`, `TIMESTAMP` | Any | Unknown; needs a mapping rule |

Column-name conventions are case-insensitive. Unknown types are emitted as
`<UNKNOWN>` placeholders requiring an explicit mapping or a source edit.
SQLite's `STRICT` tables do not allow `BOOLEAN` or `BOOL` declarations; use
`INTEGER` with a Boolean name convention or a mapping rule instead.

## Storage codecs

The following codecs are selected by `import ba.sake.squery.sqlite.{*, given}`.
The same representations apply to nullable `Option` values; `None` is SQL `NULL`.

| Scala type | SQLite storage | Example |
|---|---|---|
| `Boolean` | `INTEGER`: writes `1`/`0`; reads any nonzero integer as true | `1` |
| `UUID` | `TEXT` parsed by `UUID.fromString` | `123e4567-e89b-12d3-a456-426614174000` |
| `Instant` | `TEXT` written with `toString`, read with `Instant.parse` | `2024-02-29T12:34:56.123Z` |
| `LocalDate` | `TEXT` written with `toString`, read with `LocalDate.parse` | `2024-02-29` |
| `LocalDateTime` | `TEXT` written with `toString`, read with `LocalDateTime.parse` | `2024-02-29T12:34:56.123` |

These temporal codecs use ISO text. They do not decode Unix epoch integers,
Julian day numbers, or arbitrary JDBC timestamp strings. `LocalDateTime` is not
inferred by a name convention; an explicit rule can select it:

```shell
--typeMappingRule 'appointment_time|(?i:TEXT)|java.time.LocalDateTime'
```

Importing SQLite temporal codecs changes `LocalDate` and `LocalDateTime` writes
from the generic JDBC representation to ISO text. Existing numeric date storage
needs conversion or application-specific codecs before using these imports.

SQLite's `CURRENT_TIMESTAMP` returns UTC text formatted as `YYYY-MM-DD HH:MM:SS`.
It lacks the `T` and zone required by `Instant.parse`. An ISO instant default is:

```sql
created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
```

SQLite's `date(...)` returns `YYYY-MM-DD`, compatible with `LocalDate`.
SQLite's `datetime(...)` returns a space-separated timestamp, incompatible with
the ISO `LocalDateTime` reader. Bind a `LocalDateTime` or use an ISO-producing SQL
expression for that storage contract.

SQLite documents its [Boolean storage](https://www.sqlite.org/datatype3.html#boolean_datatype),
[date/time functions](https://www.sqlite.org/lang_datefunc.html), and
[STRICT table types](https://www.sqlite.org/stricttables.html).
Generated `RETURNING` SQL requires SQLite 3.35 or newer; `STRICT` tables require
3.37 or newer. These requirements apply to the engine bundled with the driver.
