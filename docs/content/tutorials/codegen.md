---
title: Generating Code
description: Squery Generating Code Tutorial
---

# {{ page.title }}



Squery has a code generator that can generate code for various databases:  
Postgres, MySQL, MariaDB, Oracle, H2 etc.  

It generates models for table rows and DAOs with various utility methods:
- countAll, countWhere
- findAll, findWhere, findWhereOpt, findAllWhere, findById, findByIdOpt, findByIds
- insert, updateById
- deleteWhere, deleteById, deleteIds

Squery codegen is a bit special since it is using [Regenesca library](https://github.com/sake92/regenesca).  
When you add a new column for example, it will **refactor** the `*Row` and `*Dao` code in place!  
This means you can add your own methods/vals to the *generated code*, without fear that the codegen will remove it.  
Of course, it is best to use `scalafmt` after codegen so that the diff is minimal.



## Standalone generator

You can use it with Ammonite to test the generator:
```scala
import $ivy.`ba.sake:squery-generator_2.13:{{site.data.project.artifact.version}}`
import $ivy.`ba.sake::squery:{{site.data.project.artifact.version}}`
// if using Postgres JSONB
// import $ivy.`ba.sake::squery-postgres-jawn:{{site.data.project.artifact.version}}`

import ba.sake.squery.generator.*

val dataSource = ...
val generator = SqueryGenerator(dataSource)
val generatedCode = generator.generateString(Seq("myschema"))
repl.load(generatedCode)

// now you can use the generated code
val ctx = SqueryContext(dataSource)
ctx.run {
  MyTableDao.findAll()
}
```


## Mill plugin

See how it works in the dedicated GitHub repo https://github.com/sake92/mill-squery







