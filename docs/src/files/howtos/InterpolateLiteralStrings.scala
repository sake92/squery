package files.howtos

import utils.*
import Bundle.*, Tags.*

object InterpolateLiteralStrings extends HowToPage {

  override def pageSettings =
    super.pageSettings.withTitle("Interpolate literal strings")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How To Interpolate Literal Strings?",
    s"""
    You can use `inline val` (literal type) string values in your queries:
    ```scala
    inline val columns = "id, name, age"
    sql${Consts.tq}
      SELECT $${columns}
      FROM customers
    ${Consts.tq}.readRows[Customer]()
    ```

    This will **not** make a `?` parameter, it will directly insert the literal string.  
    Same as if you wrote this:
    ```scala
    sql${Consts.tq}
      SELECT id, name, age
      FROM customers
    ${Consts.tq}.readRows[Customer]()
    ```
    """.md
  )
}
