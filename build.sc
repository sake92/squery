import mill._
import mill.scalalib._

object squery extends ScalaModule {

  def scalaVersion = "3.2.2"

  def ivyDeps = Agg(
    ivy"com.github.sake92.magnolia::magnolia::disambiguate-singleton-enums-SNAPSHOT"
  )

  def repositoriesTask() = T.task { super.repositoriesTask() ++ Seq(
    coursier.maven.MavenRepository("https://jitpack.io")
  )}

  object test extends Tests with TestModule.Munit {
    def ivyDeps = Agg(
      ivy"org.scalameta::munit:1.0.0-M7",
      ivy"com.zaxxer:HikariCP:4.0.3",
      ivy"org.xerial:sqlite-jdbc:3.32.3.2"
    )
  }
}

