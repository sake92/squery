package files.reference

import utils.*
import Bundle.*, Tags.*

object Index extends ReferencePage {

  override def pageSettings =
    super.pageSettings.withTitle("Reference")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    s"${Consts.ProjectName} reference",
    div(
      s"""
      ...
      """.md,
      chl.scala(s"""
      println("Hello!")
      """)
    )
  )
}
