package ba.sake.squery
package read

import java.{sql => jsql}
import java.time.*
import java.util.UUID
import scala.deriving.*
import scala.quoted.*
import scala.util.NotGiven
import scala.reflect.ClassTag

// reads a value from a column
trait SqlRead[T]:
  def readByName(jRes: jsql.ResultSet, colName: String): Option[T]
  def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[T]

object SqlRead {
  def apply[T](using sqlRead: SqlRead[T]): SqlRead[T] = sqlRead

  given SqlRead[String] with {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[String] =
      Option(jRes.getString(colName))
    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[String] =
      Option(jRes.getString(colIdx))
  }

  given SqlRead[Boolean] with {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[Boolean] =
      Option(jRes.getBoolean(colName)).filterNot(_ => jRes.wasNull())
    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[Boolean] =
      Option(jRes.getBoolean(colIdx)).filterNot(_ => jRes.wasNull())
  }

  given SqlRead[Byte] with {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[Byte] =
      Option(jRes.getByte(colName))

    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[Byte] =
      Option(jRes.getByte(colIdx))
  }

  given SqlRead[Short] with {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[Short] =
      Option(jRes.getShort(colName)).filterNot(_ => jRes.wasNull())
    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[Short] =
      Option(jRes.getShort(colIdx)).filterNot(_ => jRes.wasNull())
  }

  given SqlRead[Int] with {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[Int] =
      Option(jRes.getInt(colName)).filterNot(_ => jRes.wasNull())
    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[Int] =
      Option(jRes.getInt(colIdx)).filterNot(_ => jRes.wasNull())
  }

  given SqlRead[Long] with {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[Long] =
      Option(jRes.getLong(colName)).filterNot(_ => jRes.wasNull())
    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[Long] =
      Option(jRes.getLong(colIdx)).filterNot(_ => jRes.wasNull())
  }

  given SqlRead[Double] with {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[Double] =
      Option(jRes.getDouble(colName)).filterNot(_ => jRes.wasNull())
    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[Double] =
      Option(jRes.getDouble(colIdx)).filterNot(_ => jRes.wasNull())
  }

  given SqlRead[Instant] with {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[Instant] =
      Option(jRes.getTimestamp(colName)).map(_.toInstant)

    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[Instant] =
      Option(jRes.getTimestamp(colIdx)).map(_.toInstant)
  }

  given SqlRead[OffsetDateTime] with {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[OffsetDateTime] =
      Option(jRes.getObject(colName, classOf[OffsetDateTime]))

    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[OffsetDateTime] =
      Option(jRes.getObject(colIdx, classOf[OffsetDateTime]))
  }

  given SqlRead[LocalDate] with {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[LocalDate] =
      Option(jRes.getDate(colName)).map(_.toLocalDate())

    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[LocalDate] =
      Option(jRes.getDate(colIdx)).map(_.toLocalDate())
  }

  given SqlRead[LocalDateTime] with {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[LocalDateTime] =
      Option(jRes.getTimestamp(colName)).map(_.toLocalDateTime())

    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[LocalDateTime] =
      Option(jRes.getTimestamp(colIdx)).map(_.toLocalDateTime())
  }

  /* Arrays */
  // - general first, then specific ones, for implicits ordering
  // - _.map(_.asInstanceOf[T]) because of boxing/unboxing...
  given sqlReadArray1[T: SqlRead: ClassTag](using NotGiven[SqlNonScalarType[T]]): SqlRead[Array[T]] with {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[Array[T]] =
      Option(jRes.getArray(colName)).map(_.getArray().asInstanceOf[Array[T]].map(_.asInstanceOf[T]))

    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[Array[T]] =
      Option(jRes.getArray(colIdx)).map(_.getArray().asInstanceOf[Array[T]].map(_.asInstanceOf[T]))
  }

  given sqlReadArray2[T: SqlRead: ClassTag](using NotGiven[SqlNonScalarType[T]]): SqlRead[Array[Array[T]]] with {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[Array[Array[T]]] =
      Option(jRes.getArray(colName)).map(_.getArray().asInstanceOf[Array[Array[T]]].map(_.map(_.asInstanceOf[T])))

    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[Array[Array[T]]] =
      Option(jRes.getArray(colIdx)).map(_.getArray().asInstanceOf[Array[Array[T]]].map(_.map(_.asInstanceOf[T])))
  }

  given sqlReadArray3[T: SqlRead: ClassTag](using NotGiven[SqlNonScalarType[T]]): SqlRead[Array[Array[Array[T]]]] with {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[Array[Array[Array[T]]]] =
      Option(jRes.getArray(colName))
        .map(_.getArray().asInstanceOf[Array[Array[Array[T]]]].map(_.map(_.map(_.asInstanceOf[T]))))

    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[Array[Array[Array[T]]]] =
      Option(jRes.getArray(colIdx))
        .map(_.getArray().asInstanceOf[Array[Array[Array[T]]]].map(_.map(_.map(_.asInstanceOf[T]))))
  }

  given SqlRead[Array[Byte]] with {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[Array[Byte]] =
      Option(jRes.getBytes(colName))

    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[Array[Byte]] =
      Option(jRes.getBytes(colIdx))
  }

  // vector utils, nicer to deal with
  given sqlReadVector1[T: SqlRead: ClassTag](using NotGiven[SqlNonScalarType[T]]): SqlRead[Vector[T]] with {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[Vector[T]] =
      SqlRead[Array[T]].readByName(jRes, colName).map(_.toVector)

    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[Vector[T]] =
      SqlRead[Array[T]].readByIdx(jRes, colIdx).map(_.toVector)
  }

  given sqlReadVector2[T: SqlRead: ClassTag](using NotGiven[SqlNonScalarType[T]]): SqlRead[Vector[Vector[T]]] with {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[Vector[Vector[T]]] =
      SqlRead[Array[Array[T]]].readByName(jRes, colName).map(_.toVector.map(_.toVector))

    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[Vector[Vector[T]]] =
      SqlRead[Array[Array[T]]].readByIdx(jRes, colIdx).map(_.toVector.map(_.toVector))
  }

  given sqlReadVector3[T: SqlRead: ClassTag](using NotGiven[SqlNonScalarType[T]]): SqlRead[Vector[Vector[Vector[T]]]]
  with {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[Vector[Vector[Vector[T]]]] =
      SqlRead[Array[Array[Array[T]]]].readByName(jRes, colName).map(_.toVector.map(_.toVector.map(_.toVector)))

    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[Vector[Vector[Vector[T]]]] =
      SqlRead[Array[Array[Array[T]]]].readByIdx(jRes, colIdx).map(_.toVector.map(_.toVector.map(_.toVector)))
  }

  given SqlRead[Vector[Byte]] with {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[Vector[Byte]] =
      SqlRead[Array[Byte]].readByName(jRes, colName).map(_.toVector)

    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[Vector[Byte]] =
      SqlRead[Array[Byte]].readByIdx(jRes, colIdx).map(_.toVector)
  }

  // this "cannot fail"
  given [T](using sr: SqlRead[T]): SqlRead[Option[T]] with {
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
