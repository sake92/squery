---
title: How To Queries
description: Squery How To Queries
---

# {{ page.title }}

## How to Read One-Column Values?

We use the `readValues[T]()` to read single-column results:
```scala
import ba.sake.squery.{*, given}

def customersIds: List[Int] = ctx.run {
  sql"SELECT id FROM customers".readValues[Int]()
}
```

---

There are also variations that return a single result, depending if you want an `Option[T]` or `T`:
```scala
sql"SELECT ...".readValueOpt[T]() : Option[T] // first result, if present
sql"SELECT ...".readValue[T]() : T            // first result, or exception
```


## How to Read Multi-Column Values?

When reading multi-column results, we care about the column names.  
A natural fit for that are `case class`es.  
We need to add a `derives SqlReadRow`, and then we can use it:
```scala
import ba.sake.squery.{*, given}

case class Customer(id: Int, name: String) derives SqlReadRow

def customers: List[Customer] = ctx.run {
  sql"SELECT id, name FROM customers".readRows[Customer]()
}
```
Note that the `case class`' fields need to match the `SELECT` statement columns!

## How to Configure Statement Execution?

Statement options are immutable and can be chained on any query:

```scala
import scala.concurrent.duration.*

sql"SELECT id, name FROM customers"
  .withFetchSize(512)
  .withTimeout(5.seconds)
  .withMaxRows(1000)
  .readRows[Customer]()
```

Scrollable or updatable result sets can be requested when the JDBC driver supports them:

```scala
sql"SELECT id, name FROM customers"
  .withResultSet(ResultSetType.ScrollInsensitive, ResultSetConcurrency.ReadOnly)
  .readRows[Customer]()
```

JDBC does not provide a statement-preparation overload that combines result-set settings with generated-key selection.
When both fluent options are used, the last one replaces the earlier preparation option.

---

There are also variations that return a single result, depending if you want an `Option[T]` or `T`:
```scala
sql"SELECT ...".readRowOpt[T]() : Option[T] // first result, if present
sql"SELECT ...".readRow[T]() : T            // first result, or exception
```


## How to Read Rows as Named Tuples?

On Scala 3.7 or newer, a named tuple can be used instead of declaring a `case class`:
```scala
def customers: Seq[(id: Int, name: String)] = ctx.run {
  sql"SELECT id, name FROM customers"
    .readRows[(id: Int, name: String)]()
}

val firstCustomerName = customers.head.name
```

Named tuples can also be nested for joins. The outer field name becomes the column prefix, just as it does with composed `case class`es:
```scala
type Customer = (id: Int, name: String)
type Phone = (id: Int, number: String)
type CustomerWithPhone = (c: Customer, p: Phone)

def customersWithPhones: Seq[CustomerWithPhone] = ctx.run {
  sql"""
    SELECT c.id, c.name,
           p.id, p.number
    FROM customers c
    JOIN phones p ON p.customer_id = c.id
  """.readRows[CustomerWithPhone]()
}
```

For an outer join, use an optional nested tuple such as `p: Option[Phone]`.


## How to Read a Full Join?

Squery is using `case class` composition to read `JOIN`ed tables.  
Let's say we have tables whose rows are represented by these 2 case classes:
```scala
case class Customer(id: Int, name: String) derives SqlReadRow
case class Phone(id: Int, number: String) derives SqlReadRow
```

Doing a `FULL JOIN` on them would look like this:
```sql
SELECT  c.id, c.name,
        p.id, p.number
FROM customers c
JOIN phones p ON p.customer_id = c.id     
```

The result can be expressed as a composition of the 2 `case class`es above:
```scala
case class CustomerWithPhone(c: Customer, p: Phone) derives SqlReadRow
```

Variables like `c: Customer` are expected to have *corresponding column names* in the query: `c.id` and `c.name`.  
The final query is a composition of `Customer` and `Phone`, so it maps nicely in your head, it is easier to read and manipulate.  

You could have additional columns like a `COUNT`/`SUM` or whatever you need in `CustomerWithPhone` query result.

---

Full example:
```scala
import ba.sake.squery.{*, given}

case class Customer(id: Int, name: String) derives SqlReadRow
case class Phone(id: Int, number: String) derives SqlReadRow

case class CustomerWithPhone(c: Customer, p: Phone) derives SqlReadRow

def customerwithPhones: List[CustomerWithPhone] = ctx.run {
  sql"""
    SELECT c.id, c.name,
          p.id, p.number
    FROM customers c
    JOIN phones p ON p.customer_id = c.id
  """.readRows[CustomerWithPhone]()
}
```


## How to Read an Outer Join?

The principle is the same as for `FULL JOIN`.

The only thing you need change is to make the `JOIN`-ed table an `Option[T]`:
```scala
case class CustomerWithPhoneOpt(c: Customer, p: Option[Phone]) derives SqlReadRow

def customerwithPhoneOpts: List[CustomerWithPhoneOpt] = ctx.run {
  sql"SELECT ...".readRows[CustomerWithPhoneOpt]()
}
```

The `p: Option[Phone]` will be `None` when all its returned columns are `NULL`.



## How to Map Flat Rows to Objects

Suppose you have these data `class`-es:
```scala
case class Customer(id: Int, name: String) derives SqlReadRow
case class Address(id: Int, name: String) derives SqlReadRow
case class CustomerWithAddressOpt(c: Customer, a: Option[Address]) derives SqlReadRow
```

Now, when you do a `SELECT` you'll get a `Seq[CustomerWithPhoneOpt]`, a "flat" result, named tuple.  
But, on your REST API you'd like to return a structured object:
```scala
case class CustomerDTO(id: Int, name: String, addresses: Seq[String])
```

Squery has utilities for exactly that.  
Let's see how `groupByOrderedOpt` works:
```scala
import ba.sake.squery.utils.*

val groupedByCustomer: Map[Customer, Seq[Address]] = rowsLeftJoin.groupByOrderedOpt(_.c, _.a)
val dtos = groupedByCustomer.map { case (c, addresses) =>
  CustomerDTO(c.id, c.name, addresses.map(_.name))
}.toSeq
```
We can see that it returns a `Map[Customer, Seq[Address]]`, just as we wanted.  
Then we just map over it and populate the DTO object, can't be simpler!

---

This does a few thing for us:
- keeps the list of results *ordered*, so you don't have to sort it twice (once in DB, and again in memory)
- extracts the value that we need from the raw row result
- handles the `None` case
- handles the starting, empty Seq of results case


## How To Do Dynamic Queries?

Of course, in the real world, you will need to compose queries dynamically at runtime.  
Use `Query.join` to combine any number of fragments with a separator:
```scala
def customers(): Seq[Customer] = ctx.run {
  val filters = List(sql"id = 123", sql"name LIKE 'Bob%'")
  val where = Query.when(filters.nonEmpty) {
    sql"WHERE ${Query.join(filters, sql" AND ")}"
  }
  val query = sql"SELECT id, name FROM customers ${where}"
  query.readRows[Customer]()
}
```

`Query.join` returns an empty fragment for an empty collection and preserves the
separator exactly as supplied. Values interpolated into any fragment remain prepared
statement parameters.

Use `Query.when` for conditional fragments:
```scala
val orderBy = Query.when(sortByName)(sql"ORDER BY name")
sql"SELECT id, name FROM customers ${orderBy}"
```

Use `Query.in` for a dynamic `IN` list:
```scala
val ids = Seq(1, 2, 3)
sql"SELECT id, name FROM customers WHERE id IN ${Query.in(ids)}"
```

An empty collection produces `(NULL)`, so a positive `IN` predicate safely matches
no rows. Do not use that empty-list behavior for `NOT IN`, where SQL `NULL` semantics
are different.

Use `Query.values` to join already-parenthesized rows for a multi-row insert:
```scala
val rows = customers.map(customer => sql"(${customer.name}, ${customer.street})")
sql"INSERT INTO customers(name, street) VALUES ${Query.values(rows)}"
```

`Query.values` requires at least one row and reports an `IllegalArgumentException`
before executing SQL when the collection is empty.

You can still use `++` when directly appending two fragments:
```scala
sql"name = 'Alice'" ++ sql"AND active = true"
```

---

There are also some utils in the `ba.sake.squery.utils` package.  
For example, if you want to optionally filter on some columns, you can use `concatenate` function:
```scala
Seq(Option(sql"q1"), None, Option(sql"q2"))
  .concatenate(sep = sql"AND", default = sql"true")

// same as this:
sql"q1 AND q2"
```



