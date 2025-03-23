package public

import ba.sake.squery.SqueryContext

object Globals {
  private val ds = new com.zaxxer.hikari.HikariDataSource()
  ds.setJdbcUrl(sys.env("JDBC_URL"))

  val ctx = SqueryContext(ds)
}

