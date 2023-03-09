import mill._
import mill.scalalib._, publish._, scalafmt._

import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.1.4`
import de.tobiasroeser.mill.vcs.version.VcsVersion

object squery extends SqueryPublishModule {

  def scalaVersion = "3.2.2"

  def ivyDeps = Agg(
    ivy"com.github.jsqlparser:jsqlparser:4.6"
  )

  object test extends Tests with TestModule.Munit with SqueryCommonModule {
    def ivyDeps = Agg(
      ivy"org.scalameta::munit:1.0.0-M7",
      ivy"com.zaxxer:HikariCP:4.0.3",
      ivy"com.h2database:h2:2.1.214"
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