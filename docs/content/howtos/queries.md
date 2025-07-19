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

---

There are also variations that return a single result, depending if you want an `Option[T]` or `T`:
```scala
sql"SELECT ...".readRowOpt[T]() : Option[T] // first result, if present
sql"SELECT ...".readRow[T]() : T            // first result, or exception
```


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
You can use the `++` operator on queries:
```scala
def customers(): Seq[Customer] = ctx.run {
  val conds = List(sql"id = 123", sql"name LIKE 'Bob%'")
  val condsQuery = conds.reduce(_ ++ _)
  val query = sql"SELECT id, name FROM customers ${condsQuery}"
  query.readRows[Customer]()
}
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






