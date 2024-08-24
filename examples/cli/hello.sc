//> using scala "3.3.1"
//> using dep "ba.sake::squery:0.5.1"
//> using dep "com.h2database:h2:2.1.214"
//> using dep "com.lihaoyi::pprint:0.9.0"

import ba.sake.squery.{*, given}
import org.h2.jdbcx.JdbcDataSource

val ds = JdbcDataSource()
ds.setURL("jdbc:h2:mem:")

val ctx = SqueryContext(ds)

ctx.run {

  ///////////////////
  // create tables
  ///////////////////
  sql"""
    CREATE TABLE customers(
      id SERIAL PRIMARY KEY,
      name VARCHAR
    )
  """.update()
  sql"""
    CREATE TABLE phones(
      id SERIAL PRIMARY KEY,
      customer_id INTEGER REFERENCES customers(id),
      number VARCHAR
    )
  """.update()

  // id is just a dummy value here because it is autointcremented..
  val c1 = Customer(1, "my_customer")
  val c2 = Customer(1, "other_customer")

  ///////////////////
  // insert data
  ///////////////////
  val insertedCustomersCount = sql"""
    INSERT INTO customers(name)
    VALUES (${c1.name}), (${c2.name})
  """.insert()
  pprint.pprintln(s"Inserted # of customers: " + insertedCustomersCount)

  val phoneIds = sql"""
    INSERT INTO phones(customer_id, number)
    VALUES (1, '123456'), (1, '222333')
  """.insertReturningGenKeys[Int]()
  pprint.pprintln(s"Inserted phoneIds: " + phoneIds)

  ///////////////////
  // read data
  ///////////////////
  pprint.pprintln(
    sql"SELECT id FROM customers".readValues[Int]()
  )
  pprint.pprintln(
    sql"SELECT id,name FROM customers".readRows[Customer]()
  )
  pprint.pprintln(
    sql"""
      SELECT c.id, c.name,
        p.id, p.number
      FROM customers c
      JOIN phones p ON p.customer_id = c.id
    """.readRows[CustomerWithPhone]()
  )
  pprint.pprintln(
    sql"""
      SELECT c.id, c.name,
        p.id, p.number
      FROM customers c
      LEFT JOIN phones p ON p.customer_id = c.id
    """.readRows[CustomerWithPhoneOpt]()
  )

  ///////////////////
  // dynamic
  ///////////////////
  val dynQuery = sql"SELECT id, name FROM customers ${sortBy(SortCustomersField.name)}"
  pprint.pprintln(
    dynQuery.readRows[Customer]()
  )
}

case class Customer(id: Int, name: String) derives SqlReadRow

case class Phone(id: Int, number: String) derives SqlReadRow

case class CustomerWithPhone(c: Customer, p: Phone) derives SqlReadRow

case class CustomerWithPhoneOpt(c: Customer, p: Option[Phone]) derives SqlReadRow

// "dynamic"
enum SortCustomersField:
  case id, name

def sortBy(sortBy: SortCustomersField): Query = sortBy match
  case SortCustomersField.id   => sql"ORDER BY id DESC"
  case SortCustomersField.name => sql"ORDER BY name DESC"
