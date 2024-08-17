package ba.sake.squery.generator

sealed abstract class DbType(val squeryPackage: String) {
  def supportsReturning: Boolean = false
}

object DbType {

  def fromDatabaseProductName(dbName: String): DbType = {
    if (dbName.contains("h2")) H2
    else if (dbName.contains("postgres")) PostgreSQL
    else if (dbName.contains("mysql")) MySQL
    else if (dbName.contains("mariadb")) MariaDB
    else if (dbName.contains("oracle")) Oracle
    else throw new RuntimeException(s"Unknown database type $dbName")
  }

  case object H2 extends DbType("h2")
  case object PostgreSQL extends DbType("postgres") {
    override def supportsReturning: Boolean = true
  }
  case object MySQL extends DbType("mysql")
  case object MariaDB extends DbType("mariadb")
  case object Oracle extends DbType("oracle")
}
