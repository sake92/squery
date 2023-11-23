package ba.sake.squery

import ba.sake.squery.write.SqlArgument

/** Implementation of `sql""` interpolator. For a query sql"SELECT .. WHERE $a > 5 AND b = 'abc' ", there have to be `
  * SqlWrite` typeclass instances for types of $a and $b.
  */
extension (sc: StringContext) {

  def sql(args: SqlArgument[?]*): Query =
    val strings = sc.parts.iterator
    var buf = new StringBuilder(strings.next())
    while strings.hasNext do
      buf.append("?")
      buf.append(strings.next())

    Query(buf.toString, args.toSeq)

}
