package ba.sake.squery

import java.util.UUID
import java.time.Instant

case class Customer(id: Int, name: String, street: Option[String]) derives SqlReadRow:
  def insertTuple = sql"(${name}, ${street})"

case class CustomerBad(id: Int, name: String, street: String) derives SqlReadRow

// customer:phone 1:many
case class Phone(id: Int, numbr: String) derives SqlReadRow:
  def insertTuple(customerId: Int) = sql"(${customerId}, ${numbr})"

case class CustomerWithPhone(c: Customer, p: Phone) derives SqlReadRow
case class CustomerWithPhoneOpt(c: Customer, p: Option[Phone]) derives SqlReadRow

// customer:address many:many
case class Address(id: Int, name: Option[String]) derives SqlReadRow:
  def insertTuple = sql"(${name})"

case class CustomerWithAddressOpt(c: Customer, a: Option[Address]) derives SqlReadRow
