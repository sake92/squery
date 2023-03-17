package ba.sake.squery.read

import java.sql.ResultSet

import scala.deriving.*
import scala.compiletime.*
import ba.sake.squery.SqueryException

/** Reads a row (or just part of it if used in composition) */
trait SqlReadRow[T]:
  def readRow(jRes: ResultSet, prefix: Option[String]): T

object SqlReadRow:

  def apply[T](using sqlReadRow: SqlReadRow[T]): SqlReadRow[T] = sqlReadRow

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
      def readRow(jRes: ResultSet, prefix: Option[String]): T = {

        val resTuple = labels.zip(reads).map { (label, r) =>
          r match {
            case read: SqlRead[_] =>
              val colName = prefix.map(_ + ".").getOrElse("") + label
              read.readByName(jRes, colName).getOrElse {
                throw new SqueryException(
                  s"Column with name '$colName' is null"
                )
              }
            case read: SqlReadRow[_] =>
              read
                .readRow(jRes, Some(prefix.map(_ + ".").getOrElse("") + label))
          }
        }
        // hackery gibberish
        // compiletime ops are a mistery still
        val tuple = Tuple.fromArray(resTuple)
        p.fromTuple(tuple.asInstanceOf[p.MirroredElemTypes])
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
