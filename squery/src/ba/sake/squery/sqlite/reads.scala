package ba.sake.squery.sqlite

import java.{sql => jsql}
import java.time.{Instant, LocalDate, LocalDateTime}
import java.util.UUID
import ba.sake.squery.read.*

given SqlRead[UUID] with
  def readByName(jRes: jsql.ResultSet, colName: String): Option[UUID] =
    Option(jRes.getString(colName)).map(UUID.fromString)

  def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[UUID] =
    Option(jRes.getString(colIdx)).map(UUID.fromString)

given SqlRead[Boolean] with
  def readByName(jRes: jsql.ResultSet, colName: String): Option[Boolean] =
    Option(jRes.getInt(colName)).filterNot(_ => jRes.wasNull()).map(_ != 0)

  def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[Boolean] =
    Option(jRes.getInt(colIdx)).filterNot(_ => jRes.wasNull()).map(_ != 0)

given SqlRead[Instant] with
  def readByName(jRes: jsql.ResultSet, colName: String): Option[Instant] =
    Option(jRes.getString(colName)).map(Instant.parse)

  def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[Instant] =
    Option(jRes.getString(colIdx)).map(Instant.parse)

given SqlRead[LocalDate] with
  def readByName(jRes: jsql.ResultSet, colName: String): Option[LocalDate] =
    Option(jRes.getString(colName)).map(LocalDate.parse)

  def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[LocalDate] =
    Option(jRes.getString(colIdx)).map(LocalDate.parse)

given SqlRead[LocalDateTime] with
  def readByName(jRes: jsql.ResultSet, colName: String): Option[LocalDateTime] =
    Option(jRes.getString(colName)).map(LocalDateTime.parse)

  def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[LocalDateTime] =
    Option(jRes.getString(colIdx)).map(LocalDateTime.parse)
