package ba.sake.squery.cli

import ba.sake.squery.generator._
import mainargs.{ParserForMethods, arg, main}

import java.nio.file.Paths
import java.sql.DriverManager
import scala.util.Using

object SqueryMain {

  @main
  def run(
      @arg(doc = "JDBC URL for the database connection")
      jdbcUrl: String,
      @arg(doc = "Schema mappings in the format 'schema1:package1, schema2:package2'. Default is empty")
      schemaMappings: Seq[String] = Seq.empty,
      @arg(doc = "Base folder for generated sources. Default is 'src/main/scala'")
      baseFolder: String = "src/main/scala",
      @arg(doc = "Type name identifier mapping function: 'camelcase' or 'noop'. Default is 'camelcase'")
      typeNameMapper: String = "camelcase",
      @arg(doc = "Row type suffix. Default is 'Row'")
      rowTypeSuffix: String = "Row",
      @arg(doc = "DAO type suffix. Default is 'Dao'")
      daoTypeSuffix: String = "Dao"
  ) = {

    val config = SqueryGeneratorConfig(
      colNameIdentifierMapper = NameMapper.CamelCase,
      typeNameMapper = NameMapper.CamelCase,
      rowTypeSuffix = "Row",
      daoTypeSuffix = "Dao"
    )
    
    Using.resource(DriverManager.getConnection(jdbcUrl)) { connection =>
      val squeryGenerator = new SqueryGenerator(connection, config)
      val schemaConfigs = schemaMappings.map { case s"${schemaName}:${basePackage}" =>
          SchemaConfig(
            name = schemaName,
            baseFolder = Paths.get(baseFolder),
            basePackage = basePackage
          )
        }
      squeryGenerator.generateFiles(schemaConfigs)
    }
  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args)

  // TODO typeclass NameMapper
}
