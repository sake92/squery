/* Automatically generated code by squery generator */
package pagila.models

import java.time.*
import java.util.UUID
import ba.sake.squery.*
import ba.sake.squery.postgres.given
import ba.sake.squery.read.SqlRead
import ba.sake.squery.write.SqlWrite

enum MpaaRating derives SqlRead, SqlWrite:
    case G
    case PG
    case `PG-13`
    case R
    case `NC-17`


case class ActorRow(
    actor_id: Int,
    first_name: String,
    last_name: String,
    last_update: Instant
) derives SqlReadRow

object ActorRow {
  inline val actor_id = "actor_id"
  inline val first_name = "first_name"
  inline val last_name = "last_name"
  inline val last_update = "last_update"

  inline val * = prefixed("")

  transparent inline def prefixed(inline prefix: String) =
    prefix + actor_id + "," + prefix + first_name + "," + prefix + last_name + "," + prefix + last_update
}

case class AddressRow(
    address_id: Int,
    address: String,
    address2: Option[String],
    district: String,
    city_id: Int,
    postal_code: Option[String],
    phone: String,
    last_update: Instant
) derives SqlReadRow

object AddressRow {
  inline val address_id = "address_id"
  inline val address = "address"
  inline val address2 = "address2"
  inline val district = "district"
  inline val city_id = "city_id"
  inline val postal_code = "postal_code"
  inline val phone = "phone"
  inline val last_update = "last_update"

  inline val * = prefixed("")

  transparent inline def prefixed(inline prefix: String) =
    prefix + address_id + "," + prefix + address + "," + prefix + address2 + "," + prefix + district + "," + prefix + city_id + "," + prefix + postal_code + "," + prefix + phone + "," + prefix + last_update
}

case class CategoryRow(
    category_id: Int,
    name: String,
    last_update: Instant
) derives SqlReadRow

object CategoryRow {
  inline val category_id = "category_id"
  inline val name = "name"
  inline val last_update = "last_update"

  inline val * = prefixed("")

  transparent inline def prefixed(inline prefix: String) =
    prefix + category_id + "," + prefix + name + "," + prefix + last_update
}

case class CityRow(
    city_id: Int,
    city: String,
    country_id: Int,
    last_update: Instant
) derives SqlReadRow

object CityRow {
  inline val city_id = "city_id"
  inline val city = "city"
  inline val country_id = "country_id"
  inline val last_update = "last_update"

  inline val * = prefixed("")

  transparent inline def prefixed(inline prefix: String) =
    prefix + city_id + "," + prefix + city + "," + prefix + country_id + "," + prefix + last_update
}

case class CountryRow(
    country_id: Int,
    country: String,
    last_update: Instant
) derives SqlReadRow

object CountryRow {
  inline val country_id = "country_id"
  inline val country = "country"
  inline val last_update = "last_update"

  inline val * = prefixed("")

  transparent inline def prefixed(inline prefix: String) =
    prefix + country_id + "," + prefix + country + "," + prefix + last_update
}

case class CustomerRow(
    customer_id: Int,
    store_id: Int,
    first_name: String,
    last_name: String,
    email: Option[String],
    address_id: Int,
    activebool: Boolean,
    create_date: LocalDate,
    last_update: Option[Instant],
    active: Option[Int]
) derives SqlReadRow

object CustomerRow {
  inline val customer_id = "customer_id"
  inline val store_id = "store_id"
  inline val first_name = "first_name"
  inline val last_name = "last_name"
  inline val email = "email"
  inline val address_id = "address_id"
  inline val activebool = "activebool"
  inline val create_date = "create_date"
  inline val last_update = "last_update"
  inline val active = "active"

  inline val * = prefixed("")

  transparent inline def prefixed(inline prefix: String) =
    prefix + customer_id + "," + prefix + store_id + "," + prefix + first_name + "," + prefix + last_name + "," + prefix + email + "," + prefix + address_id + "," + prefix + activebool + "," + prefix + create_date + "," + prefix + last_update + "," + prefix + active
}

case class FilmRow(
    film_id: Int,
    title: String,
    description: Option[String],
    release_year: Option[Int],
    language_id: Int,
    original_language_id: Option[Int],
    rental_duration: Short,
    rental_rate: Double,
    length: Option[Short],
    replacement_cost: Double,
    rating: Option[MpaaRating],
    last_update: Instant,
    special_features: Option[Array[String]],
    fulltext: String,
    special_features2: Option[Array[Array[String]]]
) derives SqlReadRow

object FilmRow {
  inline val film_id = "film_id"
  inline val title = "title"
  inline val description = "description"
  inline val release_year = "release_year"
  inline val language_id = "language_id"
  inline val original_language_id = "original_language_id"
  inline val rental_duration = "rental_duration"
  inline val rental_rate = "rental_rate"
  inline val length = "length"
  inline val replacement_cost = "replacement_cost"
  inline val rating = "rating"
  inline val last_update = "last_update"
  inline val special_features = "special_features"
  inline val fulltext = "fulltext"
  inline val special_features2 = "special_features2"

  inline val * = prefixed("")

  transparent inline def prefixed(inline prefix: String) =
    prefix + film_id + "," + prefix + title + "," + prefix + description + "," + prefix + release_year + "," + prefix + language_id + "," + prefix + original_language_id + "," + prefix + rental_duration + "," + prefix + rental_rate + "," + prefix + length + "," + prefix + replacement_cost + "," + prefix + rating + "," + prefix + last_update + "," + prefix + special_features + "," + prefix + fulltext + "," + prefix + special_features2
}

case class FilmActorRow(
    actor_id: Int,
    film_id: Int,
    last_update: Instant
) derives SqlReadRow

object FilmActorRow {
  inline val actor_id = "actor_id"
  inline val film_id = "film_id"
  inline val last_update = "last_update"

  inline val * = prefixed("")

  transparent inline def prefixed(inline prefix: String) =
    prefix + actor_id + "," + prefix + film_id + "," + prefix + last_update
}

case class FilmCategoryRow(
    film_id: Int,
    category_id: Int,
    last_update: Instant
) derives SqlReadRow

object FilmCategoryRow {
  inline val film_id = "film_id"
  inline val category_id = "category_id"
  inline val last_update = "last_update"

  inline val * = prefixed("")

  transparent inline def prefixed(inline prefix: String) =
    prefix + film_id + "," + prefix + category_id + "," + prefix + last_update
}

case class InventoryRow(
    inventory_id: Int,
    film_id: Int,
    store_id: Int,
    last_update: Instant
) derives SqlReadRow

object InventoryRow {
  inline val inventory_id = "inventory_id"
  inline val film_id = "film_id"
  inline val store_id = "store_id"
  inline val last_update = "last_update"

  inline val * = prefixed("")

  transparent inline def prefixed(inline prefix: String) =
    prefix + inventory_id + "," + prefix + film_id + "," + prefix + store_id + "," + prefix + last_update
}

case class LanguageRow(
    language_id: Int,
    name: String,
    last_update: Instant
) derives SqlReadRow

object LanguageRow {
  inline val language_id = "language_id"
  inline val name = "name"
  inline val last_update = "last_update"

  inline val * = prefixed("")

  transparent inline def prefixed(inline prefix: String) =
    prefix + language_id + "," + prefix + name + "," + prefix + last_update
}

case class PaymentP202201Row(
    payment_id: Int,
    customer_id: Int,
    staff_id: Int,
    rental_id: Int,
    amount: Double,
    payment_date: Instant
) derives SqlReadRow

object PaymentP202201Row {
  inline val payment_id = "payment_id"
  inline val customer_id = "customer_id"
  inline val staff_id = "staff_id"
  inline val rental_id = "rental_id"
  inline val amount = "amount"
  inline val payment_date = "payment_date"

  inline val * = prefixed("")

  transparent inline def prefixed(inline prefix: String) =
    prefix + payment_id + "," + prefix + customer_id + "," + prefix + staff_id + "," + prefix + rental_id + "," + prefix + amount + "," + prefix + payment_date
}

case class PaymentP202202Row(
    payment_id: Int,
    customer_id: Int,
    staff_id: Int,
    rental_id: Int,
    amount: Double,
    payment_date: Instant
) derives SqlReadRow

object PaymentP202202Row {
  inline val payment_id = "payment_id"
  inline val customer_id = "customer_id"
  inline val staff_id = "staff_id"
  inline val rental_id = "rental_id"
  inline val amount = "amount"
  inline val payment_date = "payment_date"

  inline val * = prefixed("")

  transparent inline def prefixed(inline prefix: String) =
    prefix + payment_id + "," + prefix + customer_id + "," + prefix + staff_id + "," + prefix + rental_id + "," + prefix + amount + "," + prefix + payment_date
}

case class PaymentP202203Row(
    payment_id: Int,
    customer_id: Int,
    staff_id: Int,
    rental_id: Int,
    amount: Double,
    payment_date: Instant
) derives SqlReadRow

object PaymentP202203Row {
  inline val payment_id = "payment_id"
  inline val customer_id = "customer_id"
  inline val staff_id = "staff_id"
  inline val rental_id = "rental_id"
  inline val amount = "amount"
  inline val payment_date = "payment_date"

  inline val * = prefixed("")

  transparent inline def prefixed(inline prefix: String) =
    prefix + payment_id + "," + prefix + customer_id + "," + prefix + staff_id + "," + prefix + rental_id + "," + prefix + amount + "," + prefix + payment_date
}

case class PaymentP202204Row(
    payment_id: Int,
    customer_id: Int,
    staff_id: Int,
    rental_id: Int,
    amount: Double,
    payment_date: Instant
) derives SqlReadRow

object PaymentP202204Row {
  inline val payment_id = "payment_id"
  inline val customer_id = "customer_id"
  inline val staff_id = "staff_id"
  inline val rental_id = "rental_id"
  inline val amount = "amount"
  inline val payment_date = "payment_date"

  inline val * = prefixed("")

  transparent inline def prefixed(inline prefix: String) =
    prefix + payment_id + "," + prefix + customer_id + "," + prefix + staff_id + "," + prefix + rental_id + "," + prefix + amount + "," + prefix + payment_date
}

case class PaymentP202205Row(
    payment_id: Int,
    customer_id: Int,
    staff_id: Int,
    rental_id: Int,
    amount: Double,
    payment_date: Instant
) derives SqlReadRow

object PaymentP202205Row {
  inline val payment_id = "payment_id"
  inline val customer_id = "customer_id"
  inline val staff_id = "staff_id"
  inline val rental_id = "rental_id"
  inline val amount = "amount"
  inline val payment_date = "payment_date"

  inline val * = prefixed("")

  transparent inline def prefixed(inline prefix: String) =
    prefix + payment_id + "," + prefix + customer_id + "," + prefix + staff_id + "," + prefix + rental_id + "," + prefix + amount + "," + prefix + payment_date
}

case class PaymentP202206Row(
    payment_id: Int,
    customer_id: Int,
    staff_id: Int,
    rental_id: Int,
    amount: Double,
    payment_date: Instant
) derives SqlReadRow

object PaymentP202206Row {
  inline val payment_id = "payment_id"
  inline val customer_id = "customer_id"
  inline val staff_id = "staff_id"
  inline val rental_id = "rental_id"
  inline val amount = "amount"
  inline val payment_date = "payment_date"

  inline val * = prefixed("")

  transparent inline def prefixed(inline prefix: String) =
    prefix + payment_id + "," + prefix + customer_id + "," + prefix + staff_id + "," + prefix + rental_id + "," + prefix + amount + "," + prefix + payment_date
}

case class PaymentP202207Row(
    payment_id: Int,
    customer_id: Int,
    staff_id: Int,
    rental_id: Int,
    amount: Double,
    payment_date: Instant
) derives SqlReadRow

object PaymentP202207Row {
  inline val payment_id = "payment_id"
  inline val customer_id = "customer_id"
  inline val staff_id = "staff_id"
  inline val rental_id = "rental_id"
  inline val amount = "amount"
  inline val payment_date = "payment_date"

  inline val * = prefixed("")

  transparent inline def prefixed(inline prefix: String) =
    prefix + payment_id + "," + prefix + customer_id + "," + prefix + staff_id + "," + prefix + rental_id + "," + prefix + amount + "," + prefix + payment_date
}

case class RentalRow(
    rental_id: Int,
    rental_date: Instant,
    inventory_id: Int,
    customer_id: Int,
    return_date: Option[Instant],
    staff_id: Int,
    last_update: Instant
) derives SqlReadRow

object RentalRow {
  inline val rental_id = "rental_id"
  inline val rental_date = "rental_date"
  inline val inventory_id = "inventory_id"
  inline val customer_id = "customer_id"
  inline val return_date = "return_date"
  inline val staff_id = "staff_id"
  inline val last_update = "last_update"

  inline val * = prefixed("")

  transparent inline def prefixed(inline prefix: String) =
    prefix + rental_id + "," + prefix + rental_date + "," + prefix + inventory_id + "," + prefix + customer_id + "," + prefix + return_date + "," + prefix + staff_id + "," + prefix + last_update
}

case class StaffRow(
    staff_id: Int,
    first_name: String,
    last_name: String,
    address_id: Int,
    email: Option[String],
    store_id: Int,
    active: Boolean,
    username: String,
    password: Option[String],
    last_update: Instant,
    picture: Option[Array[Byte]]
) derives SqlReadRow

object StaffRow {
  inline val staff_id = "staff_id"
  inline val first_name = "first_name"
  inline val last_name = "last_name"
  inline val address_id = "address_id"
  inline val email = "email"
  inline val store_id = "store_id"
  inline val active = "active"
  inline val username = "username"
  inline val password = "password"
  inline val last_update = "last_update"
  inline val picture = "picture"

  inline val * = prefixed("")

  transparent inline def prefixed(inline prefix: String) =
    prefix + staff_id + "," + prefix + first_name + "," + prefix + last_name + "," + prefix + address_id + "," + prefix + email + "," + prefix + store_id + "," + prefix + active + "," + prefix + username + "," + prefix + password + "," + prefix + last_update + "," + prefix + picture
}

case class StoreRow(
    store_id: Int,
    manager_staff_id: Int,
    address_id: Int,
    last_update: Instant
) derives SqlReadRow

object StoreRow {
  inline val store_id = "store_id"
  inline val manager_staff_id = "manager_staff_id"
  inline val address_id = "address_id"
  inline val last_update = "last_update"

  inline val * = prefixed("")

  transparent inline def prefixed(inline prefix: String) =
    prefix + store_id + "," + prefix + manager_staff_id + "," + prefix + address_id + "," + prefix + last_update
}