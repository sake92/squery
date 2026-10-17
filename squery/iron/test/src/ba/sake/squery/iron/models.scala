package ba.sake.squery.iron

import java.util.UUID
import ba.sake.squery.SqlReadRow
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.any.Pure
import io.github.iltotore.iron.constraint.numeric.Positive

// This is the proposed shape for generated single-column PKs.
object UserRow:
  type PK = PK.T
  object PK extends RefinedType[Long, Pure]

case class UserRow(id: UserRow.PK, name: String) derives SqlReadRow:
  def pk: UserRow.PK = id

object OrderRow:
  type PK = PK.T
  object PK extends RefinedType[Long, Pure]

// A FK uses the referenced PK, including inside Option for nullable FKs.
case class OrderRow(id: OrderRow.PK, user_id: UserRow.PK, reviewer_id: Option[UserRow.PK]) derives SqlReadRow:
  def pk: OrderRow.PK = id

type Label = Label.T
object Label extends RefinedType[String, Pure]

type Token = Token.T
object Token extends RefinedType[UUID, Pure]

type Quantity = Quantity.T
object Quantity extends RefinedType[Int, Positive]
