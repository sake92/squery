package ba.sake.squery.generator

import java.sql.{Array => _, _}

import scala.util._
import org.apache.commons.text.CaseUtils
import com.typesafe.scalalogging.Logger
import java.io.File

class SqueryGenerator(config: SqueryGeneratorConfig = SqueryGeneratorConfig.Default) {
  private val logger = Logger(getClass.getName)

  private val Preamble = "/* DO NOT EDIT MANUALLY! Automatically generated by squery generator */"

  def generateString(dbMeta: DbMetadata, schemaNames: Seq[String]): String =
    schemaNames
      .map { schemaName =>
        dbMeta.schemas.find(_.name == schemaName) match {
          case Some(schemaDef) =>
            logger.info(s"Started generating schema '${schemaName}'")
            val (imports, enumDefsScala, tableDefsScala) = generateSchema(schemaDef, dbName = dbMeta.name)
            val res =
              s"""|${Preamble}
                  |${imports}
                  |
                  |${enumDefsScala.mkString("\n")}
                  |
                  |${tableDefsScala.mkString("\n")}
                  |""".stripMargin
            logger.info(s"Finished generating schema '${schemaName}'")
            res
          case None =>
            throw new RuntimeException(s"Schema '${schemaName}' does not exist")
        }
      }
      .mkString("\n")

  def generateFiles(dbMeta: DbMetadata, schemaConfigs: Seq[SchemaConfig]): Unit =
    schemaConfigs.foreach { schemaConfig =>
      dbMeta.schemas.find(_.name == schemaConfig.name) match {
        case Some(schemaDef) =>
          logger.info(s"Started generating schema '${schemaConfig.name}' into '${schemaConfig.baseFolder}'")
          val packagePath = os.RelPath(schemaConfig.basePackage.replaceAll("\\.", "/"))
          val (imports, enumDefsScala, tableDefsScala) = generateSchema(schemaDef, dbName = dbMeta.name)
          enumDefsScala.foreach { enumFile =>
            val enumDefWithImports =
              s"""|${Preamble}
                  |package ${schemaConfig.basePackage}.models
                  |
                  |${imports}
                  |
                  |${enumFile.content}
                  |""".stripMargin
            os.write.over(
              schemaConfig.baseFolder / packagePath / "models" / enumFile.baseName,
              enumDefWithImports,
              createFolders = true
            )
          }
          tableDefsScala.foreach { tableDef =>
            val tableDefWithImports =
              s"""|${Preamble}
                  |package ${schemaConfig.basePackage}.models
                  |
                  |${imports}
                  |
                  |${tableDef.content}
                  |""".stripMargin
            os.write.over(
              schemaConfig.baseFolder / packagePath / "models" / tableDef.baseName,
              tableDefWithImports,
              createFolders = true
            )
          }
          logger.info(s"Finished generating schema '${schemaConfig.name}'")
        case None =>
          throw new RuntimeException(s"Schema '${schemaConfig.name}' does not exist")
      }
    }

  private def generateSchema(
      schemaDef: SchemaDef,
      dbName: String
  ): (String, Seq[GeneratedFile], Seq[GeneratedFile]) = {
    val enumDefs = schemaDef.tables.flatMap {
      _.columnDefs.map(_.scalaType).collect { case e: ColumnType.Enumeration =>
        e
      }
    }
    val enumFiles = enumDefs.map { enumDef =>
      val enumCaseDefs = enumDef.values.map { enumDefCaseValue =>
        s"    case ${enumDefCaseValue.safeIdentifier}"
      }
      val enumName = transformName(enumDef.name, config.typeNameMapper)
      val contents = s"""|enum ${enumName} derives SqlRead, SqlWrite:
          |${enumCaseDefs.mkString("\n")}
          |""".stripMargin
      GeneratedFile(s"${enumName}.scala", contents)
    }

    val tableFiles = schemaDef.tables.map { tableDef =>
      val columnDefsScala = tableDef.columnDefs.map { columnDef =>
        val safeTypeName = getSafeTypeName(columnDef.scalaType, config.typeNameMapper)
        val tpe = if (columnDef.isNullable) s"Option[${safeTypeName}]" else safeTypeName
        s"    ${columnDef.name.safeIdentifier}: ${tpe}"
      }
      val columnNamesScala = tableDef.columnDefs.map { columnDef =>
        s"""  inline val ${columnDef.name.safeIdentifier} = "${columnDef.name.safeIdentifier}""""
      }
      val prefixedColumnNamesScala = tableDef.columnDefs.map { columnDef =>
        s"""prefix + ${columnDef.name.safeIdentifier}"""
      }
      val caseClassName = transformName(tableDef.name, config.typeNameMapper) + config.rowTypeSuffix
      val contents = s"""|case class ${caseClassName}(
          |${columnDefsScala.mkString(",\n")}
          |) derives SqlReadRow
          |
          |object ${caseClassName} {
          |${columnNamesScala.mkString("\n")}
          |
          |  inline val * = prefixed("")
          |
          |  transparent inline def prefixed(inline prefix: String) =
          |    ${prefixedColumnNamesScala.mkString(""" + ", " + """)}
          |}
          |""".stripMargin
      GeneratedFile(s"${caseClassName}.scala", contents)

    }

    val squeryDbPackage =
      if (dbName.contains("postgres")) "postgres"
      else if (dbName.contains("mysql")) "mysql"
      else if (dbName.contains("mariadb")) "mariadb"
      else if (dbName.contains("oracle")) "oracle"
      else if (dbName.contains("h2")) "h2"
      else throw new RuntimeException(s"Unknown database type $dbName")

    val imports =
      s"""|import java.time.*
          |import java.util.UUID
          |import ba.sake.squery.*
          |import ba.sake.squery.${squeryDbPackage}.given
          |import ba.sake.squery.read.SqlRead
          |import ba.sake.squery.write.SqlWrite
          |""".stripMargin

    (imports, enumFiles, tableFiles)
  }

  private def transformName(str: String, nameMapper: NameMapper): String =
    nameMapper match {
      case NameMapper.Noop      => str
      case NameMapper.CamelCase => CaseUtils.toCamelCase(str, true, '_')
    }

  private def getSafeTypeName(tpe: ColumnType, nameMapper: NameMapper): String = tpe match {
    case ColumnType.Predefined(name)              => name
    case ColumnType.Enumeration(enumName, values) => transformName(enumName, nameMapper).safeIdentifier
    case ColumnType.Unknown(originalName)         => s"<UNKNOWN> // ${originalName}"
  }

  implicit class StrOps(str: String) {
    def safeIdentifier: String =
      if (SqueryGenerator.ReservedScalaKeywords(str) || str.contains("-"))
        s"`${str}`"
      else str
  }

}

object SqueryGenerator {
  // https://www.scala-lang.org/files/archive/spec/3.4/01-lexical-syntax.html#regular-keywords
  private val ReservedScalaKeywords = s"""
    abstract  case      catch     class     def       do        else
    enum      export    extends   false     final     finally   for
    given     if        implicit  import    lazy      match     new
    null      object    override  package   private   protected return
    sealed    super     then      throw     trait     true      try
    type      val       var       while     with      yield
    :         =         <-        =>        <:        >:        #
    @         =>>       ?=>
    """.split("\\s+").map(_.trim).toSet
}

case class SchemaConfig(
    name: String,
    baseFolder: os.Path,
    basePackage: String
)

sealed abstract class NameMapper
object NameMapper {
  case object Noop extends NameMapper
  case object CamelCase extends NameMapper
}

case class SqueryGeneratorConfig(
    typeNameMapper: NameMapper,
    rowTypeSuffix: String
)

object SqueryGeneratorConfig {
  val Default: SqueryGeneratorConfig =
    SqueryGeneratorConfig(typeNameMapper = NameMapper.CamelCase, rowTypeSuffix = "Row")
}

case class GeneratedFile(
    baseName: String,
    content: String
)