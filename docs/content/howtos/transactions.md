---
title: How To Transactions
description: Squery How To Transactions
---

# {{ page.title }}


We use the `runTransaction` to run queries inside of a transaction:
```scala
ctx.runTransaction {
  """
    INSERT INTO customers(name)
    VALUES (1, 'abc')
  """.insert()
  sql"""
    INSERT INTO customers(name)
    VALUES (1, 'def')
  """.insert()
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
