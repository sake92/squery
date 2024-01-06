package files.howtos

import utils.*
import Bundle.*, Tags.*

object InterpolateQueries extends HowToPage {

  override def pageSettings =
    super.pageSettings.withTitle("Interpolate Queries")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How To Interpolate Queries?",
    s"""
    You can interpolate queries in other queries, just like values:
    ```scala
    val whereQuery = sql"WHERE .."
    sql${Consts.tq}
      SELECT customer_id, name
      FROM customers
      $${whereQuery}
    ${Consts.tq}.insert()
    ```
    """.md
  )
}
