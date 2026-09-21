package ba.sake.squery

private[squery] object SqueryLoggerFactory {
  def apply(name: String): SqueryLogger = new NativeSqueryLogger(System.getLogger(name))

  private final class NativeSqueryLogger(underlying: System.Logger) extends SqueryLogger {
    def trace(message: => String): Unit = log(System.Logger.Level.TRACE, message)
    def debug(message: => String): Unit = log(System.Logger.Level.DEBUG, message)
    def warn(message: => String): Unit = log(System.Logger.Level.WARNING, message)

    private def log(level: System.Logger.Level, message: => String): Unit =
      if underlying.isLoggable(level) then underlying.log(level, message)
  }
}
