package ba.sake.squery.parser

import fastparse.*
import NoWhitespace.*

/** Adds aliases to the outer SELECT projection so mappers can use stable names.
  * It deliberately recognizes only the SELECT shape needed for this rewrite.
  */
private[squery] object SqlSelectAliasParser {

  private val asAlias = """(?is)^(.+?)\s+as\s+(.+?)\s*$""".r
  private val bareAlias = """(?s)^(.+?)\s+([A-Za-z_][\w$]*)$""".r

  def addAliases(sql: String): Option[String] =
    parse(sql, selectStatement(using _)) match
      case Parsed.Success((prefix, items, suffix), _) => enrich(prefix, items, suffix)
      case _: Parsed.Failure                           => None

  private def enrich(prefix: String, items: Seq[String], suffix: String): Option[String] = {
    var used = items.flatMap(alias).toSet
    val enriched = Vector.newBuilder[String]
    var valid = true
    items.foreach { item =>
      if isWildcard(item) || alias(item).nonEmpty then enriched += item
      else {
        val name = aliasName(item)
        if used(name) then valid = false
        else {
          used += name
          enriched += s"${item.trim} AS \"${name.replace("\"", "\"\"")}\""
        }
      }
    }
    val separator = if suffix.nonEmpty && !suffix.startsWith(";") then " " else ""
    Option.when(valid)(prefix + enriched.result().mkString(", ") + separator + suffix)
  }

  private def selectStatement[$: P]: P[(String, Seq[String], String)] =
    P(outerSelectPrefix ~ modifiers ~ projection.rep(1, sep = ",") ~ tail).map {
      case (prefix, modifiers, items, suffix) => (prefix + modifiers, items, suffix)
    }

  /** Consumes CTEs and nested SELECTs until the first top-level SELECT. */
  private def outerSelectPrefix[$: P]: P[String] =
    P(&(padding ~ (keyword("select") | keyword("with"))) ~ (!keyword("select") ~ outerAtom).rep ~ keyword("select")).!

  private def modifiers[$: P]: P[String] =
    P(padding ~ (distinct | keyword("all") | top).? ~ padding).!

  private def distinct[$: P]: P[Unit] =
    P(keyword("distinct") ~ (padding1 ~ keyword("on") ~ padding ~ parenthesized).?)

  private def top[$: P]: P[Unit] =
    P(keyword("top") ~ padding ~ (parenthesized | CharsWhileIn("0-9", 1)) ~ (padding1 ~ keyword("percent")).?)

  /** Captures one top-level projection, so commas and FROM inside expressions are harmless. */
  private def projection[$: P]: P[String] =
    P((!"," ~ !";" ~ !keyword("from") ~ projectionAtom).rep(1).!).map(_.trim)

  private def tail[$: P]: P[String] =
    P((padding ~ keyword("from") ~ AnyChar.rep ~ End).! | (padding ~ ";".? ~ padding ~ End).!)

  private def outerAtom[$: P]: P[Unit] =
    P(parenthesized | quoted | comment | identifier | plainChar)

  private def projectionAtom[$: P]: P[Unit] = outerAtom

  private def parenthesized[$: P]: P[Unit] = P("(" ~ nestedAtom.rep ~ ")")

  private def nestedAtom[$: P]: P[Unit] =
    P(parenthesized | quoted | comment | !")" ~ (identifier | plainChar))

  private def quoted[$: P]: P[Unit] =
    P(singleQuoted | doubleQuoted | backtickQuoted | bracketQuoted | dollarQuoted)

  private def singleQuoted[$: P]: P[Unit] = P("'" ~ ("''" | (!"'" ~ AnyChar)).rep ~ "'")
  private def doubleQuoted[$: P]: P[Unit] = P("\"" ~ ("\"\"" | (!"\"" ~ AnyChar)).rep ~ "\"")
  private def backtickQuoted[$: P]: P[Unit] = P("`" ~ ("``" | (!"`" ~ AnyChar)).rep ~ "`")
  private def bracketQuoted[$: P]: P[Unit] = P("[" ~ (!"]" ~ AnyChar).rep ~ "]")

  private def dollarQuoted[$: P]: P[Unit] =
    P("$" ~/ CharsWhileIn("a-zA-Z0-9_", 0).! ~ "$").flatMapX { tag =>
      P((!("$" + tag + "$") ~ AnyChar).rep ~ ("$" + tag + "$"))
    }

  private def comment[$: P]: P[Unit] = P(lineComment | blockComment)
  private def lineComment[$: P]: P[Unit] = P("--" ~ (!"\n" ~ AnyChar).rep ~ ("\n" | End))
  private def blockComment[$: P]: P[Unit] = P("/*" ~ (!"*/" ~ AnyChar).rep ~ "*/")

  private def padding[$: P]: P[Unit] = P((CharsWhileIn(" \t\r\n", 1) | comment).rep)
  private def padding1[$: P]: P[Unit] = P(CharsWhileIn(" \t\r\n", 1) | comment)
  private def identifier[$: P]: P[Unit] = P(CharsWhileIn("a-zA-Z0-9_$", 1))
  private def plainChar[$: P]: P[Unit] = P(!("'" | "\"" | "`" | "[" | "(" | "$" | "--" | "/*") ~ AnyChar)

  private def keyword[$: P](value: String): P[Unit] =
    P(IgnoreCase(value) ~ !CharsWhileIn("a-zA-Z0-9_$", 1))

  private def alias(item: String): Option[String] = {
    val trimmed = item.trim
    if trimmed.startsWith("'") || trimmed.startsWith("$") then None
    else
      trimmed match
        case asAlias(_, name) if aliasToken(name) => Some(name.trim)
        case bareAlias(_, name)                   => Some(name)
        case _                                    => None
  }

  private def aliasToken(value: String): Boolean =
    value.trim.matches("""(?s)([A-Za-z_$][\w$]*|\"([^\"]|\"\")+\"|`([^`]|``)+`|\[[^]]+\])""")

  private def isWildcard(item: String): Boolean =
    aliasName(item).matches("""(?s)(\*|[A-Za-z0-9_$.`\"\[\]]+\.\*)""")

  private def aliasName(item: String): String =
    parse(item, commentFree(using _)) match
      case Parsed.Success(value, _) => value.trim
      case _: Parsed.Failure        => item.trim

  private def commentFree[$: P]: P[String] =
    P((quoted.! | comment.map(_ => " ") | AnyChar.!).rep ~ End).map(_.mkString)
}
