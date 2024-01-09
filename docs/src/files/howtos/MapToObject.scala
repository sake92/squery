package files.howtos

import utils.*
import Bundle.*, Tags.*

object MapToObject extends HowToPage {

  override def pageSettings =
    super.pageSettings.withTitle("Map Results To Objects")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How To Map Flat Rows To Objects",
    frag(
      s"""
      Suppose you have these data `class`-es:
      ```scala
      // table rows:
      case class Customer(id: Int, name: String) derives SqlReadRow
      case class Address(id: Int, name: String) derives SqlReadRow

      // left join result:
      case class CustomerWithAddressOpt(c: Customer, a: Option[Address]) derives SqlReadRow
      ```

      Now, when you do a `SELECT` you'll get a `Seq[CustomerWithPhoneOpt]`, a "flat" result, named tuple.  
      But, on your REST API you'd like to return a structured object:
      ```scala
      case class CustomerDTO(id: Int, name: String, addresses: Seq[String])
      ```

      Squery has utilities for exactly that.  
      Let's see how `groupByOrderedOpt` works:
      ```scala
      import ba.sake.squery.utils.*

      val groupedByCustomer: Map[Customer, Seq[Address]] = rowsLeftJoin.groupByOrderedOpt(_.c, _.a)
      val dtos = groupedByCustomer.map { case (c, addresses) =>
        CustomerDTO(c.id, c.name, addresses.map(_.name))
      }.toSeq
      ```
      We can see that it returns a `Map[Customer, Seq[Address]]`, just as we wanted.  
      Then we just map over it and populate the DTO object, can't be simpler!

      ---

      This does a few thing for us:
      - keeps the list of results *ordered*, so you don't have to sort it twice (once in DB, and again in memory)
      - extracts the value that we need from the raw row result
      - handles the `None` case
      - handles the starting, empty Seq of results case


      """.md
    )
  )
}
