package ba.sake.squery

import java.util.UUID
import java.time.Instant

case class Customer(id: Int, name: String, street: Option[String]) derives SqlReadRow
case class CustomerBad(id: Int, name: String, street: String) derives SqlReadRow

// customer:phone 1:many
case class Phone(id: Int, number: String) derives SqlReadRow

case class CustomerWithPhone(c: Customer, p: Phone) derives SqlReadRow
case class CustomerWithPhoneOpt(c: Customer, p: Option[Phone]) derives SqlReadRow

// customer:address many:many
case class Address(id: Int, name: Option[String]) derives SqlReadRow
case class CustomerWithAddressOpt(c: Customer, a: Option[Address]) derives SqlReadRow

case class Datatypes(
    int: Option[Int],
    long: Option[Long],
    double: Option[Double],
    boolean: Option[Boolean],
    string: Option[String],
    uuid: Option[UUID],
    tstz: Option[Instant],
    clr: Option[Color]
) derives SqlReadRow

enum Color derives SqlRead, SqlWrite:
  case red, green, blue
