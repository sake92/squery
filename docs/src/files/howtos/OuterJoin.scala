package files.howtos

import utils.*
import Bundle.*, Tags.*

object OuterJoin extends HowToPage {

  override def pageSettings =
    super.pageSettings.withTitle("Outer Join")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How To Read an Outer Join?",
    frag(
      s"""
      The principle is the same as for [FULL JOIN](${FullJoin.ref}).
      
      The only thing you need change is to make the `JOIN`-ed table an `Option[T]`:
      ```scala
      case class CustomerWithPhoneOpt(c: Customer, p: Option[Phone]) derives SqlReadRow

      def customerwithPhoneOpts: List[CustomerWithPhoneOpt] = ctx.run {
        sql"SELECT ...".readRows[CustomerWithPhoneOpt]()
      }
      ```

      The `p: Option[Phone]` will be `None` when all its returned columns are `NULL`.

      """.md
    )
  )
}
