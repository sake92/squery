package ba.sake.squery

import com.typesafe.scalalogging.Logger

private[squery] object SqueryLoggerFactory {
  def apply(name: String): SqueryLogger = new JvmSqueryLogger(Logger(name))

  private final class JvmSqueryLogger(underlying: Logger) extends SqueryLogger {
    def trace(message: => String): Unit = underlying.trace(message)
    def debug(message: => String): Unit = underlying.debug(message)
    def warn(message: => String): Unit = underlying.warn(message)
  }
}
