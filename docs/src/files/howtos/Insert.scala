package files.howtos

import utils.*
import Bundle.*, Tags.*

object Insert extends HowToPage {

  override def pageSettings =
    super.pageSettings.withTitle("Insert")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How To Insert Data?",
    frag(
      s"""
      The simplest method inserts the rows and returns the number of inserted rows.
      ```scala
      def insertCustomer: Int = ctx.run {
        sql"INSERT INTO customers(name) VALUES('my_customer')".insert()
      }
      ```

      """.md
    )
  )
}
