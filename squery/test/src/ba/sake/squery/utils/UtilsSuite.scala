package ba.sake.squery
package utils

class UtilsSuite extends munit.FunSuite {

  val customer1 = Customer(1, "1_customer", None)
  val customer2 = Customer(2, "2_customer", Some("str2"))
  val customer3 = Customer(3, "3_customer", Some("str3"))
  val address1 = Address(1, Some("a1"))
  val address2 = Address(2, Some("a2"))

  val r1 = CustomerWithAddressOpt(customer1, Some(address1))
  val r2 = CustomerWithAddressOpt(customer1, Some(address2))
  val r3 = CustomerWithAddressOpt(customer2, Some(address1))
  val r4 = CustomerWithAddressOpt(customer3, None)
  val rows = Seq(r1, r2, r3, r4)

  test("groupByOrdered") {
    val groupedByCustomer = rows.groupByOrdered(r => (r.c.id, r.c))
    assertEquals(
      groupedByCustomer,
      Map(
        1 -> (customer1, Seq(r1, r2)),
        2 -> (customer2, Seq(r3)),
        3 -> (customer3, Seq(r4))
      )
    )
  }

}
