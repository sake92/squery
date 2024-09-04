package ba.sake.squery.postgres.jawn

import java.{sql => jsql}
import org.typelevel.jawn.ast.*
import ba.sake.squery.read.*

given SqlRead[JValue] with {
  def readByName(jRes: jsql.ResultSet, colName: String): Option[JValue] =
    SqlRead[String].readByName(jRes, colName).map(JParser.parseUnsafe)

  def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[JValue] =
    SqlRead[String].readByIdx(jRes, colIdx).map(JParser.parseUnsafe)
}
