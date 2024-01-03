package ba.sake.squery

import java.util.UUID
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.testcontainers.containers.PostgreSQLContainer
import ba.sake.squery.write.SqlArgument

class SquerySuite extends munit.FunSuite {

  test("Query concat") {

    val p1 = "a_customer"
    val q1 = sql"""SELECT id FROM customers WHERE name = $p1"""

    val p2 = "a_customer2"
    val q2 = sql"""OR name = ${p2}"""

    val q = q1 ++ q2
    assertEquals(
      q.sqlString,
      """SELECT id FROM customers WHERE name = ? OR name = ?"""
    )
    assertEquals(q.arguments, Seq(p1, p2).map(SqlArgument(_)))
  }

  test("Query in query") {

    val likeArg = "%Bob%"
    val queryWhere = sql"WHERE name ILIKE ${likeArg}"

    val limitArg = 10
    val q = sql"""SELECT id FROM customers ${queryWhere} LIMIT ${limitArg}"""

    assertEquals(
      q.sqlString,
      """SELECT id FROM customers WHERE name ILIKE ? LIMIT ?"""
    )
    assertEquals(q.arguments, Seq(SqlArgument(likeArg), SqlArgument(limitArg)))
  }

}
