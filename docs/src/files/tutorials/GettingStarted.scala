package files.tutorials

import utils.*
import Bundle.*, Tags.*

object GettingStarted extends TutorialPage {

  override def pageSettings = super.pageSettings
    .withTitle("Getting Started")

  override def blogSettings =
    super.blogSettings.withSections(firstSection, runSection)

  val firstSection = Section(
    "Setting up",
    div(
      s"""
      First, we need to initialize a `SqueryContext` with a standard JDBC `DataSource`.  
      You will probably want to use a connection pool for performance (like HikariCP).

      ```scala
      import ba.sake.squery.{*, given}
      // import one of these if needed:
      // import ba.sake.squery.postgres.{*, given}
      // import ba.sake.squery.mysql.{*, given}
      // import ba.sake.squery.mariadb.{*, given}
      // import ba.sake.squery.oracle.{*, given}
      // import ba.sake.squery.h2.{*, given}
      
      val ds = com.zaxxer.hikari.HikariDataSource()
      ds.setJdbcUrl(..)
      ds.setUsername(..)
      ds.setPassword(..)

      val ctx = SqueryContext(ds)
      ```
      """.md
    )
  )

  val runSection = Section(
    "Running queries",
    div(
      s"""
      Now we can run queries inside the context:
      ```scala
      ctx.run {
        // queries go here!
      }
      ```


      or if you want to run them transactionally:
      ```scala
      ctx.runTransaction() {
        // queries go here!
      }
      ```

      `ctx.run*` functions provide an implicit JDBC connection under the cover,  
      thanks to scala3's context functions! <3

      """.md
    )
  )
}
