package ba.sake.squery.utils

import scala.collection.mutable

extension [T](seq: Seq[T]) {

  /** Group by key, preserving sequence order, *immutable* ~ORM. Usually used for LEFT JOIN results.
    * @param K
    *   key to group by
    * @param extractKey
    *   function that extracts key
    * @param extractValue
    *   function that extracts optional value
    * @return
    *   map of grouped items, with order preserved
    */
  def groupByOrderedOpt[K, V](extractKey: T => K, extractValue: T => Option[V]): Map[K, Seq[V]] =
    val resMap = mutable.LinkedHashMap.empty[K, Seq[V]]
    seq.foreach { row =>
      val key = extractKey(row)
      // insert empty seq if not there
      resMap.getOrElseUpdate(key, Seq.empty)
      extractValue(row).foreach { value =>
        val groupRows = resMap(key)
        resMap(key) = groupRows.appended(value)
      }
    }
    resMap.toMap

  /** Group by key, preserving sequence order, *immutable* ~ORM. Usually used for (INNER/FULL) JOIN results.
    * @param K
    *   key to group by
    * @param extractKey
    *   function that extracts key
    * @param extractValue
    *   function that extracts value
    * @return
    *   map of grouped items, with order preserved
    */
  def groupByOrdered[K, V](extractKey: T => K, extractValue: T => V): Map[K, Seq[V]] =
    groupByOrderedOpt(extractKey, x => Some(extractValue(x)))

  /** Group by key, preserving sequence order, *immutable* ~ORM. Usually used for JOIN results.
    * @param K
    *   key to group by
    * @param extractKey
    *   function that extracts key
    * @return
    *   map of grouped items, with order preserved
    */
  def groupByOrdered[K, V](extractKey: T => K): Map[K, Seq[T]] =
    groupByOrdered(extractKey, identity)
}
