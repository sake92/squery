package ba.sake.squery

import java.util.UUID
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.lang.reflect.Proxy
import java.sql.Connection
import javax.sql.DataSource
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

  for (withIsolation <- Seq(false, true); failureDuringCommit <- Seq(false, true))
    test(s"transaction preserves the original failure when rollback fails (isolation=$withIsolation, commit=$failureDuringCommit)") {
      val originalError = RuntimeException("original failure")
      val rollbackError = RuntimeException("rollback failure")
      val ctx = failingRollbackContext(originalError, rollbackError, failureDuringCommit)

      val thrown = intercept[RuntimeException] {
        if withIsolation then
          ctx.runTransactionWithIsolation(TransactionIsolation.Serializable) {
            if !failureDuringCommit then throw originalError
          }
        else
          ctx.runTransaction {
            if !failureDuringCommit then throw originalError
          }
      }

      assert(thrown eq originalError)
      assertEquals(thrown.getSuppressed.toSeq, Seq(rollbackError))
    }

  private def failingRollbackContext(
      originalError: RuntimeException,
      rollbackError: RuntimeException,
      failCommit: Boolean
  ): SqueryContext = {
    val connection = Proxy
      .newProxyInstance(
        getClass.getClassLoader,
        Array(classOf[Connection]),
        (_, method, _) =>
          method.getName match
            case "commit" if failCommit => throw originalError
            case "rollback"             => throw rollbackError
            case _                        => defaultValue(method.getReturnType)
      )
      .asInstanceOf[Connection]
    val dataSource = Proxy
      .newProxyInstance(
        getClass.getClassLoader,
        Array(classOf[DataSource]),
        (_, method, _) =>
          if method.getName == "getConnection" then connection
          else defaultValue(method.getReturnType)
      )
      .asInstanceOf[DataSource]
    SqueryContext(dataSource)
  }

  private def defaultValue(returnType: Class[?]): AnyRef =
    if !returnType.isPrimitive then null
    else if returnType == java.lang.Boolean.TYPE then java.lang.Boolean.FALSE
    else if returnType == java.lang.Integer.TYPE then Int.box(0)
    else if returnType == java.lang.Long.TYPE then Long.box(0L)
    else if returnType == java.lang.Double.TYPE then Double.box(0.0)
    else if returnType == java.lang.Float.TYPE then Float.box(0.0f)
    else if returnType == java.lang.Short.TYPE then Short.box(0.toShort)
    else if returnType == java.lang.Byte.TYPE then Byte.box(0.toByte)
    else if returnType == java.lang.Character.TYPE then Char.box(0.toChar)
    else null

}
