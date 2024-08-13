package files.howtos

import utils.*
import Bundle.*, Tags.*

object Transactions extends HowToPage {

  override def pageSettings =
    super.pageSettings.withTitle("Transactions")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "How To Run Transactions?",
    frag(
      s"""
      We use the `runTransaction` to run queries inside of a transaction:
      ```scala
      ctx.runTransaction {
        sql${Consts.tq}
          INSERT INTO customers(name)
          VALUES (1, 'abc')
        ${Consts.tq}.insert()
        sql${Consts.tq}
          INSERT INTO customers(name)
          VALUES (1, 'def')
        ${Consts.tq}.insert()
      }
      ```
      If one of the queries fails, the transaction will be rolled back, nothing will happen.

      ---
      The `runTransaction` uses the *default JDBC driver* transaction isolation  (depends on db).  
      If you want to explicitly set the transaction isolation you can use the `runTransactionWithIsolation` function:
      ```scala
      ctx.runTransactionWithIsolation(TransactionIsolation.Serializable) {
        // queries here
      }
      ```

      """.md
    )
  )
}
