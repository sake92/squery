---
title: How To Interpolation
description: Squery How To Interpolation
---

# {{ page.title }}



## How To Interpolate Literal Strings?

You can use `inline val` (literal type) string values in your queries:
```scala
inline val columns = "id, name, age"
sql"""
  SELECT ${columns}
  FROM customers
""".readRows[Customer]()
```

This will **not** make a `?` parameter, it will directly insert the literal string.  
Same as if you wrote this:
```scala
sql"""
  SELECT id, name, age
  FROM customers
""".readRows[Customer]()
```

## How To Interpolate Values?

You can use any `SqlWrite[T]`-able value in your queries:
```scala
val customerId = 123
val phoneNumber = "123456"
sql"""
  INSERT INTO phones(customer_id, number)
  VALUES($customerId, $phoneNumber)
""".insert()
```

The final query that gets executed will look like this:
  ```sql
  INSERT INTO phones(customer_id, number)
  VALUES(?, ?)
  ```
  so it is injection-safe.




## How To Interpolate Queries?

You can interpolate queries in other queries, just like values:
```scala
val whereQuery = sql"WHERE .."
sql"""
  SELECT customer_id, name
  FROM customers
  ${whereQuery}
""".readRow[Customer]()
```







