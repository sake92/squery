package ba.sake.squery

import scala.jdk.CollectionConverters.*

import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.util.AddAliasesVisitor
import net.sf.jsqlparser.statement.select.Select
import net.sf.jsqlparser.statement.select.SelectItemVisitor
import net.sf.jsqlparser.statement.select.SelectVisitor
import net.sf.jsqlparser.statement.select.AllColumns
import net.sf.jsqlparser.statement.select.SetOperationList
import net.sf.jsqlparser.statement.select.AllTableColumns
import net.sf.jsqlparser.statement.select.PlainSelect
import net.sf.jsqlparser.statement.select.WithItem
import net.sf.jsqlparser.expression.Alias
import net.sf.jsqlparser.parser.SimpleNode
import net.sf.jsqlparser.statement.select.SelectItem
import net.sf.jsqlparser.statement.select.ParenthesedSelect
import net.sf.jsqlparser.statement.select.Values
import net.sf.jsqlparser.statement.select.LateralSubSelect

/*
Dbs usually strip away the prefixes, for example: `SELECT u.id, p.id FROM users u JOIN phones p ..` would return 2 columns called "id".
This makes it impossible to map the result in any meaningful way to our case classes...
Thus, we inject the aliases for each column, so that SELECT from above would actually run `SELECT u.id AS "u.id", p.id AS "p.id"`,
so we are back in the game!
 */

// adapted from AddAliasesVisitor
// https://github.com/JSQLParser/JSqlParser/blob/388b7c3afff4f500d23880e4e1c491637eb0bfb3/src/main/java/net/sf/jsqlparser/util/AddAliasesVisitor.java
private class SqueryAddAliasesVisitor extends SelectVisitor, SelectItemVisitor {

  private var firstRun = false
  private var usedAliases = Set.empty[String]

  override def visit(parenthesedSelect: ParenthesedSelect): Unit =
    parenthesedSelect.getSelect().accept(this)

  override def visit(plainSelect: PlainSelect): Unit = {
    firstRun = true
    plainSelect.getSelectItems().asScala.foreach(_.accept(this))

    firstRun = false
    plainSelect.getSelectItems().asScala.foreach(_.accept(this))
  }

  override def visit(setOpList: SetOperationList): Unit =
    for (select <- setOpList.getSelects().asScala) {
      select.accept(this)
    }

  override def visit(selectExpressionItem: SelectItem[?]): Unit =
    if selectExpressionItem.getExpression.isInstanceOf[AllColumns] then return
    if firstRun then
      if selectExpressionItem.getAlias() != null then usedAliases += selectExpressionItem.getAlias().getName()
    else if selectExpressionItem.getAlias() == null then
      val alias = s""" "$selectExpressionItem" """.trim
      if usedAliases.contains(alias) then throw SqueryException(s"Alias '$alias' is already used in this query.")
      usedAliases += alias
      selectExpressionItem.setAlias(Alias(alias))

  override def visit(withItem: WithItem): Unit = {}

  override def visit(values: Values): Unit = {}

  override def visit(lateralSubSelect: LateralSubSelect): Unit = {}

}
