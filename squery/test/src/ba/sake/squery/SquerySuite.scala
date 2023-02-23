package ba.sake.squery

class SquerySuite extends munit.FunSuite {

  test("SELECT simple types") {
    val ds = com.zaxxer.hikari.HikariDataSource()
    ds.setJdbcUrl("jdbc:sqlite::memory:")

    val conn = ds.getConnection()
    List(
      "CREATE TABLE users(id INTEGER, name VARCHAR)",
      "INSERT INTO users VALUES(123, 'myuser')",
      // phones
      "CREATE TABLE phones(id INTEGER, user_id INTEGER, number VARCHAR)",
      "INSERT INTO phones VALUES(1, 123, '061 123 456')",
      "INSERT INTO phones VALUES(2, 123, '062 225 883')"
    ).foreach { stmtString =>
      val stmt = conn.prepareStatement(stmtString)
      stmt.execute()
      stmt.close()
    }
    conn.close()

    run(ds) {
     /* assertEquals(
        read[Int](Query("SELECT id FROM users")),
        List(123)
      )
      assertEquals(
        read[String](Query("SELECT name FROM users")),
        List("myuser")
      )

      val res = read.row[User](Query("SELECT id, name FROM users"))
*/
      val res2 = read.rows[UserWithPhone](Query("""
        SELECT u.id "u.id", u.name "u.name", p.id "p.id", p.number "p.number"
        FROM users u
        JOIN phones p on p.user_id = u.id
      """))
      println(res2.mkString("\n"))
    }

  }

}

case class User(id: Int, name: String)
object User {
  given SqlReadRow[User] = new {
    def readRow(jRes: java.sql.ResultSet, prefix: Option[String]): User = {
      User(
        SqlRead[Int].readByName(jRes, prefix.map(_ + ".").getOrElse("") + "id"),
        SqlRead[String].readByName(jRes, prefix.map(_ + ".").getOrElse("") +"name")
      )
    }
  }
}


case class Phone(id: Int, number: String)

object Phone {
  given SqlReadRow[Phone] = new {
    def readRow(jRes: java.sql.ResultSet, prefix: Option[String]): Phone = {
      Phone(
        SqlRead[Int].readByName(jRes, prefix.map(_ + ".").getOrElse("") +"id"),
        SqlRead[String].readByName(jRes, prefix.map(_ + ".").getOrElse("") +"number")
      )
    }
  }
}


case class UserWithPhone(u: User, p: Phone)
object UserWithPhone {
  given SqlReadRow[UserWithPhone] = new {
    def readRow(jRes: java.sql.ResultSet, prefix: Option[String]): UserWithPhone = {
      UserWithPhone(
        SqlReadRow[User].readRow(jRes, Some("u")),
        SqlReadRow[Phone].readRow(jRes, Some("p"))
      )
    }
  }
}