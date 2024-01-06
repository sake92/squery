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

  // cannot fail, it will *at least* give you a None
  given [T](using srr: SqlReadRow[T]): SqlReadRow[Option[T]] with {
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

        var allScalar = true
        val resTuple = labels.zip(reads).map { (label, readTypeclass) =>
          val colName = prefix.map(_ + ".").getOrElse("") + label
          readTypeclass match {
            case read: SqlRead[_] =>
              read.readByName(jRes, colName)
            case read: SqlReadRow[_] =>
              allScalar = false
              read.readRow(jRes, Some(colName))
          }
        }

        // ugly but works.. :/
        // issue with returning an Option[Option[T]]
        // we dont know if it was null, or just a dummy Some(None) value returned by squery
        // TODO try to find a more elegant way
        val allColsEmpty = resTuple
          .map {
            case None       => true
            case Some(None) => true
            case _          => false
          }
          .forall(identity)

        val flattened = resTuple.flatten

        // if all SCALAR columns are NULL (e.g. LEFT JOIN result) -> None
        if allScalar then
          if allColsEmpty then None
          else if flattened.size < labels.size then
            throw SqueryException(
              s"Result set for ${p} is missing a mandatory value, maybe some columns are optional but you forgot to use Option[T]?"
            )
          else
            val tuple = Tuple.fromArray(flattened)
            Some(p.fromTuple(tuple.asInstanceOf[p.MirroredElemTypes]))
        else
          val tuple = Tuple.fromArray(flattened)
          Some(p.fromTuple(tuple.asInstanceOf[p.MirroredElemTypes]))
      }

  // a bit nicer recursive get-all-stuff maybe?
  // https://github.com/lampepfl/dotty/blob/3.2.2/tests/pos-special/fatal-warnings/not-looping-implicit.scala#L12

  private inline def getReads[T <: Tuple]: Tuple =
    inline erasedValue[T] match
      case _: EmptyTuple => EmptyTuple
      case _: (t *: ts) =>
        summonFrom {
          case sr: SqlRead[`t`]     => sr
          case srr: SqlReadRow[`t`] => srr
        } *: getReads[ts]
