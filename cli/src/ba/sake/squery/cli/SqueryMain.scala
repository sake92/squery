package ba.sake.squery.cli

import ba.sake.squery.generator._
import mainargs.{ParserForMethods, TokensReader, arg, main}

import java.nio.file.Paths
import java.sql.DriverManager
import scala.meta._
import scala.util.Using

object SqueryMain {

  implicit object NewtypesReader extends TokensReader.Simple[Newtypes] {
    def shortName = "none|iron"
    def read(strs: Seq[String]): Either[String, Newtypes] = strs.head match {
      case "none" => Right(Newtypes.None)
      case "iron" => Right(Newtypes.Iron)
      case other  => Left(s"Unknown newtypes mode '$other'. Expected 'none' or 'iron'.")
    }
  }

  @main
  def run(
      @arg(doc = "JDBC URL for the database connection")
      jdbcUrl: String,
      @arg(doc = "Schema mappings in the format 'schema1:package1, schema2:package2'. Default is empty")
      schemaMappings: Seq[String] = Seq.empty,
      @arg(doc = "Base folder for generated sources. Default is 'src/main/scala'")
      baseFolder: String = "src/main/scala",
      @arg(doc = "Column name identifier mapping function: 'camelcase' or 'noop'. Default is 'camelcase'")
      colNameIdentifierMapper: String = "camelcase",
      @arg(doc = "Type name mapping function: 'camelcase' or 'noop'. Default is 'camelcase'")
      typeNameMapper: String = "camelcase",
      @arg(doc = "Row type suffix. Default is 'Row'")
      rowTypeSuffix: String = "Row",
      @arg(doc = "DAO type suffix. Default is 'Dao'")
      daoTypeSuffix: String = "Dao",
      @arg(
        name = "typeMappingRule",
        doc = "Type mapping rule in the format 'column-name-regex|declared-type-regex|Scala-type'. Repeatable."
      )
      typeMappingRule: Seq[String] = Seq.empty,
      @arg(
        name = "includeTables",
        doc = "Table name regex to include. Matches 'schema.table'. Repeatable; defaults to all tables."
      )
      includeTables: Seq[String] = Seq.empty,
      @arg(
        name = "excludeTables",
        doc = "Table name regex to exclude. Matches 'schema.table'. Repeatable and takes precedence over includes."
      )
      excludeTables: Seq[String] = Seq.empty,
      @arg(
        name = "newtypes",
        doc = "Key newtypes: none (default) or iron. Iron requires squery-iron."
      )
      newtypes: Newtypes = Newtypes.None
  ) = {

    val config = SqueryGeneratorConfig(
      colNameIdentifierMapper = NameMapper(colNameIdentifierMapper),
      typeNameMapper = NameMapper(typeNameMapper),
      rowTypeSuffix = rowTypeSuffix,
      daoTypeSuffix = daoTypeSuffix,
      typeMappingRules = typeMappingRule.map(parseTypeMappingRule),
      tableFilter = TableFilter(if (includeTables.isEmpty) Seq(".*") else includeTables, excludeTables),
      newtypes = newtypes
    )

    Using.resource(DriverManager.getConnection(jdbcUrl)) { connection =>
      val schemaConfigs = schemaMappings.flatMap {
        case s"${schemaName}:${basePackage}" =>
          Some(
            SchemaConfig(
              name = schemaName,
              baseFolder = Paths.get(baseFolder),
              basePackage = basePackage
            )
          )
        case other =>
          println(s"Unrecognized schema:package mapping format ${other}")
          None
      }
      val squeryGenerator = new SqueryGenerator(connection, config)
      squeryGenerator.generateFiles(schemaConfigs)
    }
  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args)

  private def parseTypeMappingRule(value: String): TypeMappingRule = {
    value.split("\\|", 3) match {
      case Array(columnNameRegex, declaredTypeRegex, scalaType)
          if columnNameRegex.nonEmpty && declaredTypeRegex.nonEmpty && scalaType.nonEmpty =>
        TypeMappingRule(columnNameRegex, declaredTypeRegex, scalaType.parse[Type].get)
      case _ =>
        throw new IllegalArgumentException(
          s"Invalid type mapping rule '$value'. Expected column-name-regex|declared-type-regex|Scala-type."
        )
    }
  }

}
