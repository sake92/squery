package files.howtos

import utils.*
import Bundle.*

trait HowToPage extends DocPage {

  override def categoryPosts = List(
    Index,
    ReadOneColRows,
    ReadMultiColRows,
    FullJoin,
    OuterJoin,
    Insert,
    InsertRetGenKeys,
    InsertRetValues,
    Update,
    InterpolateLiteralStrings,
    InterpolateValues,
    InterpolateQueries,
    DynamicQueries,
    Transactions,
    MapToObject
  )

  override def pageCategory = Some("How-Tos")

  override def navbar = Some(Navbar.withActiveUrl(Index.ref))
}
