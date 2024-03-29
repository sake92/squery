package ba.sake.squery.postgres

import java.{sql => jsql}
import java.util.UUID
import ba.sake.squery.read.*

given SqlRead[UUID] with {
  def readByName(jRes: jsql.ResultSet, colName: String): Option[UUID] =
    Option(jRes.getObject(colName, classOf[UUID]))

  def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[UUID] =
    Option(jRes.getObject(colIdx, classOf[UUID]))
}
