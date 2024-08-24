package ba.sake.squery.generator.mill

import mill._
import mill.scalalib._
import upickle.default.{ReadWriter, macroRW}
import _root_.ba.sake.squery.generator._

trait SqueryGeneratorModule extends JavaModule {

  implicit val NoopRW: ReadWriter[NameMapper.Noop.type] = macroRW
  implicit val CamelCaseRW: ReadWriter[NameMapper.CamelCase.type] = macroRW
  implicit val NameMapperRW: ReadWriter[NameMapper] = macroRW

  implicit val SqueryGeneratorConfigRW: ReadWriter[SqueryGeneratorConfig] = macroRW

  def squeryJdbcUrl: T[String]
  def squeryUsername: T[String]
  def squeryPassword: T[String]

  /** List of (schema, basePackage) */
  def squerySchemas: T[Seq[(String, String)]]

  def squeryTargetDir: T[os.Path] = T(millSourcePath / "src")

  def squeryGeneratorConfig: T[SqueryGeneratorConfig] = T(SqueryGeneratorConfig.Default)

  def squeryGenerate(): Command[Unit] = T.command {
    println("Starting to generate Squery sources...")

    val jdbcUrl = squeryJdbcUrl()
    val username = squeryUsername()
    val password = squeryPassword()
    val dataSource: javax.sql.DataSource =
      if (jdbcUrl.startsWith("jdbc:h2:")) {
        val ds = new org.h2.jdbcx.JdbcDataSource()
        ds.setURL(jdbcUrl)
        ds.setUser(username)
        ds.setPassword(password)
        ds
      } else if (jdbcUrl.startsWith("jdbc:postgresql:")) {
        val ds = new org.postgresql.ds.PGSimpleDataSource()
        ds.setURL(jdbcUrl)
        ds.setUser(username)
        ds.setPassword(password)
        ds
      } else if (jdbcUrl.startsWith("jdbc:mysql:")) {
        val ds = new com.mysql.cj.jdbc.MysqlDataSource()
        ds.setURL(jdbcUrl)
        ds.setUser(username)
        ds.setPassword(password)
        ds
      } else if (jdbcUrl.startsWith("jdbc:mariadb:")) {
        val ds = new org.mariadb.jdbc.MariaDbDataSource()
        ds.setUrl(jdbcUrl)
        ds.setUser(username)
        ds.setPassword(password)
        ds
      } else if (jdbcUrl.startsWith("jdbc:oracle:")) {
        val ds = new oracle.jdbc.pool.OracleDataSource()
        ds.setURL(jdbcUrl)
        ds.setUser(username)
        ds.setPassword(password)
        ds
      } else throw new RuntimeException(s"Unsupported database ${jdbcUrl}")

    val generator = new SqueryGenerator(dataSource, squeryGeneratorConfig())
    generator.generateFiles(
      squerySchemas().map { case (schemaName, basePackage) =>
        SchemaConfig(
          name = schemaName,
          baseFolder = squeryTargetDir(),
          basePackage = basePackage
        )
      }
    )

    println("Finished generating Squery sources")
  }

}
