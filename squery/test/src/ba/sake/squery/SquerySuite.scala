package ba.sake.squery

import java.util.UUID
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.testcontainers.containers.PostgreSQLContainer
import ba.sake.squery.DynamicArg

class SquerySuite extends munit.FunSuite {

  test("Interpolate literal/constant in query") {
    inline val columns = "id, name"
    val q = sql"""SELECT ${columns} FROM customers"""

    assertEquals(
      q.sqlString,
      """SELECT id, name FROM customers"""
    )
    assertEquals(q.arguments, Seq())
  }

  test("Interpolate value in query") {
    val p1 = "a_customer"
    val p2 = "a_customer2"
    val q = sql"""SELECT id FROM customers WHERE name IN ($p1, $p2)"""

    assertEquals(
      q.sqlString,
      """SELECT id FROM customers WHERE name IN (?, ?)"""
    )
    assertEquals(q.arguments, Seq(p1, p2).map(DynamicArg.apply))
  }

  test("Interpolate query in query") {
    val likeArg = "%Bob%"
    val queryWhere = sql"WHERE name ILIKE ${likeArg}"

    val limitArg = 10
    val q = sql"""SELECT id FROM customers ${queryWhere} LIMIT ${limitArg}"""

    assertEquals(
      q.sqlString,
      """SELECT id FROM customers WHERE name ILIKE ? LIMIT ?"""
    )
    assertEquals(q.arguments, Seq(DynamicArg(likeArg), DynamicArg(limitArg)))
  }

  test("Query concat ++") {
    val p1 = "a_customer"
    val q1 = sql"""SELECT id FROM customers WHERE name = $p1"""

    val p2 = "a_customer2"
    val q2 = sql"""OR name = ${p2}"""

    val q = q1 ++ q2
    assertEquals(
      q.sqlString,
      """SELECT id FROM customers WHERE name = ? OR name = ?"""
    )
    assertEquals(q.arguments, Seq(p1, p2).map(DynamicArg.apply))
  }

  test("DbAction") {
    val a1: DbAction[Int] = sql"""SELECT id FROM customers""".readValue[Int]()
  }

}
