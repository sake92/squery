package ba.sake.squery.write

import java.time.*

// used for createArrayOf(sqlTypeName, myArray)
trait SqlTypeName[T]:
  def value: String

// for Array[Array[... T]] the value is the inner-most T name
given [T](using stn: SqlTypeName[T]): SqlTypeName[Array[T]] with {
  def value: String = stn.value
}
// for Seq[Seq[... T]] the value is the inner-most T name
given [T](using stn: SqlTypeName[T]): SqlTypeName[Seq[T]] with {
  def value: String = stn.value
}

given SqlTypeName[String] with {
  def value: String = "VARCHAR"
}

given SqlTypeName[Boolean] with {
  def value: String = "BOOLEAN"
}
given SqlTypeName[Byte] with {
  def value: String = "TINYINT"
}
given SqlTypeName[Short] with {
  def value: String = "SMALLINT"
}
given SqlTypeName[Int] with {
  def value: String = "INTEGER"
}
given SqlTypeName[Long] with {
  def value: String = "BIGINT"
}
given SqlTypeName[Double] with {
  def value: String = "REAL"
}

given SqlTypeName[LocalDate] with {
  def value: String = "DATE"
}
given SqlTypeName[LocalDateTime] with {
  def value: String = "TIMESTAMPT"
}
given SqlTypeName[Instant] with {
  def value: String = "TIMESTAMPTZ"
}

given SqlTypeName[Array[Byte]] with {
  def value: String = "BINARY"
}
