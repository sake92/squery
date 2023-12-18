package files.howtos

import utils.*
import Bundle.*, Tags.*

object DynamicQueries extends HowToPage {

  override def pageSettings =
    super.pageSettings.withTitle("Dynamic queries")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How To Do Dynamic queries?",
    frag(
      s"""
      Of course, in the real world, you will need to compose queries dynamically at runtime.  
      A common example on the web is when you have a sorting functionality.  

      ```scala
      enum SortCustomersField:
        case id, name

      def customers(sortBy: SortCustomersField): List[Customer] = ctx.run {
        val query = sql"SELECT id, name FROM customers" ++ sortBy(sortBy)
        query.readRows[Customer]()
      }

      def sortBy(sortBy: SortCustomersField): Query = sortBy match
        case SortCustomersField.id   => sql"ORDER BY id DESC"
        case SortCustomersField.name => sql"ORDER BY name DESC"
      ```

      """.md
    )
  )
}
