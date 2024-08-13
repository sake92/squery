package ba.sake.squery.generator

import java.sql.{Array => _, *}

import scala.util.*
import org.apache.commons.text.CaseUtils

class SqueryGenerator(config: SqueryGeneratorConfig = SqueryGeneratorConfig.Default) {

  // https://www.scala-lang.org/files/archive/spec/3.4/01-lexical-syntax.html#regular-keywords
  private final val ReservedScalaKeywords = s"""
    abstract  case      catch     class     def       do        else
    enum      export    extends   false     final     finally   for
    given     if        implicit  import    lazy      match     new
    null      object    override  package   private   protected return
    sealed    super     then      throw     trait     true      try
    type      val       var       while     with      yield
    :         =         <-        =>        <:        >:        #
    @         =>>       ?=>
    """.split("\\s+").map(_.trim).toSet

  def generate(dbMeta: DbMetadata, schema2Package: Map[String, String]): Unit =
    dbMeta.schemas.foreach { schema =>
      schema2Package.get(schema.name) match
        case Some(pkg) => generateSchema(pkg, schema, dbName = dbMeta.name)
        case None      => ()
    }

  private def generateSchema(
      packageName: String,
      schemaDef: SchemaDef,
      dbName: String
  ): Unit = {
    val enumDefs = schemaDef.tables.flatMap {
      _.columnDefs.map(_.scalaType).collect { case e: ColumnType.Enumeration =>
        e
      }
    }
    val enumDefsScala = enumDefs.map { enumDef =>
      val enumCaseDefs = enumDef.values.map { enumDefCaseValue =>
        s"    case ${enumDefCaseValue.safeIdentifier}"
      }
      val enumName = transformName(enumDef.name, config.typeNameMapper)
      s"""|enum ${enumName} derives SqlRead, SqlWrite:
          |${enumCaseDefs.mkString("\n")}
          |""".stripMargin
    }

    val tableDefsScala = schemaDef.tables.map { tableDef =>
      val columnDefsScala = tableDef.columnDefs.map { columnDef =>
        val safeTypeName = getSafeTypeName(columnDef.scalaType, config.typeNameMapper)
        val tpe = if columnDef.isNullable then s"Option[${safeTypeName}]" else safeTypeName
        s"    ${columnDef.name.safeIdentifier}: ${tpe}"
      }
      val columnNamesScala = tableDef.columnDefs.map { columnDef =>
        s"""  inline val ${columnDef.name.safeIdentifier} = "${columnDef.name.safeIdentifier}""""
      }
      val prefixedColumnNamesScala = tableDef.columnDefs.map { columnDef =>
        s"""prefix + ${columnDef.name.safeIdentifier}"""
      }
      val caseClassName = transformName(tableDef.name, config.typeNameMapper) + config.rowTypeSuffix
      s"""|case class ${caseClassName}(
          |${columnDefsScala.mkString(",\n")}
          |) derives SqlReadRow
          |
          |object ${caseClassName} {
          |${columnNamesScala.mkString("\n")}
          |
          |  inline val * = prefixed("")
          |
          |  transparent inline def prefixed(inline prefix: String) =
          |    ${prefixedColumnNamesScala.mkString(""" + "," + """)}
          |}
          |""".stripMargin
    }

    val squeryDbPackage =
      if dbName.contains("postgres") then "postgres"
      else if dbName.contains("mysql") then "mysql"
      else if dbName.contains("mariadb") then "mariadb"
      else if dbName.contains("oracle") then "oracle"
      else if dbName.contains("h2") then "h2"
      else throw new RuntimeException(s"Unknown database type $dbName")

    val finalResult =
      s"""|/* Automatically generated code by squery generator */
          |package ${packageName}.models
          |
          |import java.time.*
          |import java.util.UUID
          |import ba.sake.squery.*
          |import ba.sake.squery.${squeryDbPackage}.given
          |import ba.sake.squery.read.SqlRead
          |import ba.sake.squery.write.SqlWrite
          |
          |${enumDefsScala.mkString("\n")}
          |
          |${tableDefsScala.mkString("\n")}
          |""".stripMargin

    println(finalResult)
  }

  private def transformName(str: String, nameMapper: NameMapper): String =
    nameMapper match
      case NameMapper.Noop      => str
      case NameMapper.CamelCase => CaseUtils.toCamelCase(str, true, '_')

  private def getSafeTypeName(tpe: ColumnType, nameMapper: NameMapper): String = tpe match
    case ColumnType.Predefined(name)              => name
    case ColumnType.Enumeration(enumName, values) => transformName(enumName, nameMapper).safeIdentifier
    case ColumnType.Unknown(originalName)         => s"<UNKNOWN> // ${originalName}"

  extension (str: String) {
    private def safeIdentifier =
      if ReservedScalaKeywords(str) || str.contains("-")
      then s"`${str}`"
      else str
  }

}

enum NameMapper:
  case Noop
  case CamelCase

case class SqueryGeneratorConfig(
    typeNameMapper: NameMapper,
    rowTypeSuffix: String
)

object SqueryGeneratorConfig:
  val Default: SqueryGeneratorConfig =
    SqueryGeneratorConfig(typeNameMapper = NameMapper.CamelCase, rowTypeSuffix = "Row")
