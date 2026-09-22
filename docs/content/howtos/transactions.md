---
title: How To Transactions
description: Squery How To Transactions
---

# {{ page.title }}


We use the `runTransaction` to run queries inside of a transaction:
```scala
ctx.runTransaction {
  sql"""
    INSERT INTO customers(name)
    VALUES ('abc')
  """.insert()
  sql"""
    INSERT INTO customers(name)
    VALUES ('def')
  """.insert()
}
```
If the block throws, Squery rolls the transaction back and rethrows the original error.
If rollback also fails, that error is attached as a suppressed exception.

---
The `runTransaction` uses the *default JDBC driver* transaction isolation  (depends on db).  
If you want to explicitly set the transaction isolation you can use the `runTransactionWithIsolation` function:
```scala
ctx.runTransactionWithIsolation(TransactionIsolation.Serializable) {
  // queries here
}
```
