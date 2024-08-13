package ba.sake.squery.generator

import ba.sake.squery.*
import ba.sake.squery.given

@main def run(): Unit =
  val ds = com.zaxxer.hikari.HikariDataSource()
  ds.setJdbcUrl("jdbc:postgresql://localhost:9091/pagila")
  ds.setUsername("flowrun")
  ds.setPassword("flowrun")

  val extractor = DbMetadataExtractor(ds)
  val dbMeta = extractor.extract()
  //pprint.pprintln(dbMeta)
  val generator = SqueryGenerator()
//  generator.generate(dbMeta, Map("public" -> "pagila"))

  import pagila.models.ActorRow
  val ctx = SqueryContext(ds)
  ctx.run {
    val actors = sql"SELECT ${ActorRow.*} FROM public.actor".readRows[ActorRow]()
    pprint.pprintln(actors)
  }
