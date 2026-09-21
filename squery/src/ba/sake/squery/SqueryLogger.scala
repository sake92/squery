package ba.sake.squery

private[squery] trait SqueryLogger {
  def trace(message: => String): Unit
  def debug(message: => String): Unit
  def warn(message: => String): Unit
}
