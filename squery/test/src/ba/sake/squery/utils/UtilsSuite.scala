package ba.sake.squery
package utils

class UtilsSuite extends munit.FunSuite {

  val customer1 = Customer(1, "1_customer", None)
  val customer2 = Customer(2, "2_customer", Some("str2"))
  val customer3 = Customer(3, "3_customer", Some("str3"))
  val customer4 = Customer(4, "4_customer", Some("str4"))

  // phones, 1:1, FULL JOIN
  val phone1 = Phone(1, "061 123 456")
  val phone2 = Phone(1, "062 225 883")
  val rowsFullJoin = Seq(
    CustomerWithPhone(customer1, phone1),
    CustomerWithPhone(customer1, phone2),
    CustomerWithPhone(customer2, phone2)
  )

  // addresses, 1:N, LEFT JOIN
  val address1 = Address(1, Some("a1"))
  val address2 = Address(2, Some("a2"))
  val rowsLeftJoin = Seq(
    CustomerWithAddressOpt(customer1, Some(address1)),
    CustomerWithAddressOpt(customer1, Some(address2)),
    CustomerWithAddressOpt(customer2, Some(address1)),
    CustomerWithAddressOpt(customer3, None),
    CustomerWithAddressOpt(customer4, Some(address2))
  )

  test("groupByOrdered") {
    val groupedByCustomer = rowsFullJoin.groupByOrdered(_.c, _.p)
    assertEquals(
      groupedByCustomer,
      Map(
        customer1 -> Seq(phone1, phone2),
        customer2 -> Seq(phone2)
      )
    )
  }

  test("groupByOrdered identity") {
    val groupedByCustomer = rowsFullJoin.groupByOrdered(_.c)
    assertEquals(
      groupedByCustomer,
      Map(
        customer1 -> Seq(CustomerWithPhone(customer1, phone1), CustomerWithPhone(customer1, phone2)),
        customer2 -> Seq(CustomerWithPhone(customer2, phone2))
      )
    )
  }

  test("groupByOrderedOpt") {
    val groupedByCustomer = rowsLeftJoin.groupByOrderedOpt(_.c, _.a)
    assertEquals(
      groupedByCustomer,
      Map(
        customer1 -> Seq(address1, address2),
        customer2 -> Seq(address1),
        customer3 -> Seq(),
        customer4 -> Seq(address2)
      )
    )
  }

}
