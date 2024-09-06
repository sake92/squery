package ba.sake.squery.postgres

import ba.sake.squery.write.SqlTypeName
import java.util.UUID

given SqlTypeName[UUID] with {
  def value: String = "UUID"
}
