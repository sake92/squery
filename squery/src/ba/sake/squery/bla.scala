package ba.sake.squery

import ba.sake.squery.read.SqlReadRow

@main def main: Unit = {

  val c = Customer(1, "Sake")

  val q = sql"""
    INSERT INTO customers(id, name)
    VALUES (${c.id}, ${c.name})
  """

  println(q)

}

case class Customer(id: Int, name: String) derives SqlReadRow
