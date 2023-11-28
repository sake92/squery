package ba.sake.squery.utils

import scala.collection.mutable

extension [T](seq: Seq[T]) {

  /** group by id, preserving sequence order, aka "poor-man's ORM"
    * @param ID
    *   id to group by
    * @param A
    *   extracted value, not necessarily the whole row
    * @param extractFun
    *   function that extracts ID and a value
    * @return
    *   map of grouped items, with order preserved
    */
  def groupByOrderedOpt[ID, A](extractFun: T => Option[(ID, A)]): Map[ID, (A, Seq[T])] =
    val resMap = mutable.LinkedHashMap.empty[ID, (A, Seq[T])]
    seq.foreach { item =>
      extractFun(item).foreach { case (id, extracted) =>
        val (_, groupRows) = resMap.getOrElse(id, (extracted, Seq.empty))
        resMap(id) = (extracted, groupRows.appended(item))
      }
    }
    resMap.toMap

    /** group by id, preserving sequence order, aka "poor-man's ORM"
      * @param ID
      *   id to group by
      * @param A
      *   extracted value, not necessarily the whole row
      * @param extractFun
      *   function that extracts ID and a value
      * @return
      *   map of grouped items, with order preserved
      */
  def groupByOrdered[ID, A](extractFun: T => (ID, A)): Map[ID, (A, Seq[T])] =
    groupByOrderedOpt(x => Some(extractFun(x)))

}
