package ba.sake.squery.mariadb

import java.{sql => jsql}
import java.util.UUID
import ba.sake.squery.read.*

given SqlRead[UUID] with {
  def readByName(jRes: jsql.ResultSet, colName: String): Option[UUID] =
    Option(jRes.getString(colName)).map(UUID.fromString)

  def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[UUID] =
    Option(jRes.getString(colIdx)).map(UUID.fromString)
}
