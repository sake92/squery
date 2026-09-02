package ba.sake.squery.parser

import fastparse.*
import NoWhitespace.*

/** FastParse-based classification for the optional unsafe UPDATE/DELETE warning. */
private[squery] object SqlStatementLinter {

  def isUpdateOrDeleteWithoutWhere(sql: String): Boolean =
    parse(sql, unsafeMutation(using _)).isSuccess

  private def unsafeMutation[$: P]: P[Unit] =
    P(padding ~ (keyword("update") | keyword("delete")) ~ (!keyword("where") ~ atom).rep ~ End)

  private def atom[$: P]: P[Unit] = P(parenthesized | quoted | comment | identifier | AnyChar)
  private def parenthesized[$: P]: P[Unit] = P("(" ~ (!")" ~ atom).rep ~ ")")
  private def quoted[$: P]: P[Unit] = P("'" ~ ("''" | (!"'" ~ AnyChar)).rep ~ "'")
  private def comment[$: P]: P[Unit] = P("--" ~ (!"\n" ~ AnyChar).rep ~ ("\n" | End) | "/*" ~ (!"*/" ~ AnyChar).rep ~ "*/")
  private def padding[$: P]: P[Unit] = P((CharsWhileIn(" \t\r\n", 1) | comment).rep)
  private def identifier[$: P]: P[Unit] = P(CharsWhileIn("a-zA-Z0-9_$", 1))
  private def keyword[$: P](value: String): P[Unit] = P(IgnoreCase(value) ~ !CharsWhileIn("a-zA-Z0-9_$", 1))
}
