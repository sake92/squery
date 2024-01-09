package files.howtos

import utils.*
import Bundle.*

// TODO how to map flat result to List[stuff] groupByOrdered :)
// TODO transactions

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
  )

  override def pageCategory = Some("How-Tos")

  override def navbar = Some(Navbar.withActiveUrl(Index.ref))
}
