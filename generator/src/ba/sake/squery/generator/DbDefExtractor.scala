package ba.sake.squery.generator

import java.sql.{Array => _, _}
import javax.sql.DataSource
import scala.util._
import scala.util.chaining._
import scala.collection.mutable.ArrayBuffer
import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.CaseUtils

object DbDefExtractor {
  def apply(ds: DataSource): DbDefExtractor =
    Using.resource(ds.getConnection()) { connection =>
      val databaseMetaData = connection.getMetaData()
      val dbName = databaseMetaData.getDatabaseProductName().toLowerCase
      dbName match {
        case "postgresql" => new PostgresDefExtractor(ds)
        case _            => new JdbcDefExtractor(ds)
      }
    }
}

abstract class DbDefExtractor(ds: DataSource) {

  def extract(): DbDef = Using.resource(ds.getConnection()) { connection =>
    val databaseMetaData = connection.getMetaData()
    val dbName = databaseMetaData.getDatabaseProductName().toLowerCase
    val schemaNames = Using.resource(databaseMetaData.getSchemas()) { rs =>
      val buff = ArrayBuffer.empty[String]
      while (rs.next()) {
        buff += rs.getString("TABLE_SCHEM")
      }
      buff.toSeq
    }
    val schemaDefs = schemaNames.map { schemaName =>
      val tables = extractTables(connection, schemaName, databaseMetaData)
      SchemaDef(name = schemaName, tables = tables)
    }
    DbDef(
      name = dbName,
      schemas = schemaDefs
    )
  }

  // (table, column) -> ColumnType
  protected def getColumnTypes(
      connection: Connection,
      schemaName: String,
      columnsMetadatas: Seq[ColumnMetadata]
  ): Map[(String, String), ColumnType]

  private def extractTables(
      connection: Connection,
      schemaName: String,
      databaseMetaData: DatabaseMetaData
  ): Seq[TableDef] = {

    val allColumnsMetadatas = extractColumnMetadatas(databaseMetaData, schemaName)
    val allColumnTypes = getColumnTypes(connection, schemaName, allColumnsMetadatas)
    val allColumnDefs = allColumnsMetadatas.map { cMeta =>
      val resolvedType = allColumnTypes((cMeta.table, cMeta.name))
      ColumnDef(cMeta, resolvedType)
    }

    Using.resource(databaseMetaData.getTables(null, schemaName, null, Array("TABLE"))) { tablesRS =>
      val tableDefsRes = ArrayBuffer.empty[TableDef]
      while (tablesRS.next()) {
        val tableName = tablesRS.getString("TABLE_NAME")
        val tableColumnDefs = allColumnDefs.filter(_.metadata.table == tableName)
        val pkColumns = Using.resource(databaseMetaData.getPrimaryKeys(null, schemaName, tableName)) { pksRS =>
          val pkColumnRes = ArrayBuffer.empty[ColumnDef]
          while (pksRS.next()) {
            val pkColName = pksRS.getString("COLUMN_NAME")
            pkColumnRes += tableColumnDefs
              .find(_.metadata.name == pkColName)
              .getOrElse(throw new RuntimeException(s"PK column not found: ${pkColName}"))
          }
          pkColumnRes.toSeq
        }
        tableDefsRes += TableDef(schemaName, tableName, tableColumnDefs, pkColumns)
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
        val tableName = resultSet.getString("TABLE_NAME")
        val columnName = resultSet.getString("COLUMN_NAME")
        val typeName = resultSet.getString("TYPE_NAME")
        val jdbcType = resultSet.getInt("DATA_TYPE") // java.sql.Types
        val isNullable = resultSet.getString("IS_NULLABLE") == "YES"
        val isAutoInc = resultSet.getString("IS_AUTOINCREMENT") == "YES"
        val isGenerated = resultSet.getString("IS_GENERATEDCOLUMN") == "YES"
        val defaultValue = Option(resultSet.getString("COLUMN_DEF"))
        res += ColumnMetadata(
          schemaName,
          tableName,
          columnName,
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
}

case class ColumnDef(
    metadata: ColumnMetadata,
    scalaType: ColumnType
)

sealed abstract class ColumnType {
  def name: String
}
object ColumnType {
  case class Predefined(name: String) extends ColumnType
  case class Enumeration(name: String, values: Seq[String]) extends ColumnType
  case class Unknown(name: String) extends ColumnType
}

// raw db data
case class ColumnMetadata(
    schema: String,
    table: String,
    name: String,
    jdbcType: Int,
    isNullable: Boolean,
    isAutoInc: Boolean,
    isGenerated: Boolean,
    defaultValue: Option[String]
)
