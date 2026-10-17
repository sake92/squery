package ba.sake.squery.iron

import java.sql.{PreparedStatement, ResultSet}
import ba.sake.squery.{SqlRead, SqlWrite}
import ba.sake.squery.write.SqlTypeName
import io.github.iltotore.iron.RefinedType

// Iron newtypes have the same representation as their base type.
given [T](using mirror: RefinedType.Mirror[T], base: SqlRead[mirror.BaseType]): SqlRead[T] with
  def readByName(rs: ResultSet, name: String): Option[T] =
    base.readByName(rs, name).map(value => mirror.ops.applyUnsafe(value).asInstanceOf[T])

  def readByIdx(rs: ResultSet, idx: Int): Option[T] =
    base.readByIdx(rs, idx).map(value => mirror.ops.applyUnsafe(value).asInstanceOf[T])

given [T](using mirror: RefinedType.Mirror[T], base: SqlWrite[mirror.BaseType]): SqlWrite[T] with
  def write(ps: PreparedStatement, idx: Int, value: Option[T]): Unit =
    base.write(ps, idx, value.asInstanceOf[Option[mirror.BaseType]])

given [T](using mirror: RefinedType.Mirror[T], base: SqlTypeName[mirror.BaseType]): SqlTypeName[T] with
  def value: String = base.value
