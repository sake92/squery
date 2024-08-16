import $ivy.`org.scalameta::munit:0.7.29`
import $ivy.`com.lihaoyi::mill-contrib-flyway:`
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.4.0`
import $file.plugins

import mill._, scalalib._
import de.tobiasroeser.mill.vcs.version.VcsVersion
import contrib.flyway.FlywayModule
import munit.Assertions._
import ba.sake.squery.generator.mill._

// https://github.com/maxandersen/sakila-h2
object root extends ScalaModule with SqueryGeneratorModule with FlywayModule {
  def scalaVersion = "3.4.2"

  def ivyDeps = Agg(
    ivy"com.zaxxer:HikariCP:4.0.3",
    ivy"com.h2database:h2:2.3.232",
    ivy"ba.sake::squery:0.3.0-8-a20b4a-DIRTY8921b651-SNAPSHOT"
  )

  def flywayDriverDeps = Agg(ivy"com.h2database:h2:2.3.232")
  def flywayUrl = "jdbc:h2:./h2_pagila"

  def squeryJdbcUrl = "jdbc:h2:./h2_pagila"
  def squeryUsername = ""
  def squeryPassword = ""
  def squerySchemas = Seq("PUBLIC" -> "public")

  object test extends ScalaTests with TestModule.Munit {
    def ivyDeps = Agg(
      ivy"org.scalameta::munit:0.7.29"
    )
  }
}

def verify(): Command[Unit] = T.command {
  root.flywayMigrate()()

  root.squeryGenerate()()

  val generatedModels = os.walk(root.squeryTargetDir() / "public" / "models").filter(os.isFile)
  assertEquals(generatedModels.size, 17) // 16 + flyway table..
  assert(generatedModels.map(_.last).contains("ActorRow.scala"), "ActorRow was not generated")

  val generatedDaos = os.walk(root.squeryTargetDir() / "public" / "daos").filter(os.isFile)
  assertEquals(generatedDaos.size, 17)
  assert(generatedDaos.map(_.last).contains("ActorCrudDao.scala"), "ActorCrudDao was not generated")

  root.test.test()()
  ()
}
