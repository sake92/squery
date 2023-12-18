package files.howtos

import utils.*
import Bundle.*, Tags.*

object ReadOneColRows extends HowToPage {

  override def pageSettings =
    super.pageSettings.withTitle("Read One-Column Values")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How To Read One-Column Values?",
    frag(
      s"""
      We use the `readValues[T]()` to read single-column results:
      ```scala
      import ba.sake.squery.*
      
      def customersIds: List[Int] = ctx.run {
        sql"SELECT id FROM customers".readValues[Int]()
      }
      ```

      ---

      There are also variations that return a single result, depending if you want an `Option[T]` or `T` (throws if no row returned):
      ```scala
      sql"SELECT ...".readValueOpt[T]() : Option[T]
      sql"SELECT ...".readValue[T]() : T
      ```
      """.md
    )
  )
}
