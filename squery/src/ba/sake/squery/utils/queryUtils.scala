package ba.sake.squery
package utils

extension [T](seq: Seq[Option[Query]]) {

  def concatenate(sep: Query, default: Query): Query =
    val conds = seq.flatten
    if conds.isEmpty then default
    else if conds.length == 1 then conds.head
    else conds.tail.foldLeft(conds.head)((result, query) => result ++ sep ++ query)

}
