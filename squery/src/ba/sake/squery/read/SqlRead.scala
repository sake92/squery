package ba.sake.squery
package read

import java.{sql => jsql}
import java.time.*
import java.util.UUID
import scala.deriving.*
import scala.quoted.*

// reads a value from a column
trait SqlRead[T]:
  def readByName(jRes: jsql.ResultSet, colName: String): Option[T]
  def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[T]

object SqlRead {
  def apply[T](using sqlRead: SqlRead[T]): SqlRead[T] = sqlRead

  given SqlRead[String] = new {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[String] =
      Option(jRes.getString(colName))
    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[String] =
      Option(jRes.getString(colIdx))
  }

  given SqlRead[Boolean] = new {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[Boolean] =
      Option(jRes.getBoolean(colName)).filterNot(_ => jRes.wasNull())
    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[Boolean] =
      Option(jRes.getBoolean(colIdx)).filterNot(_ => jRes.wasNull())
  }

  given SqlRead[Int] = new {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[Int] =
      Option(jRes.getInt(colName)).filterNot(_ => jRes.wasNull())
    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[Int] =
      Option(jRes.getInt(colIdx)).filterNot(_ => jRes.wasNull())
  }

  given SqlRead[Long] = new {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[Long] =
      Option(jRes.getLong(colName)).filterNot(_ => jRes.wasNull())
    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[Long] =
      Option(jRes.getLong(colIdx)).filterNot(_ => jRes.wasNull())
  }

  given SqlRead[Double] = new {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[Double] =
      Option(jRes.getDouble(colName)).filterNot(_ => jRes.wasNull())
    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[Double] =
      Option(jRes.getDouble(colIdx)).filterNot(_ => jRes.wasNull())
  }

  given SqlRead[Instant] = new {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[Instant] =
      Option(jRes.getTimestamp(colName)).map(_.toInstant)

    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[Instant] =
      Option(jRes.getTimestamp(colIdx)).map(_.toInstant)
  }

  given SqlRead[OffsetDateTime] = new {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[OffsetDateTime] =
      Option(jRes.getObject(colName, classOf[OffsetDateTime]))

    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[OffsetDateTime] =
      Option(jRes.getObject(colIdx, classOf[OffsetDateTime]))
  }

  given SqlRead[LocalDate] = new {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[LocalDate] =
      Option(jRes.getDate(colName)).map(_.toLocalDate())

    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[LocalDate] =
      Option(jRes.getDate(colIdx)).map(_.toLocalDate())
  }

  given SqlRead[LocalDateTime] = new {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[LocalDateTime] =
      Option(jRes.getTimestamp(colName)).map(_.toLocalDateTime())

    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[LocalDateTime] =
      Option(jRes.getTimestamp(colIdx)).map(_.toLocalDateTime())
  }

  given SqlRead[UUID] = new {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[UUID] =
      Option(jRes.getObject(colName, classOf[UUID]))

    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[UUID] =
      Option(jRes.getObject(colIdx, classOf[UUID]))
  }

  // this "cannot fail"
  given [T](using sr: SqlRead[T]): SqlRead[Option[T]] = new {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[Option[T]] =
      Some(sr.readByName(jRes, colName))

    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[Option[T]] =
      Some(sr.readByIdx(jRes, colIdx))
  }

  /* macro derived instances */
  inline def derived[T]: SqlRead[T] = ${ derivedMacro[T] }

  private def derivedMacro[T: Type](using Quotes): Expr[SqlRead[T]] = {
    import quotes.reflect.*

    val mirror: Expr[Mirror.Of[T]] = Expr.summon[Mirror.Of[T]].getOrElse {
      report.errorAndAbort(
        s"Cannot derive SqlRead[${Type.show[T]}] automatically because ${Type.show[T]} is not a singleton enum"
      )
    }

    mirror match
      case '{ $m: Mirror.ProductOf[T] } =>
        report.errorAndAbort("Product types are not supported")

      case '{
            type label <: Tuple;
            $m: Mirror.SumOf[T] { type MirroredElemLabels = `label` }
          } =>
        val labels = Expr(Type.valueOfTuple[label].map(_.toList.map(_.toString)).getOrElse(List.empty))

        val isSingleCasesEnum = isSingletonCasesEnum[T]
        if !isSingleCasesEnum then
          report.errorAndAbort(
            s"Cannot derive SqlRead[${Type.show[T]}] automatically because ${Type.show[T]} is not a singleton-cases enum"
          )

        val companion = TypeRepr.of[T].typeSymbol.companionModule.termRef
        val valueOfSelect = Select.unique(Ident(companion), "valueOf").symbol

        '{
          new SqlRead[T] {
            override def readByName(jRes: jsql.ResultSet, colName: String): Option[T] =
              SqlRead[String].readByName(jRes, colName).map { enumString =>
                try {
                  ${
                    val bla = '{ enumString }
                    Block(Nil, Apply(Select(Ident(companion), valueOfSelect), List(bla.asTerm))).asExprOf[T]
                  }
                } catch {
                  case e: IllegalArgumentException =>
                    throw SqueryException(
                      s"Enum value not found: '${enumString}'. Possible values: ${$labels.map(l => s"'$l'").mkString(", ")}"
                    )
                }
              }

            override def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[T] =
              SqlRead[String].readByIdx(jRes, colIdx).map { enumString =>
                try {
                  ${
                    val bla = '{ enumString }
                    Block(Nil, Apply(Select(Ident(companion), valueOfSelect), List(bla.asTerm))).asExprOf[T]
                  }
                } catch {
                  case e: IllegalArgumentException =>
                    throw SqueryException(
                      s"Enum value not found: '${enumString}'. Possible values: ${$labels.map(l => s"'$l'").mkString(", ")}"
                    )
                }
              }
          }
        }

      case hmm => report.errorAndAbort("Sum types are not supported")
  }

  private def isSingletonCasesEnum[T: Type](using Quotes): Boolean =
    import quotes.reflect.*
    val ts = TypeRepr.of[T].typeSymbol
    ts.flags.is(Flags.Enum) && ts.companionClass.methodMember("values").nonEmpty

}
