package files.howtos

import utils.*
import Bundle.*, Tags.*

object InsertRetValues extends HowToPage {

  override def pageSettings =
    super.pageSettings.withTitle("Insert returning inserted values")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How To Insert returning inserted values?",
    frag(
      s"""
      This method inserts the rows and returns columns you want from the inserted rows.  
      This is **not supported by all databases**, unfortunately.
      ```scala
      def insertCustomers: List[Customer] = ctx.run {
        sql${Consts.tq}
          INSERT INTO customers(name)
          VALUES ('abc'), ('def'), ('ghi')
          RETURNING id, name
        ${Consts.tq}.insertReturningRows[Customer]()
      }
      ```
      Here in one query you can both **insert + get** the row you inserted.  


      """.md
    )
  )
}
