import mill._
import mill.scalalib._, publish._, scalafmt._

import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.1.4`
import de.tobiasroeser.mill.vcs.version.VcsVersion

object squery extends SqueryPublishModule {

  def scalaVersion = "3.2.2"

  def ivyDeps = Agg(
    ivy"com.typesafe.scala-logging::scala-logging:3.9.4",
    ivy"com.github.jsqlparser:jsqlparser:4.6"
  )

  object test extends Tests with TestModule.Munit with SqueryCommonModule {
    def ivyDeps = Agg(
      // https://mvnrepository.com/artifact/org.slf4j/slf4j-simple
      ivy"org.slf4j:slf4j-simple:2.0.7",
      ivy"org.scalameta::munit:1.0.0-M7",
      ivy"com.zaxxer:HikariCP:4.0.3",
      ivy"org.postgresql:postgresql:42.5.4",
      ivy"org.testcontainers:testcontainers:1.17.6",
      ivy"org.testcontainers:postgresql:1.17.6"
    )
  }
}

trait SqueryCommonModule extends ScalaModule with ScalafmtModule

trait SqueryPublishModule extends SqueryCommonModule with PublishModule {
  def artifactName = "squery"

  override def publishVersion: T[String] = VcsVersion.vcsState().format()

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
