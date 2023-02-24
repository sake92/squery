import mill._
import mill.scalalib._, scalafmt._

object squery extends SqueryCommonModule {

  def scalaVersion = "3.2.2"

  object test extends Tests with TestModule.Munit with SqueryCommonModule {
    def ivyDeps = Agg(
      ivy"org.scalameta::munit:1.0.0-M7",
      ivy"com.zaxxer:HikariCP:4.0.3",
      ivy"org.xerial:sqlite-jdbc:3.32.3.2"
    )
  }
}

trait SqueryCommonModule extends ScalaModule with ScalafmtModule {

}