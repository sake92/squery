package ba.sake.squery

import java.sql.Connection

@annotation.implicitNotFound(
  "No database connection found. Make sure to call this in a `ctx.run{ }` or `ctx.runTransaction{ }` block."
)
case class SqueryConnection(underlying: Connection)
