package public

import ba.sake.squery.SqueryContext

object Globals {
  private val ds = new com.zaxxer.hikari.HikariDataSource()
  ds.setJdbcUrl("jdbc:h2:./h2_pagila")

  val ctx = SqueryContext(ds)
}

