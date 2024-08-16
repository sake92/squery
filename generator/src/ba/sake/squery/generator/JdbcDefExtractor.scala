package ba.sake.squery.generator

import java.sql.{Array => _, _}
import javax.sql.DataSource
import scala.util._
import scala.util.chaining._
import scala.collection.mutable.ArrayBuffer
import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.CaseUtils

/** General data types extractor, based on JDBC metadata
  *
  * @param ds
  */
class JdbcDefExtractor(ds: DataSource) extends DbDefExtractor(ds) {

  // (table, column) -> ColumnType
  override protected def getColumnTypes(
      connection: Connection,
      schemaName: String,
      columnsMetadatas: Seq[ColumnMetadata]
  ): Map[(String, String), ColumnType] = {
    val databaseMetaData = connection.getMetaData()
    columnsMetadatas.map { cMeta =>
      val tpe = cMeta.jdbcType match {
        case Types.BIT                     => ColumnType.Predefined("Boolean")
        case Types.BOOLEAN                 => ColumnType.Predefined("Boolean")
        case Types.TINYINT                 => ColumnType.Predefined("Byte")
        case Types.SMALLINT                => ColumnType.Predefined("Short")
        case Types.INTEGER                 => ColumnType.Predefined("Int")
        case Types.BIGINT                  => ColumnType.Predefined("Long")
        case Types.DECIMAL                 => ColumnType.Predefined("Double")
        case Types.DOUBLE                  => ColumnType.Predefined("Double")
        case Types.NUMERIC                 => ColumnType.Predefined("Double")
        case Types.NVARCHAR                => ColumnType.Predefined("String")
        case Types.VARCHAR                 => ColumnType.Predefined("String")
        case Types.DATE                    => ColumnType.Predefined("LocalDate")
        case Types.TIMESTAMP               => ColumnType.Predefined("LocalDateTime")
        case Types.TIMESTAMP_WITH_TIMEZONE => ColumnType.Predefined("Instant")
        case Types.VARBINARY               => ColumnType.Predefined("Array[Byte]")
        case Types.BINARY                  => ColumnType.Predefined("Array[Byte]")
        case _                             => ColumnType.Unknown(cMeta.jdbcType.toString)
      }
      (cMeta.table, cMeta.name) -> tpe
    }.toMap
  }

}
