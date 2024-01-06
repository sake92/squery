package files.howtos

import utils.*
import Bundle.*, Tags.*

object DynamicQueries extends HowToPage {

  override def pageSettings =
    super.pageSettings.withTitle("Dynamic Queries")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How To Do Dynamic Queries?",
    frag(
      s"""
      Of course, in the real world, you will need to compose queries dynamically at runtime.  
      You can use the `++` operator on queries:
      ```scala
      def customers(): Seq[Customer] = ctx.run {
        val conds = List(sql"id = 123", sql"name LIKE 'Bob%'")
        val condsQuery = conds.reduce(_ ++ _)
        val query = sql"SELECT id, name FROM customers $${condsQuery}"
        query.readRows[Customer]()
      }
      ```

      ---

      There are also some utils in the `ba.sake.squery.utils` package.  
      For example, if you want to optionally filter on some columns, you can use `concatenate` function:
      ```scala
      Seq(Option(sql"q1"), None, Option(sql"q2"))
        .concatenate(sep = sql"AND", default = sql"true")

      // same as this:
      sql"q1 AND q2"
      ```

      """.md
    )
  )
}
