package ba.sake.squery.read

import java.sql.ResultSet

import scala.deriving.*
import scala.compiletime.*
import ba.sake.squery.SqueryException

/** Reads a row (or just part of it if used in composition) */
trait SqlReadRow[T]:
  def readRow(jRes: ResultSet, prefix: Option[String]): Option[T]

object SqlReadRow:

  def apply[T](using sqlReadRow: SqlReadRow[T]): SqlReadRow[T] = sqlReadRow

  given [T](using srr: SqlReadRow[T]): SqlReadRow[Option[T]] = new {
    def readRow(jRes: ResultSet, prefix: Option[String]): Option[Option[T]] =
      Some(srr.readRow(jRes, prefix))
  }

  inline given derived[T](using m: Mirror.Of[T]): SqlReadRow[T] =
    inline m match
      case s: Mirror.SumOf[T]     => error("Sum types are not supported")
      case p: Mirror.ProductOf[T] => deriveProduct(p)

  private inline def deriveProduct[T](
      p: Mirror.ProductOf[T]
  ): SqlReadRow[T] =
    val labels = constValueTuple[p.MirroredElemLabels].toArray.map(_.toString)
    val reads = getReads[p.MirroredElemTypes].toArray
    new SqlReadRow[T]:
      def readRow(jRes: ResultSet, prefix: Option[String]): Option[T] = {

        val resTuple = labels.zip(reads).map { (label, r) =>
          val colName = prefix.map(_ + ".").getOrElse("") + label
          r match {
            case read: SqlRead[_] =>
              read.readByName(jRes, colName)
            case read: SqlReadRow[_] =>
              read.readRow(jRes, Some(colName))
          }
        }

        // if all columns are NULL -> left join's result -> None
        if resTuple.forall(_.isEmpty) then None
        else
          val tuple = Tuple.fromArray(resTuple.flatten)
          Some(p.fromTuple(tuple.asInstanceOf[p.MirroredElemTypes]))
      }

  // TODO a bit nicer recursive get-all-stuff
  // https://github.com/lampepfl/dotty/blob/3.2.2/tests/pos-special/fatal-warnings/not-looping-implicit.scala#L12

  private inline def getReads[T <: Tuple]: Tuple =
    inline erasedValue[T] match
      case _: EmptyTuple => EmptyTuple
      case _: (t *: ts) =>
        summonFrom {
          case sr: SqlRead[`t`]     => sr
          case srr: SqlReadRow[`t`] => srr
        } *: getReads[ts]
