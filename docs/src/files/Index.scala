package files

import utils.*
import Bundle.*, Tags.*

object Index extends DocStaticPage {

  override def pageSettings = super.pageSettings
    .withTitle(Consts.ProjectName)

  override def navbar = Some(Navbar)

  override def pageContent = Grid.row(
    h1(Consts.ProjectName),
    s"""
    Simple SQL queries in Scala 3.

    No DSLs, no fuss, just plain SQL.

    Jump right into:
    - [Tutorials](${files.tutorials.Index.ref}) to get you started
    - [How-Tos](${files.howtos.Index.ref}) to get answers for some common questions
    - [Reference](${files.reference.Index.ref}) to see detailed information
    - [Philosophy](${files.philosophy.Index.ref}) to get insights into design decisions
    """.md
  )
}
