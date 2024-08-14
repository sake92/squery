import $ivy.`io.chris-kipp::mill-ci-release::0.1.9`
import $ivy.`ba.sake::mill-hepek::0.0.2`

import mill._, mill.scalalib._, publish._, scalafmt._

import io.kipp.mill.ci.release.CiReleaseModule
import ba.sake.millhepek.MillHepekModule

object squery extends CommonScalaModule with SqueryPublishModule {
  def artifactName = "squery"

  def ivyDeps = Agg(
    ivy"com.typesafe.scala-logging::scala-logging:3.9.4",
    ivy"com.github.jsqlparser:jsqlparser:4.7",
    ivy"org.scala-lang.modules::scala-collection-contrib:0.3.0"
  )

  object test extends ScalaTests with TestModule.Munit {
    def ivyDeps = Agg(
      ivy"org.scalameta::munit:1.0.0",
      ivy"ch.qos.logback:logback-classic:1.4.6",
      ivy"com.zaxxer:HikariCP:4.0.3",
      ivy"com.h2database:h2:2.1.214",
      ivy"org.testcontainers:testcontainers:1.17.6",
      ivy"org.testcontainers:postgresql:1.17.6",
      ivy"org.postgresql:postgresql:42.5.4",
      ivy"org.testcontainers:mysql:1.19.3",
      ivy"mysql:mysql-connector-java:8.0.33",
      ivy"org.testcontainers:mariadb:1.19.3",
      ivy"org.mariadb.jdbc:mariadb-java-client:3.3.2",
      ivy"org.testcontainers:oracle-free:1.19.3",
      ivy"com.oracle.database.jdbc:ojdbc8:23.3.0.23.09"
    )
  }
}

object generator extends CommonScalaModule with SqueryPublishModule {
  def artifactName = "squery-generator"

  def moduleDeps = Seq(squery)

  def ivyDeps = Agg(
    ivy"ch.qos.logback:logback-classic:1.4.6",
    ivy"com.lihaoyi::os-lib:0.10.3",
    ivy"org.apache.commons:commons-text:1.12.0"
  )

  object test extends ScalaTests with TestModule.Munit {

    def ivyDeps = Agg(
      ivy"org.scalameta::munit:1.0.0",
      ivy"com.zaxxer:HikariCP:4.0.3",
      ivy"org.postgresql:postgresql:42.5.4",
      ivy"org.testcontainers:testcontainers:1.17.6",
      ivy"org.testcontainers:postgresql:1.17.6"
    )
  }
}

object docs extends CommonScalaModule with MillHepekModule {
  def ivyDeps = Agg(
    ivy"ba.sake::hepek:0.24.1"
  )
}

trait CommonScalaModule extends ScalaModule with ScalafmtModule {
  def scalaVersion = "3.4.0"
}

trait SqueryPublishModule extends CiReleaseModule {

  def pomSettings = PomSettings(
    organization = "ba.sake",
    url = "https://github.com/sake92/squery",
    licenses = Seq(License.Common.Apache2),
    versionControl = VersionControl.github("sake92", "squery"),
    description = "Squery SQL library",
    developers = Seq(
      Developer("sake92", "Sakib Hadžiavdić", "https://sake.ba")
    )
  )
}
