package files.howtos

import utils.*
import Bundle.*, Tags.*

object InterpolateValues extends HowToPage {

  override def pageSettings =
    super.pageSettings.withTitle("Interpolate values")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How To Interpolate Values?",
    s"""
    You can use any JDBC-writable value in your queries.  

    ```scala
    val customerId = 123
    val phoneNumber = "123456"
    sql${Consts.tq}
      INSERT INTO phones(customer_id, number)
      VALUES($$customerId, $$phoneNumber)
    ${Consts.tq}.insert()
    ```

    Actually, the type of a interpolated value needs to have a `SqlWrite[T]` typeclass instance implemented.
    """.md
  )
}
