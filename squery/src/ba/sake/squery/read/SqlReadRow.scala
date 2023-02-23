package ba.sake.squery.read

import java.{sql => jsql}
import scala.util.Using

// TODO typeclassss
// reads a row (or just part of it if used in composition)
trait SqlReadRow[T]:
  def readRow(jRes: jsql.ResultSet, prefix: Option[String]): T

object SqlReadRow:
  def apply[T](using sqlReadRow: SqlReadRow[T]): SqlReadRow[T] = sqlReadRow

