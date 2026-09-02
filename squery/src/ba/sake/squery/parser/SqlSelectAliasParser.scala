package ba.sake.squery.parser

import fastparse.*
import NoWhitespace.*

/** Adds stable result-column aliases to the outermost SELECT projection list.
  * Unsupported statements return `None` so their original SQL is executed unchanged.
  */
private[squery] object SqlSelectAliasParser {

  def addAliases(sql: String): Option[String] =
    parse(sql, context => statementStart(using context)) match
      case _: Parsed.Success[?] =>
        findOuterSelect(sql).flatMap { selectStart =>
          val projectionStart = skipSelectModifiers(sql, selectStart)
          splitProjections(sql, projectionStart).flatMap { case (projections, remainder) =>
            var usedAliases = projections.flatMap(existingAlias).toSet
            val enriched = Vector.newBuilder[String]
            var valid = true
            projections.foreach { projection =>
              if isWildcard(projection) || existingAlias(projection).nonEmpty then enriched += projection
              else {
                val alias = aliasExpression(projection)
                if usedAliases.contains(alias) then valid = false
                else {
                  usedAliases += alias
                  enriched += s"${projection.trim} AS \"${alias.replace("\"", "\"\"")}\""
                }
              }
            }
            val prefix = sql.substring(0, projectionStart)
            val prefixSeparator = if prefix.lastOption.exists(_.isWhitespace) then "" else " "
            Option.when(valid)(prefix + prefixSeparator + enriched.result().mkString(", ") + " " + remainder)
          }
        }
      case _: Parsed.Failure => None

  private def statementStart[$: P]: P[Unit] =
    P(ignored ~ (IgnoreCase("select") | IgnoreCase("with")) ~ !CharsWhileIn("a-zA-Z0-9_$"))

  private def ignored[$: P]: P[Unit] =
    P((CharsWhileIn(" \t\r\n").rep(1) | lineComment | blockComment).rep)

  private def lineComment[$: P]: P[Unit] =
    P("--" ~ (!"\n" ~ AnyChar).rep ~ ("\n" | End))

  private def blockComment[$: P]: P[Unit] =
    P("/*" ~ (!"*/" ~ AnyChar).rep ~ "*/")

  private def findOuterSelect(sql: String): Option[Int] =
    scanTopLevelWords(sql).collectFirst { case (word, _, end) if word.equalsIgnoreCase("select") => end }

  private def skipSelectModifiers(sql: String, start: Int): Int = {
    var index = start
    var continue = true
    while continue do
      val afterWhitespace = skipWhitespace(sql, index)
      if isKeywordAt(sql, afterWhitespace, "distinct") || isKeywordAt(sql, afterWhitespace, "all") then
        index = afterWhitespace + (if isKeywordAt(sql, afterWhitespace, "distinct") then 8 else 3)
        if isKeywordAt(sql, skipWhitespace(sql, index), "on") then
          index = skipWhitespace(sql, index) + 2
          index = skipBalanced(sql, skipWhitespace(sql, index)).getOrElse(index)
      else if isKeywordAt(sql, afterWhitespace, "top") then
        index = skipWhitespace(sql, afterWhitespace + 3)
        if index < sql.length && sql.charAt(index) == '(' then index = skipBalanced(sql, index).getOrElse(index)
        else while index < sql.length && sql.charAt(index).isDigit do index += 1
      else continue = false
    index
  }

  private def skipWhitespace(sql: String, start: Int): Int =
    var index = start
    while index < sql.length && sql.charAt(index).isWhitespace do index += 1
    index

  private def skipBalanced(sql: String, start: Int): Option[Int] = {
    if start >= sql.length || sql.charAt(start) != '(' then None
    else {
      var depth = 0
      var index = start
      var result: Option[Int] = None
      while index < sql.length && result.isEmpty do
        sql.charAt(index) match
          case '\'' | '"' | '`' =>
            skipQuoted(sql, index, sql.charAt(index)) match
              case Some(next) => index = next
              case None       => index = sql.length
          case '(' => depth += 1; index += 1
          case ')' =>
            depth -= 1
            index += 1
            if depth == 0 then result = Some(index)
          case _ => index += 1
      result
    }
  }

  private def splitProjections(sql: String, start: Int): Option[(Vector[String], String)] = {
    val projections = Vector.newBuilder[String]
    var depth = 0
    var projectionStart = start
    var index = start
    while index < sql.length do
      sql.charAt(index) match
        case '\'' | '"' | '`' =>
          skipQuoted(sql, index, sql.charAt(index)) match
            case Some(next) => index = next
            case None       => return None
        case '[' =>
          skipBracketIdentifier(sql, index) match
            case Some(next) => index = next
            case None       => return None
        case '$' =>
          skipDollarQuoted(sql, index) match
            case Some(next) => index = next
            case None       => index += 1
        case '-' if startsWith(sql, index, "--") => index = skipLineComment(sql, index)
        case '/' if startsWith(sql, index, "/*") =>
          skipBlockComment(sql, index) match
            case Some(next) => index = next
            case None       => return None
        case '(' => depth += 1; index += 1
        case ')' if depth > 0 => depth -= 1; index += 1
        case ',' if depth == 0 =>
          val projection = sql.substring(projectionStart, index).trim
          if projection.isEmpty then return None
          projections += projection
          projectionStart = index + 1
          index += 1
        case _ if depth == 0 && isKeywordAt(sql, index, "from") =>
          val projection = sql.substring(projectionStart, index).trim
          if projection.isEmpty then return None
          projections += projection
          return Some(projections.result() -> sql.substring(index))
        case _ => index += 1
    val projection = sql.substring(projectionStart).trim.stripSuffix(";").trim
    val remainder = if sql.trim.endsWith(";") then ";" else ""
    Option.when(projection.nonEmpty)(projections.result().appended(projection) -> remainder)
  }

  private def existingAlias(projection: String): Option[String] = {
    val words = scanTopLevelWords(projection)
    words.reverse.collectFirst { case (word, _, end) if word.equalsIgnoreCase("as") => projection.substring(end).trim }.filter(_.nonEmpty).orElse {
      val bareAlias = """(?s)^([\w$\."`\[\]]+)\s+([\w$"`\[\]]+)$""".r
      projection.trim match
        case bareAlias(_, alias) => Some(alias)
        case _                   => None
    }
  }

  private def isWildcard(projection: String): Boolean =
    aliasExpression(projection) == "*" || aliasExpression(projection).matches("""(?s)^[\w$"`\[\].]+\.\*$""")

  private def aliasExpression(projection: String): String = {
    val result = new StringBuilder()
    var index = 0
    while index < projection.length do
      projection.charAt(index) match
        case '\'' | '"' | '`' =>
          skipQuoted(projection, index, projection.charAt(index)) match
            case Some(next) =>
              result.append(projection.substring(index, next))
              index = next
            case None =>
              result.append(projection.substring(index))
              index = projection.length
        case '-' if startsWith(projection, index, "--") =>
          result.append(' ')
          index = skipLineComment(projection, index)
        case '/' if startsWith(projection, index, "/*") =>
          result.append(' ')
          index = skipBlockComment(projection, index).getOrElse(projection.length)
        case '$' =>
          skipDollarQuoted(projection, index) match
            case Some(next) =>
              result.append(projection.substring(index, next))
              index = next
            case None => result.append('$'); index += 1
        case char => result.append(char); index += 1
    result.toString.trim
  }

  private def scanTopLevelWords(sql: String): Vector[(String, Int, Int)] = {
    val words = Vector.newBuilder[(String, Int, Int)]
    var depth = 0
    var index = 0
    while index < sql.length do
      sql.charAt(index) match
        case '\'' | '"' | '`' => index = skipQuoted(sql, index, sql.charAt(index)).getOrElse(sql.length)
        case '[' => index = skipBracketIdentifier(sql, index).getOrElse(sql.length)
        case '$' => index = skipDollarQuoted(sql, index).getOrElse(index + 1)
        case '-' if startsWith(sql, index, "--") => index = skipLineComment(sql, index)
        case '/' if startsWith(sql, index, "/*") => index = skipBlockComment(sql, index).getOrElse(sql.length)
        case '(' => depth += 1; index += 1
        case ')' if depth > 0 => depth -= 1; index += 1
        case c if depth == 0 && (c.isLetter || c == '_') =>
          val start = index
          index += 1
          while index < sql.length && isIdentifierChar(sql.charAt(index)) do index += 1
          words += ((sql.substring(start, index), start, index))
        case _ => index += 1
    words.result()
  }

  private def isKeywordAt(sql: String, index: Int, keyword: String): Boolean =
    sql.regionMatches(true, index, keyword, 0, keyword.length) &&
      (index == 0 || !isIdentifierChar(sql.charAt(index - 1))) &&
      (index + keyword.length == sql.length || !isIdentifierChar(sql.charAt(index + keyword.length)))

  private def isIdentifierChar(char: Char): Boolean = char.isLetterOrDigit || char == '_' || char == '$'
  private def startsWith(sql: String, index: Int, value: String): Boolean = sql.startsWith(value, index)

  private def skipQuoted(sql: String, start: Int, quote: Char): Option[Int] = {
    var index = start + 1
    while index < sql.length do
      if sql.charAt(index) == quote then
        if index + 1 < sql.length && sql.charAt(index + 1) == quote then index += 2
        else return Some(index + 1)
      else index += 1
    None
  }

  private def skipBracketIdentifier(sql: String, start: Int): Option[Int] =
    Option.when(sql.indexOf(']', start + 1) >= 0)(sql.indexOf(']', start + 1) + 1)

  private def skipLineComment(sql: String, start: Int): Int = {
    val end = sql.indexOf('\n', start + 2)
    if end >= 0 then end + 1 else sql.length
  }

  private def skipBlockComment(sql: String, start: Int): Option[Int] =
    Option.when(sql.indexOf("*/", start + 2) >= 0)(sql.indexOf("*/", start + 2) + 2)

  private def skipDollarQuoted(sql: String, start: Int): Option[Int] = {
    val tagEnd = sql.indexOf('$', start + 1)
    if tagEnd <= start then None
    else {
      val tag = sql.substring(start, tagEnd + 1)
      val valueEnd = sql.indexOf(tag, tagEnd + 1)
      Option.when(valueEnd >= 0)(valueEnd + tag.length)
    }
  }
}
