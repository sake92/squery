package files.howtos

import utils.*
import Bundle.*, Tags.*

object FullJoin extends HowToPage {

  override def pageSettings =
    super.pageSettings.withTitle("Full Join")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How To Read a Full Join?",
    frag(
      s"""
      ${Consts.ProjectName} is using `case class` composition to read `JOIN`ed tables.  
      Let's say we have tables whose rows are represented by these 2 case classes:
      ```scala
      case class Customer(id: Int, name: String) derives SqlReadRow
      case class Phone(id: Int, number: String) derives SqlReadRow
      ```

      Doing a `FULL JOIN` on them would look like this:
      ```sql
      SELECT  c.id, c.name,
              p.id, p.number
      FROM customers c
      JOIN phones p ON p.customer_id = c.id     
      ```

      The result can be expressed as a composition of the 2 `case class`es above:
      ```scala
      case class CustomerWithPhone(c: Customer, p: Phone) derives SqlReadRow
      ```

      Variables like `c: Customer` are expected to have *corresponding column names* in the query: `c.id` and `c.name`.  
      The final query is a composition of `Customer` and `Phone`, so it maps nicely in your head, it is easier to read and manipulate.  

      You could have additional columns like a `COUNT`/`SUM` or whatever you need in `CustomerWithPhone` query result.

      ---

      Full example:
      ```scala
      import ba.sake.squery.{*, given}

      case class Customer(id: Int, name: String) derives SqlReadRow
      case class Phone(id: Int, number: String) derives SqlReadRow

      case class CustomerWithPhone(c: Customer, p: Phone) derives SqlReadRow

      def customerwithPhones: List[CustomerWithPhone] = ctx.run {
        sql${Consts.tq}
          SELECT c.id, c.name,
                p.id, p.number
          FROM customers c
          JOIN phones p ON p.customer_id = c.id
        ${Consts.tq}.readRows[CustomerWithPhone]()
      }
      ```


      """.md
    )
  )
}
