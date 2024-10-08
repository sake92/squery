package ba.sake.squery
package write

import java.{sql => jsql}
import java.time.Instant
import java.sql.Timestamp
import java.util.UUID
import java.sql.JDBCType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.OffsetDateTime
import scala.deriving.*
import scala.quoted.*
import scala.reflect.ClassTag
import scala.util.NotGiven

trait SqlWrite[T]:
  def write(ps: jsql.PreparedStatement, idx: Int, valueOpt: Option[T]): Unit

object SqlWrite {

  def apply[T](using sqlWrite: SqlWrite[T]): SqlWrite[T] = sqlWrite

  given SqlWrite[String] with {
    def write(
        ps: jsql.PreparedStatement,
        idx: Int,
        valueOpt: Option[String]
    ): Unit = valueOpt match
      case Some(value) => ps.setString(idx, value)
      case None        => ps.setString(idx, null)
  }

  given SqlWrite[Boolean] with {
    def write(
        ps: jsql.PreparedStatement,
        idx: Int,
        valueOpt: Option[Boolean]
    ): Unit = valueOpt match
      case Some(value) => ps.setBoolean(idx, value)
      case None        => ps.setNull(idx, jsql.Types.BOOLEAN)
  }

  given SqlWrite[Byte] with {
    def write(
        ps: jsql.PreparedStatement,
        idx: Int,
        valueOpt: Option[Byte]
    ): Unit = valueOpt match
      case Some(value) => ps.setByte(idx, value)
      case None        => ps.setNull(idx, jsql.Types.TINYINT)
  }

  given SqlWrite[Short] with {
    def write(
        ps: jsql.PreparedStatement,
        idx: Int,
        valueOpt: Option[Short]
    ): Unit = valueOpt match
      case Some(value) => ps.setShort(idx, value)
      case None        => ps.setNull(idx, jsql.Types.SMALLINT)
  }

  given SqlWrite[Int] with {
    def write(
        ps: jsql.PreparedStatement,
        idx: Int,
        valueOpt: Option[Int]
    ): Unit = valueOpt match
      case Some(value) => ps.setInt(idx, value)
      case None        => ps.setNull(idx, jsql.Types.INTEGER)
  }

  given SqlWrite[Long] with {
    def write(
        ps: jsql.PreparedStatement,
        idx: Int,
        valueOpt: Option[Long]
    ): Unit = valueOpt match
      case Some(value) => ps.setLong(idx, value)
      case None        => ps.setNull(idx, jsql.Types.BIGINT)
  }

  given SqlWrite[Double] with {
    def write(
        ps: jsql.PreparedStatement,
        idx: Int,
        valueOpt: Option[Double]
    ): Unit = valueOpt match
      case Some(value) => ps.setDouble(idx, value)
      case None        => ps.setNull(idx, jsql.Types.DOUBLE)
  }

  given SqlWrite[Instant] with {
    def write(
        ps: jsql.PreparedStatement,
        idx: Int,
        valueOpt: Option[Instant]
    ): Unit = valueOpt match
      case Some(value) => ps.setTimestamp(idx, Timestamp.from(value))
      case None        => ps.setNull(idx, jsql.Types.TIMESTAMP_WITH_TIMEZONE)
  }

  given SqlWrite[OffsetDateTime] with {
    def write(
        ps: jsql.PreparedStatement,
        idx: Int,
        valueOpt: Option[OffsetDateTime]
    ): Unit = valueOpt match
      case Some(value) => ps.setObject(idx, value)
      case None        => ps.setNull(idx, jsql.Types.TIMESTAMP_WITH_TIMEZONE)
  }

  given SqlWrite[LocalDate] with {
    def write(
        ps: jsql.PreparedStatement,
        idx: Int,
        valueOpt: Option[LocalDate]
    ): Unit = valueOpt match
      case Some(value) => ps.setDate(idx, java.sql.Date.valueOf(value))
      case None        => ps.setNull(idx, jsql.Types.DATE)
  }

  given SqlWrite[LocalDateTime] with {
    def write(
        ps: jsql.PreparedStatement,
        idx: Int,
        valueOpt: Option[LocalDateTime]
    ): Unit = valueOpt match
      case Some(value) => ps.setObject(idx, value)
      case None        => ps.setNull(idx, jsql.Types.TIMESTAMP)
  }

  /* Arrays */
  given sqlWriteArray1[T: SqlWrite](using stn: SqlTypeName[T], ng: NotGiven[SqlNonScalarType[T]]): SqlWrite[Array[T]]
  with {
    def write(
        ps: jsql.PreparedStatement,
        idx: Int,
        valueOpt: Option[Array[T]]
    ): Unit = valueOpt match
      case Some(value) =>
        val valuesAsAnyRef = value.map(_.asInstanceOf[AnyRef]) // box primitives like Array[Int]
        val sqlArray = ps.getConnection().createArrayOf(stn.value, valuesAsAnyRef)
        ps.setArray(idx, sqlArray)
      case None => ps.setArray(idx, null)
  }
  given sqlWriteArray2[T: SqlWrite](using
      stn: SqlTypeName[T],
      ng: NotGiven[SqlNonScalarType[T]]
  ): SqlWrite[Array[Array[T]]] with {
    def write(
        ps: jsql.PreparedStatement,
        idx: Int,
        valueOpt: Option[Array[Array[T]]]
    ): Unit = valueOpt match
      case Some(value) =>
        val valuesAsAnyRef = value.map(_.map(_.asInstanceOf[AnyRef])) // box primitives like Array[Array[Int]]
        val sqlArray = ps.getConnection().createArrayOf(stn.value, valuesAsAnyRef.asInstanceOf[Array[AnyRef]])
        ps.setArray(idx, sqlArray)
      case None => ps.setArray(idx, null)
  }
  given sqlWriteArray3[T: SqlWrite](using
      stn: SqlTypeName[T],
      ng: NotGiven[SqlNonScalarType[T]]
  ): SqlWrite[Array[Array[Array[T]]]] with {
    def write(
        ps: jsql.PreparedStatement,
        idx: Int,
        valueOpt: Option[Array[Array[Array[T]]]]
    ): Unit = valueOpt match
      case Some(value) =>
        val valuesAsAnyRef =
          value.map(_.map(_.map(_.asInstanceOf[AnyRef]))) // box primitives like Array[Array[Array[Int]]]
        val sqlArray = ps.getConnection().createArrayOf(stn.value, valuesAsAnyRef.asInstanceOf[Array[AnyRef]])
        ps.setArray(idx, sqlArray)
      case None => ps.setArray(idx, null)
  }
  given SqlWrite[Array[Byte]] with {
    def write(
        ps: jsql.PreparedStatement,
        idx: Int,
        valueOpt: Option[Array[Byte]]
    ): Unit = valueOpt match
      case Some(value) => ps.setBytes(idx, value)
      case None        => ps.setNull(idx, jsql.Types.BINARY)
  }

  given sqlWriteVector1[T: SqlWrite: ClassTag](using
      stn: SqlTypeName[T],
      ng: NotGiven[SqlNonScalarType[T]]
  ): SqlWrite[Vector[T]] with {
    def write(
        ps: jsql.PreparedStatement,
        idx: Int,
        valueOpt: Option[Vector[T]]
    ): Unit = SqlWrite[Array[T]].write(ps, idx, valueOpt.map(_.toArray))
  }

  given sqlWriteVector2[T: SqlWrite: ClassTag](using
      stn: SqlTypeName[T],
      ng: NotGiven[SqlNonScalarType[T]]
  ): SqlWrite[Vector[Vector[T]]] with {
    def write(
        ps: jsql.PreparedStatement,
        idx: Int,
        valueOpt: Option[Vector[Vector[T]]]
    ): Unit = SqlWrite[Array[Array[T]]].write(ps, idx, valueOpt.map(_.toArray.map(_.toArray)))
  }

  given sqlWriteVector3[T: SqlWrite: ClassTag](using
      stn: SqlTypeName[T],
      ng: NotGiven[SqlNonScalarType[T]]
  ): SqlWrite[Vector[Vector[Vector[T]]]] with {
    def write(
        ps: jsql.PreparedStatement,
        idx: Int,
        valueOpt: Option[Vector[Vector[Vector[T]]]]
    ): Unit = SqlWrite[Array[Array[Array[T]]]].write(ps, idx, valueOpt.map(_.toArray.map(_.toArray.map(_.toArray))))
  }

  given SqlWrite[Vector[Byte]] with {
    def write(
        ps: jsql.PreparedStatement,
        idx: Int,
        valueOpt: Option[Vector[Byte]]
    ): Unit = SqlWrite[Array[Byte]].write(ps, idx, valueOpt.map(_.toArray))
  }

  given [T](using sw: SqlWrite[T]): SqlWrite[Option[T]] with {
    def write(
        ps: jsql.PreparedStatement,
        idx: Int,
        value: Option[Option[T]]
    ): Unit =
      sw.write(ps, idx, value.flatten)
  }

  /* macro derived instances */
  inline def derived[T]: SqlWrite[T] = ${ derivedMacro[T] }

  private def derivedMacro[T: Type](using Quotes): Expr[SqlWrite[T]] = {
    import quotes.reflect.*

    val mirror: Expr[Mirror.Of[T]] = Expr.summon[Mirror.Of[T]].getOrElse {
      report.errorAndAbort(
        s"Cannot derive SqlWrite[${Type.show[T]}] automatically because ${Type.show[T]} is not a singleton enum"
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
            s"Cannot derive SqlWrite[${Type.show[T]}] automatically because ${Type.show[T]} is not a singleton-cases enum"
          )

        val companion = TypeRepr.of[T].typeSymbol.companionModule.termRef
        val valueOfSelect = Select.unique(Ident(companion), "valueOf").symbol

        '{
          new SqlWrite[T] {
            def write(ps: jsql.PreparedStatement, idx: Int, valueOpt: Option[T]): Unit =
              valueOpt match
                case Some(value) =>
                  val index = $m.ordinal(value)
                  val label = $labels(index)
                  ps.setString(idx, label)
                case None => ps.setString(idx, null)
          }
        }

      case hmm => report.errorAndAbort("Sum types are not supported")
  }

  private def isSingletonCasesEnum[T: Type](using Quotes): Boolean =
    import quotes.reflect.*
    val ts = TypeRepr.of[T].typeSymbol
    ts.flags.is(Flags.Enum) && ts.companionClass.methodMember("values").nonEmpty

}
