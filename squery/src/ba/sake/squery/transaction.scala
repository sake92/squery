package ba.sake.squery

import java.sql.Connection

// https://docs.oracle.com/javadb/10.8.3.0/devguide/cdevconcepts15366.html
// https://www.postgresql.org/docs/current/transaction-iso.html
enum TransactionIsolation(val jdbcLevel: Int):
  /** Dirty (not-yet-committed) reads allowed.
    */
  case ReadUncommited extends TransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED)

  /** Default in most dbs */
  case ReadCommited extends TransactionIsolation(Connection.TRANSACTION_READ_COMMITTED)

  /** */
  case RepeatableRead extends TransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ)

  /** Strongest guarantees, "lock everything"
    */
  case Serializable extends TransactionIsolation(Connection.TRANSACTION_SERIALIZABLE)
