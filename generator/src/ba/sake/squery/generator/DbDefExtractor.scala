package ba.sake.squery.generator

import java.sql.{Array => _, _}
import javax.sql.DataSource
import scala.util._
import scala.util.chaining._
import scala.collection.mutable.ArrayBuffer
import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.CaseUtils

object DbDefExtractor {
  def apply(
      connection: Connection,
      typeMappingRules: Seq[TypeMappingRule] = Seq.empty,
      tableFilter: TableFilter = TableFilter.All
  ): DbDefExtractor = {
    val databaseMetaData = connection.getMetaData
    val dbName = databaseMetaData.getDatabaseProductName.toLowerCase
    dbName match {
      case "postgresql" => new PostgresDefExtractor(connection, typeMappingRules, tableFilter)
      case "sqlite"     => new SqliteDefExtractor(connection, typeMappingRules, tableFilter)
      case _              => new JdbcDefExtractor(connection, typeMappingRules, tableFilter)
    }
  }
}

case class TableFilter(
    includePatterns: Seq[String] = Seq(".*"),
    excludePatterns: Seq[String] = Seq.empty
) {
  private val compiledIncludes = includePatterns.map(_.r.pattern)
  private val compiledExcludes = excludePatterns.map(_.r.pattern)

  def includes(schema: String, table: String): Boolean = {
    val qualifiedName = s"$schema.$table"
    compiledIncludes.exists(_.matcher(qualifiedName).matches()) &&
    !compiledExcludes.exists(_.matcher(qualifiedName).matches())
  }
}

object TableFilter {
  val All: TableFilter = TableFilter()
}

case class TypeMappingRule(
    columnNameRegex: String,
    declaredTypeRegex: String,
    scalaType: scala.meta.Type,
    requiredImports: Seq[String] = Seq.empty
) {
  def matches(metadata: ColumnMetadata): Boolean =
    columnNameRegex.r.pattern.matcher(metadata.name).matches() &&
      declaredTypeRegex.r.pattern.matcher(metadata.declaredType).matches()
}

abstract class DbDefExtractor(
    connection: Connection,
    typeMappingRules: Seq[TypeMappingRule] = Seq.empty,
    tableFilter: TableFilter = TableFilter.All
) {

  def extract(): DbDef =  { 
    val databaseMetaData = connection.getMetaData
    val dbName = databaseMetaData.getDatabaseProductName.toLowerCase
    val schemaNames = schemaNamesFrom(databaseMetaData)
    val schemaDefs = schemaNames.map { schemaName =>
      val tables = extractTables(schemaName, databaseMetaData)
      SchemaDef(name = schemaName, tables = tables)
    }
    val dbType = DbType.fromDatabaseProductName(dbName)
    DbDef(
      name = dbName,
      tpe = dbType,
      schemas = schemaDefs
    )
  }

  protected def schemaNamesFrom(databaseMetaData: DatabaseMetaData): Seq[String] =
    Using.resource(databaseMetaData.getSchemas()) { rs =>
      val buff = ArrayBuffer.empty[String]
      while (rs.next()) {
        buff += rs.getString("TABLE_SCHEM")
      }
      buff.toSeq
    }

  protected def includeTable(schemaName: String, tableSchema: String, tableName: String): Boolean =
    tableFilter.includes(tableSchema, tableName)
  protected def includeColumn(schemaName: String, columnSchema: String): Boolean = true

  // (table, column) -> ColumnType
  protected def getColumnTypes(
      schemaName: String,
      columnsMetadatas: Seq[ColumnMetadata]
  ): Map[(String, String), ColumnType]

  protected def extractTables(
      schemaName: String,
      databaseMetaData: DatabaseMetaData
  ): Seq[TableDef] = {

    val allColumnsMetadatas = extractColumnMetadatas(databaseMetaData, schemaName)
    val configuredColumnTypes = allColumnsMetadatas.flatMap { metadata =>
      typeMappingRules.collectFirst {
        case rule if rule.matches(metadata) => (metadata.table, metadata.name) -> ColumnType.ThirdParty(rule.scalaType, rule.requiredImports)
      }
    }.toMap
    val inferredColumnTypes = getColumnTypes(
      schemaName,
      allColumnsMetadatas.filterNot(metadata => configuredColumnTypes.contains((metadata.table, metadata.name)))
    )
    val allColumnTypes = inferredColumnTypes ++ configuredColumnTypes
    val allColumnDefs = allColumnsMetadatas.map { cMeta =>
      val resolvedType = allColumnTypes((cMeta.table, cMeta.name))
      ColumnDef(cMeta, resolvedType)
    }

    Using.resource(databaseMetaData.getTables(null, schemaName, null, Array("TABLE"))) { tablesRS =>
      val tableDefsRes = ArrayBuffer.empty[TableDef]
      while (tablesRS.next()) {
        val tableSchema = Option(tablesRS.getString("TABLE_SCHEM")).getOrElse(schemaName)
        val tableName = tablesRS.getString("TABLE_NAME")
        if (includeTable(schemaName, tableSchema, tableName)) {
          val tableColumnDefs = allColumnDefs.filter(c => c.metadata.table == tableName && includeColumn(schemaName, c.metadata.schema))
        val pkColumns = Using.resource(databaseMetaData.getPrimaryKeys(null, schemaName, tableName)) { pksRS =>
          val pkColumnRes = ArrayBuffer.empty[(Short, ColumnDef)]
          while (pksRS.next()) {
            val pkColName = pksRS.getString("COLUMN_NAME")
            val pkColumn = tableColumnDefs
              .find(_.metadata.name == pkColName)
              .getOrElse(throw new RuntimeException(s"PK column not found: ${pkColName}"))
            pkColumnRes += ((pksRS.getShort("KEY_SEQ"), pkColumn))
          }
          pkColumnRes.sortBy(_._1).map(_._2).toSeq
        }
          tableDefsRes += TableDef(schemaName, tableName, tableColumnDefs, pkColumns)
        }
      }
      tableDefsRes.toSeq
    }
  }

  private def extractColumnMetadatas(
      databaseMetaData: DatabaseMetaData,
      schemaName: String
  ): Seq[ColumnMetadata] = {
    val res = ArrayBuffer.empty[ColumnMetadata]
    Using.resource(databaseMetaData.getColumns(null, schemaName, null, null)) { resultSet =>
      while (resultSet.next()) {
        val tableSchema = Option(resultSet.getString("TABLE_SCHEM")).getOrElse(schemaName)
        val tableName = resultSet.getString("TABLE_NAME")
        val columnName = resultSet.getString("COLUMN_NAME")
        val typeName = resultSet.getString("TYPE_NAME")
        val jdbcType = resultSet.getInt("DATA_TYPE") // java.sql.Types
        val isNullable = resultSet.getString("IS_NULLABLE") == "YES"
        val isAutoInc = resultSet.getString("IS_AUTOINCREMENT") == "YES"
        val isGenerated = resultSet.getString("IS_GENERATEDCOLUMN") == "YES"
        val defaultValue = Option(resultSet.getString("COLUMN_DEF"))
        if (includeColumn(schemaName, tableSchema)) res += ColumnMetadata(
          tableSchema,
          tableName,
          columnName,
          typeName,
          jdbcType,
          isNullable,
          isAutoInc,
          isGenerated,
          defaultValue
        )
      }
    }
    res.toSeq
  }

  // test utils
  protected def printAll(resultSet: ResultSet) = {
    val metadata = resultSet.getMetaData()
    val totalCols = metadata.getColumnCount()
    var columnNames = Seq.empty[String]
    for (i <- 1 to totalCols) {
      columnNames = columnNames.appended(metadata.getColumnName(i))
    }

    while (resultSet.next()) {
      println("+" * 30)
      for (i <- 1 to totalCols) {
        val value = resultSet.getString(i)
        print(s"${columnNames(i - 1)} = ${value}; ")
      }
      println()
    }
  }
}

case class DbDef(
    name: String,
    tpe: DbType,
    schemas: Seq[SchemaDef]
)

case class SchemaDef(
    name: String,
    tables: Seq[TableDef]
)

case class TableDef(schema: String, name: String, columnDefs: Seq[ColumnDef], pkColumns: Seq[ColumnDef]) {
  def hasPk: Boolean = pkColumns.nonEmpty
  def hasCompositePk: Boolean = pkColumns.length > 1
  def nonPkColDefs: Seq[ColumnDef] = columnDefs.filterNot(pkColumns.contains)
  def insertableColumnDefs: Seq[ColumnDef] =
    columnDefs.filterNot(column => column.metadata.isAutoInc || column.metadata.isGenerated)
}

case class ColumnDef(
    metadata: ColumnMetadata,
    scalaType: ColumnType
)

sealed abstract class ColumnType {
  def name: String
}
object ColumnType {
  sealed abstract class ScalarType extends ColumnType {
    def tpe: scala.meta.Type
  }

  case class Predefined(tpe: scala.meta.Type) extends ScalarType {
    override def name: String = tpe.toString()
  }
  // e.g. jawn JSON, needs a custom import
  case class ThirdParty(tpe: scala.meta.Type, requiredImports: Seq[String]) extends ScalarType {
    override def name: String = tpe.toString()
  }
  case class Enumeration(name: String, values: Seq[String]) extends ColumnType
  case class Unknown(name: String) extends ColumnType
}

// raw db data
case class ColumnMetadata(
    schema: String,
    table: String,
    name: String,
    declaredType: String,
    jdbcType: Int,
    isNullable: Boolean,
    isAutoInc: Boolean,
    isGenerated: Boolean,
    defaultValue: Option[String]
)
