package ba.sake.squery.parser

import munit.FunSuite

class SqlSelectAliasParserSuite extends FunSuite {

  test("aliases qualified columns while leaving wildcards and explicit aliases intact") {
    assertEquals(
      SqlSelectAliasParser.addAliases("SELECT u.id, p.id, u.* FROM users u JOIN phones p ON p.user_id = u.id"),
      Some("SELECT u.id AS \"u.id\", p.id AS \"p.id\", u.* FROM users u JOIN phones p ON p.user_id = u.id")
    )
    assertEquals(
      SqlSelectAliasParser.addAliases("SELECT u.id AS user_id, p.id phone_id FROM users u JOIN phones p ON p.user_id = u.id"),
      Some("SELECT u.id AS user_id, p.id phone_id FROM users u JOIN phones p ON p.user_id = u.id")
    )
    assertEquals(
      SqlSelectAliasParser.addAliases("SELECT DISTINCT u.id FROM users u"),
      Some("SELECT DISTINCT u.id AS \"u.id\" FROM users u")
    )
  }

  test("handles functions, strings, quoted identifiers, and nested expressions") {
    assertEquals(
      SqlSelectAliasParser.addAliases("SELECT coalesce(u.name, 'unknown, user'), upper(\"Display Name\") FROM users u"),
      Some("SELECT coalesce(u.name, 'unknown, user') AS \"coalesce(u.name, 'unknown, user')\", upper(\"Display Name\") AS \"upper(\"\"Display Name\"\")\" FROM users u")
    )
  }

  test("handles CTEs, subqueries, comments, dollar quotes, and keyword casing") {
    assertEquals(
      SqlSelectAliasParser.addAliases("WITH active AS (SELECT id FROM users WHERE name = 'from') select a.id, (select max(p.id) from phones p) FROM active a"),
      Some("WITH active AS (SELECT id FROM users WHERE name = 'from') select a.id AS \"a.id\", (select max(p.id) from phones p) AS \"(select max(p.id) from phones p)\" FROM active a")
    )
    assertEquals(
      SqlSelectAliasParser.addAliases("-- report\nSELECT id /*, ignored */, name FROM users"),
      Some("-- report\nSELECT id /*, ignored */ AS \"id\", name AS \"name\" FROM users")
    )
    assertEquals(
      SqlSelectAliasParser.addAliases("SELECT $$from, value, still a string$$, id FROM events"),
      Some("SELECT $$from, value, still a string$$ AS \"$$from, value, still a string$$\", id AS \"id\" FROM events")
    )
  }

  test("supports SELECT without FROM and trailing semicolons") {
    assertEquals(SqlSelectAliasParser.addAliases("SELECT 1, 'x';"), Some("SELECT 1 AS \"1\", 'x' AS \"'x'\";"))
  }

  test("does not rewrite malformed or non-SELECT statements") {
    assertEquals(SqlSelectAliasParser.addAliases("UPDATE users SET name = 'a'"), None)
    assertEquals(SqlSelectAliasParser.addAliases("SELECT 'unterminated FROM users"), None)
  }
}
