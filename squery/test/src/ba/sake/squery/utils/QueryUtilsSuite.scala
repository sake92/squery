package ba.sake.squery
package utils

class QueryUtilsSuite extends munit.FunSuite {
  test("Seq[Option[Query]].concat") {
    assertEquals(
      Seq(None, None, None).concatenate(sql"AND", sql"true"),
      sql"true"
    )
    assertEquals(
      Seq(Option(sql"q1")).concatenate(sql"AND", sql"true"),
      sql"q1"
    )
    assertEquals(
      Seq(Option(sql"q1"), Option(sql"q2"), Option(sql"q3")).concatenate(sql"AND", sql"true"),
      sql"q1 AND q2 AND q3"
    )
  }
}
