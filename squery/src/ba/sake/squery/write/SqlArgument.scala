package ba.sake.squery.write

case class SqlArgument[T](
    value: T
)(using val sqlWrite: SqlWrite[T])

object SqlArgument {

  given t2sqlArgument[T: SqlWrite]: Conversion[T, SqlArgument[T]] with {
    def apply(value: T): SqlArgument[T] = new SqlArgument(value)
  }
}
