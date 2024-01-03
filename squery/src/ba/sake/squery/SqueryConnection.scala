package ba.sake.squery

import java.sql.Connection

@annotation.implicitNotFound(
  "No `SqueryConnection` found. " +
    "Make sure to call this in a `ctx.run{ }` or `ctx.runTransaction{ }` block, " +
    "or set the return type to `DbAction[T]`."
)
case class SqueryConnection(underlying: Connection)
