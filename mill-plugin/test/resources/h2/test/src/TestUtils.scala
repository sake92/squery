package public

import ba.sake.squery.SqueryContext

trait TestUtils {
  private val ds = new com.zaxxer.hikari.HikariDataSource()
  ds.setJdbcUrl(sys.env("JDBC_URL"))

  val squeryCtx = SqueryContext(ds)
}

