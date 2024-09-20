package ba.sake.squery.generator

import java.sql.{Array => _, _}
import javax.sql.DataSource
import scala.util._
import scala.util.chaining._
import scala.collection.mutable.ArrayBuffer
import scala.meta._
import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.CaseUtils

// https://stackoverflow.com/a/16624964/4496364
class PostgresDefExtractor(ds: DataSource) extends DbDefExtractor(ds) {

  // (table, column) -> ColumnType
  override protected def getColumnTypes(
      connection: Connection,
      schemaName: String,
      columnsMetadatas: Seq[ColumnMetadata]
  ): Map[(String, String), ColumnType] = {
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
        while (rs.next()) {
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
    if (arrayDim > 0) {
      val arrayScalarType = displayTypeOriginal.takeWhile(_ != '[')
      resolveScalarType(arrayScalarType)
        .map { scalarType =>
          // TODO array of enums/domain types?
          if (arrayDim == 1) {
            ColumnType.Predefined(t"Array[${scalarType.tpe}]")
          } else if (arrayDim == 2) {
            ColumnType.Predefined(t"Array[Array[${scalarType.tpe}]]")
          } else {
            throw new RuntimeException(s"Unsupported array dimension: $arrayDim")
          }

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

  //

  private def resolveScalarType(tpe: String): Try[ColumnType.ScalarType] = Try {
    tpe match {
      case "boolean"                     => ColumnType.Predefined(t"Boolean")
      case "integer"                     => ColumnType.Predefined(t"Int")
      case "smallint"                    => ColumnType.Predefined(t"Short")
      case "bigint"                      => ColumnType.Predefined(t"Long")
      case "numeric"                     => ColumnType.Predefined(t"Double")
      case "text"                        => ColumnType.Predefined(t"String")
      case "character"                   => ColumnType.Predefined(t"String")
      case "character varying"           => ColumnType.Predefined(t"String")
      case "tsvector"                    => ColumnType.Predefined(t"String")
      case "date"                        => ColumnType.Predefined(t"LocalDate")
      case "timestamp without time zone" => ColumnType.Predefined(t"LocalDateTime")
      case "timestamp with time zone"    => ColumnType.Predefined(t"Instant")
      case "uuid"                        => ColumnType.Predefined(t"UUID")
      case "bytea"                       => ColumnType.Predefined(t"Array[Byte]")
      case "json" =>
        ColumnType.ThirdParty(
          t"JValue",
          Seq("org.typelevel.jawn.ast.JValue", "ba.sake.squery.postgres.jawn.{*, given}")
        )
      case "jsonb" =>
        ColumnType.ThirdParty(
          t"JValue",
          Seq("org.typelevel.jawn.ast.JValue", "ba.sake.squery.postgres.jawn.{*, given}")
        )
      case other => throw new RuntimeException(s"Unknown scalar type ${other}")
    }
  }

  private def resolveEnumType(connection: Connection, typeName: String): Try[ColumnType.Enumeration] =
    Using(connection.createStatement()) { stmt =>
      val resultSet = stmt.executeQuery(s"select unnest(enum_range(null, null::${typeName}))")
      val enumValues = ArrayBuffer.empty[String]
      while (resultSet.next()) {
        enumValues += resultSet.getString(1)
      }
      if (enumValues.isEmpty) throw new RuntimeException(s"Enum '${typeName}' has no values and cannot be generated.")
      else ColumnType.Enumeration(typeName, enumValues.toSeq)
    }

}
