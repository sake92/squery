package ba.sake.squery.read

import java.{sql => jsql}

import magnolia2.{*, given}

// TODO typeclassss
// reads a row (or just part of it if used in composition)
trait SqlReadRow[T]:
  def readRow(jRes: jsql.ResultSet, prefix: Option[String]): T

object SqlReadRow extends AutoDerivation[SqlReadRow]:
  def apply[T](using sqlReadRow: SqlReadRow[T]): SqlReadRow[T] = sqlReadRow

  override def join[T](ctx: CaseClass[Typeclass, T]): JsonRW[T] = new {
    def readRow(jRes: jsql.ResultSet, prefix: Option[String]): T = {}
  }
