package files.howtos

import utils.*
import Bundle.*, Tags.*

object Update extends HowToPage {

  override def pageSettings =
    super.pageSettings.withTitle("Update")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How To Do Updates?",
    frag(
      s"""
      You can do arbitrary SQL commands here.  
      The most common one is `UPDATE`-ing some rows:
      ```scala
      // returns number of affected rows
      def updateCustomers: Int = ctx.run {
        sql${Consts.tq}
          UPDATE customers
          SET name = 'whatever'
          WHERE name LIKE 'xyz_%'
        ${Consts.tq}.update()
      }
      ```

      ---

      But of course you can do other commands as well:
      ```scala
      def createTable: Unit = ctx.run {
        sql${Consts.tq}
          CREATE TABLE customers(
            id SERIAL PRIMARY KEY,
            name VARCHAR
          )
        ${Consts.tq}.update()
      }
      ```

      """.md
    )
  )
}
