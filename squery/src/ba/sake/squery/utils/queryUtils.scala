package ba.sake.squery
package utils

import scala.collection.decorators._

extension [T](seq: Seq[Option[Query]]) {
  def concatenate(sep: Query, default: Query): Query =
    val conds = seq.flatten
    if conds.isEmpty then default
    else if conds.length == 1 then conds.head
    else conds.intersperse(sep).reduce(_ ++ _)

}
