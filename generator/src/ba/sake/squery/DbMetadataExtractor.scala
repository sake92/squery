package ba.sake.squery.generator

import java.sql.{Array => _, *}
import javax.sql.DataSource
import scala.util.*
import scala.util.chaining.*
import scala.collection.mutable.ArrayBuffer
import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.CaseUtils

// https://stackoverflow.com/a/16624964/4496364
class DbMetadataExtractor(ds: DataSource) {

  def extract(): DbMetadata = Using.resource(ds.getConnection()) { connection =>

    val databaseMetaData = connection.getMetaData()
    val dbName = databaseMetaData.getDatabaseProductName().toLowerCase

    val schemaNames = Using.resource(databaseMetaData.getSchemas()) { rs =>
      val buff = ArrayBuffer.empty[String]
      while (rs.next()) {
        buff += rs.getString("TABLE_SCHEM")
      }
      buff.toSeq
    }
    val schemas = schemaNames.map { schemaName =>
      val tables = extractTables(connection, schemaName, databaseMetaData)
      SchemaDef(name = schemaName, tables = tables)
    }
    DbMetadata(
      name = dbName,
      schemas = schemas
    )
  }

  private def extractTables(
      connection: Connection,
      schemaName: String,
      databaseMetaData: DatabaseMetaData
  ): Seq[TableDef] = {
    var readTablesCount = 0
    val columnsMetadata = getColumnsMetadata(connection, schemaName)
    Using.resource(databaseMetaData.getTables(null, schemaName, null, Array("TABLE"))) { resultSet =>
      val res = ArrayBuffer.empty[TableDef]
      while (resultSet.next()) {
        val schema = resultSet.getString("TABLE_SCHEM")
        val tableName = resultSet.getString("TABLE_NAME")
        val columnDefs = generateColumnDefs(databaseMetaData, columnsMetadata, schema, tableName)
        res += TableDef(schema, tableName, columnDefs)
        readTablesCount += 1
      }
      res.toSeq
    }
  }

// TODO getPrimaryKeys
  private def generateColumnDefs(
      databaseMetaData: DatabaseMetaData,
      columnsMetadata: Map[(String, String), ColumnType],
      schemaName: String,
      tableName: String
  ): Seq[ColumnDef] = {

    val res = ArrayBuffer.empty[ColumnDef]
    Using.resource(databaseMetaData.getColumns(null, schemaName, tableName, null)) { resultSet =>
      while resultSet.next() do {
        val columnName = resultSet.getString("COLUMN_NAME")
        val typeName = resultSet.getString("TYPE_NAME")
        val jdbcType = resultSet.getInt("DATA_TYPE") // java.sql.Types
        val isNullable = resultSet.getString("IS_NULLABLE") == "YES"
        val isAutoInc = resultSet.getString("IS_AUTOINCREMENT") == "YES"
        val isGenerated = resultSet.getString("IS_GENERATEDCOLUMN") == "YES"
        val defaultValue = Option(resultSet.getString("COLUMN_DEF"))
        val resolvedType = columnsMetadata((tableName, columnName))
        res += ColumnDef(columnName, resolvedType, isNullable, isAutoInc, isGenerated, defaultValue)
      }
    }
    res.toSeq
  }

  // (table, column) -> ColumnType
  private def getColumnsMetadata(connection: Connection, schemaName: String): Map[(String, String), ColumnType] = {
    val query = s"""
      SELECT  ns.nspname AS schema_name,
              tbl.relname AS table_name,
              att.attname AS column_name,
              pg_catalog.format_type(pt1.oid, NULL) AS display_type_original,
              pg_catalog.format_type(pt2.oid, NULL) AS display_type_resolved,
              att.attndims AS array_dim
      FROM pg_attribute att
      JOIN pg_class tbl ON tbl.oid = att.attrelid
      JOIN pg_namespace ns ON tbl.relnamespace = ns.oid
      JOIN pg_type pt1 ON pt1.oid = att.atttypid
      LEFT JOIN pg_type pt2 ON pt2.oid = pt1.typbasetype
      WHERE att.attnum > 0 -- exclude system columns
            AND ns.nspname = '${schemaName}'
      ORDER BY table_name ASC
    """
    Using.resource(connection.createStatement()) { stmt =>
      Using.resource(stmt.executeQuery(query)) { rs =>
        val buff = ArrayBuffer.empty[((String, String), ColumnType)]
        while rs.next() do {
          val tableName = rs.getString("table_name")
          val columnName = rs.getString("column_name")
          val displayTypeOriginal = rs.getString("display_type_original")
          val displayTypeResolved = rs.getString("display_type_resolved") // for resolving domain types
          val arrayDim = rs.getInt("array_dim")
          val resolvedType = resolveType(connection, displayTypeOriginal, displayTypeResolved, arrayDim)
          buff += (tableName, columnName) -> resolvedType
        }
        buff.toMap
      }
    }
  }

  private def resolveType(
      connection: Connection,
      displayTypeOriginal: String,
      displayTypeResolved: String,
      arrayDim: Int
  ): ColumnType = {
    if arrayDim > 0 then {
      val arrayScalarType = displayTypeOriginal.takeWhile(_ != '[')
      resolveScalarType(arrayScalarType)
        .map { scalarType =>
          val scalarTypeName = scalarType match
            case ColumnType.Predefined(name)     => name
            case ColumnType.Enumeration(name, _) => name
            case ColumnType.Unknown(_)           => "<UNKNOWN>"
          val arrayType = ("Array[" * arrayDim) + scalarTypeName + ("]" * arrayDim)
          // TODO array of enums/domain types?
          ColumnType.Predefined(arrayType)
        }
        .getOrElse(ColumnType.Unknown(displayTypeOriginal))
    } else
      resolveScalarType(displayTypeOriginal)
        .orElse(
          resolveScalarType(displayTypeResolved) // domain type (alias)
        )
        .orElse(resolveEnumType(connection, displayTypeOriginal))
        .getOrElse(ColumnType.Unknown(displayTypeOriginal))

  }

  private def resolveScalarType(tpe: String) = Try {
    tpe match {
      case "boolean"                     => ColumnType.Predefined("Boolean")
      case "integer"                     => ColumnType.Predefined("Int")
      case "smallint"                    => ColumnType.Predefined("Short")
      case "bigint"                      => ColumnType.Predefined("Long")
      case "numeric"                     => ColumnType.Predefined("Double")
      case "text"                        => ColumnType.Predefined("String")
      case "character"                   => ColumnType.Predefined("String")
      case "character varying"           => ColumnType.Predefined("String")
      case "tsvector"                    => ColumnType.Predefined("String")
      case "date"                        => ColumnType.Predefined("LocalDate")
      case "timestamp without time zone" => ColumnType.Predefined("LocalDateTime")
      case "timestamp with time zone"    => ColumnType.Predefined("Instant")
      case "bytea"                       => ColumnType.Predefined("Array[Byte]")
      case other                         => throw RuntimeException(s"Unknown scalar type ${other}")
    }
  }

  def resolveEnumType(connection: Connection, typeName: String): Try[ColumnType.Enumeration] =
    Using(connection.createStatement()) { stmt =>
      val resultSet = stmt.executeQuery(s"select unnest(enum_range(null, null::${typeName}))")
      val enumValues = ArrayBuffer.empty[String]
      while resultSet.next() do {
        enumValues += resultSet.getString(1)
      }
      if enumValues.isEmpty then throw RuntimeException(s"Enum '${typeName}' has no values and cannot be generated.")
      else ColumnType.Enumeration(typeName, enumValues.toSeq)
    }

  // test utils
  private def printAll(resultSet: ResultSet) = {
    val metadata = resultSet.getMetaData()
    val totalCols = metadata.getColumnCount()
    var columnNames = Seq.empty[String]
    for i <- 1 to totalCols do columnNames = columnNames.appended(metadata.getColumnName(i))

    while (resultSet.next()) do
      println("+" * 30)
      for i <- 1 to totalCols do
        val value = resultSet.getString(i)
        print(s"${columnNames(i - 1)} = ${value}; ")
      println()
    end while
  }
}

case class DbMetadata(
    name: String,
    schemas: Seq[SchemaDef]
)

case class SchemaDef(
    name: String,
    tables: Seq[TableDef]
)

case class TableDef(schema: String, name: String, columnDefs: Seq[ColumnDef])

case class ColumnDef(
    name: String,
    scalaType: ColumnType, // scala type
    isNullable: Boolean,
    isAutoInc: Boolean,
    isGenerated: Boolean,
    defaultValue: Option[String]
)

enum ColumnType:
  case Predefined(name: String)
  case Enumeration(name: String, values: Seq[String])
  case Unknown(originalName: String)
