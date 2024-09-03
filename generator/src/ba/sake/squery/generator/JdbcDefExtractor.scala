package ba.sake.squery.generator

import java.sql.{Array => _, _}
import javax.sql.DataSource
import scala.util._
import scala.util.chaining._
import scala.collection.mutable.ArrayBuffer
import scala.meta._
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
        case Types.BIT                     => ColumnType.Predefined(t"Boolean")
        case Types.BOOLEAN                 => ColumnType.Predefined(t"Boolean")
        case Types.TINYINT                 => ColumnType.Predefined(t"Byte")
        case Types.SMALLINT                => ColumnType.Predefined(t"Short")
        case Types.INTEGER                 => ColumnType.Predefined(t"Int")
        case Types.BIGINT                  => ColumnType.Predefined(t"Long")
        case Types.DECIMAL                 => ColumnType.Predefined(t"Double")
        case Types.DOUBLE                  => ColumnType.Predefined(t"Double")
        case Types.NUMERIC                 => ColumnType.Predefined(t"Double")
        case Types.NVARCHAR                => ColumnType.Predefined(t"String")
        case Types.VARCHAR                 => ColumnType.Predefined(t"String")
        case Types.DATE                    => ColumnType.Predefined(t"LocalDate")
        case Types.TIMESTAMP               => ColumnType.Predefined(t"LocalDateTime")
        case Types.TIMESTAMP_WITH_TIMEZONE => ColumnType.Predefined(t"Instant")
        case Types.VARBINARY               => ColumnType.Predefined(t"Array[Byte]")
        case Types.BINARY                  => ColumnType.Predefined(t"Array[Byte]")
        case _                             => ColumnType.Unknown(cMeta.jdbcType.toString)
      }
      (cMeta.table, cMeta.name) -> tpe
    }.toMap
  }

}
