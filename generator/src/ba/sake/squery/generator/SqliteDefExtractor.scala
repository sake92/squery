package ba.sake.squery.generator

import java.sql.{Array => _, _}
import scala.meta._
import scala.util.Using

class SqliteDefExtractor(
    connection: Connection,
    typeMappingRules: Seq[TypeMappingRule] = Seq.empty,
    tableFilter: TableFilter = TableFilter.All
) extends DbDefExtractor(connection, typeMappingRules, tableFilter) {
  override protected def schemaNamesFrom(databaseMetaData: DatabaseMetaData): Seq[String] = Seq("main")
  override protected def includeTable(schemaName: String, tableSchema: String, tableName: String): Boolean =
    tableSchema == "main" && super.includeTable(schemaName, tableSchema, tableName)
  override protected def includeColumn(schemaName: String, columnSchema: String): Boolean = columnSchema == "main"

  // JDBC metadata loses component order for implicit composite REFERENCES parent.
  override protected def extractForeignKeys(
      databaseMetaData: DatabaseMetaData,
      schemaName: String,
      tableName: String
  ): Seq[ForeignKeyColumn] =
    Using.resource(connection.createStatement()) { stmt =>
      val escapedTable = tableName.replace("'", "''")
      Using.resource(stmt.executeQuery(s"PRAGMA main.foreign_key_list('$escapedTable')")) { rs =>
        val columns = scala.collection.mutable.ArrayBuffer.empty[ForeignKeyColumn]
        while (rs.next()) {
          columns += ForeignKeyColumn(
            rs.getString("from"),
            "main",
            rs.getString("table"),
            Option(rs.getString("to")).filter(_.nonEmpty),
            rs.getInt("seq") + 1
          )
        }
        columns.toSeq
      }
    }

  override protected def getColumnTypes(
      schemaName: String,
      columnsMetadatas: Seq[ColumnMetadata]
  ): Map[(String, String), ColumnType] =
    columnsMetadatas.map(m => (m.table, m.name) -> resolveType(m)).toMap

  private def resolveType(metadata: ColumnMetadata): ColumnType = {
    val declaredType = metadata.declaredType.trim.toUpperCase
    val name = metadata.name.toLowerCase
    if (declaredType == "BOOLEAN" || declaredType == "BOOL") ColumnType.Predefined(t"Boolean")
    else if (isIntegerAffinity(declaredType) && name.matches("(?:is|has|can)_.+")) ColumnType.Predefined(t"Boolean")
    else if (isIntegerAffinity(declaredType)) ColumnType.Predefined(t"Long")
    else if (declaredType == "REAL") ColumnType.Predefined(t"Double")
    else if (declaredType == "TEXT") {
      if (name.endsWith("_at")) ColumnType.Predefined(t"Instant")
      else if (name.endsWith("_date")) ColumnType.Predefined(t"LocalDate")
      else ColumnType.Predefined(t"String")
    } else if (declaredType == "BLOB") ColumnType.Predefined(t"Array[Byte]")
    else ColumnType.Unknown(metadata.declaredType)
  }

  private def isIntegerAffinity(declaredType: String): Boolean =
    declaredType.toUpperCase.contains("INT")
}
