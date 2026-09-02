package ba.sake.squery.parser

/** Minimal statement classification used solely for the optional unsafe-update warning. */
private[squery] object SqlStatementLinter {

  def isUpdateOrDeleteWithoutWhere(sql: String): Boolean =
    firstKeyword(sql).exists(keyword => (keyword.equalsIgnoreCase("update") || keyword.equalsIgnoreCase("delete")) && !hasTopLevelWhere(sql))

  private def firstKeyword(sql: String): Option[String] =
    sql.dropWhile(_.isWhitespace).takeWhile(char => char.isLetter || char == '_') match
      case ""      => None
      case keyword => Some(keyword)

  private def hasTopLevelWhere(sql: String): Boolean =
    var depth = 0
    var index = 0
    var found = false
    while index < sql.length do
      sql.charAt(index) match
        case '\'' =>
          skipString(sql, index) match
            case Some(next) => index = next
            case None       => index = sql.length
        case '('  => depth += 1; index += 1
        case ')' if depth > 0 => depth -= 1; index += 1
        case char if depth == 0 && char.isLetter =>
          val start = index
          index += 1
          while index < sql.length && sql.charAt(index).isLetter do index += 1
          if sql.substring(start, index).equalsIgnoreCase("where") then found = true
        case _ => index += 1
    found

  private def skipString(sql: String, start: Int): Option[Int] = {
    var index = start + 1
    while index < sql.length do
      if sql.charAt(index) == '\'' then
        if index + 1 < sql.length && sql.charAt(index + 1) == '\'' then index += 2
        else return Some(index + 1)
      else index += 1
    None
  }
}
