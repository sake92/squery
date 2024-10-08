import $ivy.`io.chris-kipp::mill-ci-release::0.1.10`
import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest::0.7.1`
import $ivy.`ba.sake::mill-hepek::0.0.2`

import mill._, mill.scalalib._
import mill.scalalib.scalafmt._
import mill.scalalib.publish._
import mill.scalalib.api.ZincWorkerUtil.scalaNativeBinaryVersion
import de.tobiasroeser.mill.integrationtest._

import io.kipp.mill.ci.release.CiReleaseModule
import ba.sake.millhepek.MillHepekModule

val scala213 = "2.13.14"
val scala3 = "3.4.2"
val pomSettingsValue = PomSettings(
  organization = "ba.sake",
  url = "https://github.com/sake92/squery",
  licenses = Seq(License.Common.Apache2),
  versionControl = VersionControl.github("sake92", "squery"),
  description = "Squery SQL library",
  developers = Seq(
    Developer("sake92", "Sakib Hadžiavdić", "https://sake.ba")
  )
)
val millVersion = "0.11.12"

object squery extends ScalaModule with ScalafmtModule with CiReleaseModule {
  def artifactName = "squery"
  def pomSettings = pomSettingsValue
  def scalaVersion = scala3
  def ivyDeps = Agg(
    ivy"com.typesafe.scala-logging::scala-logging:3.9.4",
    ivy"com.github.jsqlparser:jsqlparser:4.7",
    ivy"org.scala-lang.modules::scala-collection-contrib:0.3.0"
  )
  def scalacOptions = Seq("-Ywarn-unused", "-deprecation", "-feature")

  object `postgres-jawn` extends ScalaModule with ScalafmtModule with CiReleaseModule {
    def artifactName = "squery-postgres-jawn"
    def pomSettings = pomSettingsValue
    def scalaVersion = scala3
    def moduleDeps = Seq(squery)
    def ivyDeps = Agg(
      ivy"org.typelevel::jawn-ast::1.6.0"
    )
  }

  object test extends ScalaTests with TestModule.Munit {
    def ivyDeps = Agg(
      ivy"org.scalameta::munit:1.0.0",
      ivy"ch.qos.logback:logback-classic:1.5.7",
      ivy"com.zaxxer:HikariCP:4.0.3",
      ivy"com.h2database:h2:2.3.232",
      ivy"org.testcontainers:testcontainers:1.17.6",
      ivy"org.testcontainers:postgresql:1.17.6",
      ivy"org.postgresql:postgresql:42.7.4",
      ivy"org.testcontainers:mysql:1.19.3",
      ivy"mysql:mysql-connector-java:8.0.33",
      ivy"org.testcontainers:mariadb:1.19.3",
      ivy"org.mariadb.jdbc:mariadb-java-client:3.3.2",
      ivy"org.testcontainers:oracle-free:1.19.3",
      ivy"com.oracle.database.jdbc:ojdbc8:23.3.0.23.09"
    )
  }
}

object generator extends ScalaModule with CiReleaseModule with ScalafmtModule {
  def artifactName = "squery-generator"
  def pomSettings = pomSettingsValue
  def scalaVersion = scala213
  def ivyDeps = Agg(
    ivy"ba.sake::regenesca:0.1.0",
    ivy"com.typesafe.scala-logging::scala-logging:3.9.4",
    ivy"ch.qos.logback:logback-classic:1.5.7",
    ivy"org.apache.commons:commons-text:1.12.0"
  )
}

/* MILL PLUGIN */

object `mill-plugin` extends ScalaModule with CiReleaseModule with ScalafmtModule {
  def artifactName =
    s"mill-squery-generator_mill${millBinaryVersion(millVersion)}"
  def millBinaryVersion(millVersion: String) =
    scalaNativeBinaryVersion(millVersion)
  def pomSettings = pomSettingsValue
  def scalaVersion = scala213
  def compileIvyDeps = super.compileIvyDeps() ++ Agg(
    ivy"com.lihaoyi::mill-scalalib:${millVersion}"
  )
  def moduleDeps = Seq(generator)
  def ivyDeps = Agg(
    ivy"com.h2database:h2:2.3.232",
    ivy"org.postgresql:postgresql:42.7.4",
    ivy"mysql:mysql-connector-java:8.0.33",
    ivy"org.mariadb.jdbc:mariadb-java-client:3.3.2",
    ivy"com.oracle.database.jdbc:ojdbc8:23.3.0.23.09"
  )
  def scalacOptions = Seq("-Ywarn-unused", "-deprecation")
}

object `mill-plugin-itest` extends MillIntegrationTestModule {
  def millTestVersion = millVersion
  def pluginsUnderTest = Seq(`mill-plugin`)
  def temporaryIvyModules = Seq(squery)
  def testBase = millSourcePath / "src"
  def testInvocations: T[Seq[(PathRef, Seq[TestInvocation.Targets])]] = T {
    Seq(
      PathRef(testBase / "h2") -> Seq(TestInvocation.Targets(Seq("verify"), noServer = true))
    )
  }
  override def perTestResources = T.sources {
    os.write(
      T.dest / "versions.sc",
      s"""object Versions {
        val squery = "${squery.publishVersion()}"
      }"""
    )
    Seq(PathRef(T.dest))
  }
}

/* DOCS */
object docs extends ScalaModule with MillHepekModule with ScalafmtModule {
  def scalaVersion = scala3
  def ivyDeps = Agg(
    ivy"ba.sake::hepek:0.24.1"
  )
}
