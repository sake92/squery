package ba.sake.squery.generator

import java.sql.{Array => _, _}
import scala.meta._

class SqliteDefExtractor(
    connection: Connection,
    rules: Seq[SqliteTypeMappingRule] = Seq.empty
) extends DbDefExtractor(connection) {
  override protected def schemaNamesFrom(databaseMetaData: DatabaseMetaData): Seq[String] = Seq("main")
  override protected def includeTable(schemaName: String, tableSchema: String): Boolean = tableSchema == "main"
  override protected def includeColumn(schemaName: String, columnSchema: String): Boolean = columnSchema == "main"

  override protected def getColumnTypes(
      schemaName: String,
      columnsMetadatas: Seq[ColumnMetadata]
  ): Map[(String, String), ColumnType] =
    columnsMetadatas.map(m => (m.table, m.name) -> resolveType(m)).toMap

  private def resolveType(metadata: ColumnMetadata): ColumnType =
    rules.collectFirst {
      case rule if rule.matches(metadata) => ColumnType.ThirdParty(rule.scalaType, rule.requiredImports)
    }.getOrElse {
      val declaredType = metadata.declaredType.trim.toUpperCase
      val name = metadata.name.toLowerCase
      if (isIntegerAffinity(declaredType) && name.matches("(?:is|has|can)_.+")) ColumnType.Predefined(t"Boolean")
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

case class SqliteTypeMappingRule(
    columnNameRegex: String,
    declaredTypeRegex: String,
    scalaType: scala.meta.Type,
    requiredImports: Seq[String] = Seq.empty
) {
  def matches(metadata: ColumnMetadata): Boolean =
    columnNameRegex.r.pattern.matcher(metadata.name).matches() &&
      declaredTypeRegex.r.pattern.matcher(metadata.declaredType).matches()
}
