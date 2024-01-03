import $ivy.`io.chris-kipp::mill-ci-release::0.1.9`
import $ivy.`ba.sake::mill-hepek::0.0.2`

import mill._, mill.scalalib._, publish._, scalafmt._

import io.kipp.mill.ci.release.CiReleaseModule
import ba.sake.millhepek.MillHepekModule

object squery extends CommonScalaModule with SqueryPublishModule {

  def ivyDeps = Agg(
    ivy"com.typesafe.scala-logging::scala-logging:3.9.4",
    ivy"com.github.jsqlparser:jsqlparser:4.7",
    ivy"org.scala-lang.modules::scala-collection-contrib:0.3.0"
  )

  object test extends ScalaTests with TestModule.Munit {
    def ivyDeps = Agg(
      ivy"ch.qos.logback:logback-classic:1.4.6",
      ivy"org.scalameta::munit:1.0.0-M7",
      ivy"com.zaxxer:HikariCP:4.0.3",
      ivy"org.postgresql:postgresql:42.5.4",
      ivy"org.testcontainers:testcontainers:1.17.6",
      ivy"org.testcontainers:postgresql:1.17.6"
    )
  }
}

object docs extends CommonScalaModule with MillHepekModule {

  def ivyDeps = Agg(
    ivy"ba.sake::hepek:0.22.0"
  )
}

trait CommonScalaModule extends ScalaModule with ScalafmtModule {
  def scalaVersion = "3.3.1"
}

trait SqueryPublishModule extends CiReleaseModule {

  def artifactName = "squery"

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
