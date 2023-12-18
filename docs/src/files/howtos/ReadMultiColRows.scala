package files.howtos

import utils.*
import Bundle.*, Tags.*

object ReadMultiColRows extends HowToPage {

  override def pageSettings =
    super.pageSettings.withTitle("Read Multi-Column Values")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How To Read Multi-Column Values?",
    frag(
      s"""

      When reading multi-column results, we care about the column names.  
      A natural fit for that are `case class`es.  
      We need to add a `derives SqlReadRow`, and then we can use it:
      ```scala
      import ba.sake.squery.*

      case class Customer(id: Int, name: String) derives SqlReadRow

      def customers: List[Customer] = ctx.run {
        sql"SELECT id, name FROM customers".readRows[Customer]()
      }
      ```
      Note that the `case class`' fields need to match the `SELECT` statement columns!

      ---

      There are also variations that return a single result, depending if you want an `Option[T]` or `T` (throws if no row returned):
      ```scala
      sql"SELECT ...".readRowOpt[T]() : Option[T]
      sql"SELECT ...".readRow[T]() : T
      ```

      """.md
    )
  )
}
