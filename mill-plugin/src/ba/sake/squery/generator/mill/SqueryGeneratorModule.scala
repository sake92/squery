package ba.sake.squery.generator.mill

import mill._
import mill.scalalib._
import _root_.ba.sake.squery.generator._
import org.postgresql.ds.PGSimpleDataSource

trait SqueryGeneratorModule extends JavaModule {

  def squeryServer: T[String] = T("localhost")
  def squeryPort: T[Int]

  def squeryUsername: T[String]
  def squeryPassword: T[String]

  def squeryDatabase: T[String]

  /** List of (schema, basePackage)
    */
  def squerySchemas: T[Seq[(String, String)]]

  def squeryTargetDir: T[os.Path] = T(millSourcePath / "src")

  def squeryGenerate(): Command[Unit] = T.command {
    println("Started generating Squery sources...")

    // TODO parametrize db type
    val ds = new PGSimpleDataSource()
    ds.setUser(squeryUsername())
    ds.setPassword(squeryPassword())
    ds.setDatabaseName(squeryDatabase())
    ds.setServerNames(Array(squeryServer()))
    ds.setPortNumbers(Array(squeryPort()))

    val extractor = new DbMetadataExtractor(ds)
    val dbMeta = extractor.extract()
    val generator = new SqueryGenerator()
    generator.generateFiles(
      dbMeta,
      squerySchemas().map { case (schemaName, basePackage) =>
        SchemaConfig(
          name = schemaName,
          baseFolder = squeryTargetDir(),
          basePackage = basePackage
        )
      }
    )

    println("Finished generating Squery sources...")
  }

}
