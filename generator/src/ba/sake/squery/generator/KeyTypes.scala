package ba.sake.squery.generator

import scala.meta._
import scala.meta.dialects.Scala34

/** Resolves each key column to one owning PK column before any files are written. */
private[generator] final class KeyTypes(
    schemas: Seq[SchemaDef],
    packages: Map[String, String],
    rowName: TableDef => String,
    componentName: ColumnDef => String,
    baseType: ColumnDef => Type
) {
  private case class Column(schema: String, table: String, name: String)
  private val tables = schemas.flatMap(_.tables).map(t => (t.schema, t.name) -> t).toMap
  private val primaryKeys = tables.values.toSeq.flatMap(t => t.pkColumns.map(c => column(t, c))).toSet
  private val references: Map[Column, Seq[Column]] = tables.values.toSeq
    .flatMap { table =>
      table.foreignKeys.flatMap { fk =>
        val target = tables.getOrElse(
          (fk.referencedSchema, fk.referencedTable),
          throw new IllegalArgumentException(
            s"Cannot generate Iron key for ${table.schema}.${table.name}.${fk.column}: " +
              s"referenced table ${fk.referencedSchema}.${fk.referencedTable} is not selected. " +
              "Include the referenced table and schema, or set newtypes to none."
          )
        )
        // SQLite permits REFERENCES parent without naming its primary-key columns.
        val targetName = fk.referencedColumn.orElse(target.pkColumns.lift(fk.keySequence - 1).map(_.metadata.name))
        targetName.map(name => Column(target.schema, target.name, name)).filter(primaryKeys).map { referenced =>
          Column(table.schema, table.name, fk.column) -> referenced
        }
      }
    }
    .groupMap(_._1)(_._2)

  private val owners = (primaryKeys ++ references.keySet).map(c => c -> resolve(c, Vector.empty)).toMap

  def columnType(table: TableDef, col: ColumnDef): Option[Type] =
    owners.get(column(table, col)).map(owner => typeRef(owner, table.schema))

  def declarations(table: TableDef): List[Stat] = table.pkColumns.toList.flatMap { col =>
    val own = column(table, col)
    val name = Type.Name(keyName(own))
    val termName = Term.Name(name.value)
    val owner = owners(own)
    if (owner == own) {
      val base = baseType(col)
      List(
        q"type $name = ${Type.Select(termName, Type.Name("T"))}",
        q"object $termName extends RefinedType[$base, Pure] {}"
      )
    } else {
      val targetType = typeRef(owner, table.schema)
      val targetTerm = targetType.syntax.parse[Term].get
      List(q"type $name = $targetType", q"val ${Pat.Var(termName)} = $targetTerm")
    }
  }

  private def column(table: TableDef, col: ColumnDef): Column = Column(table.schema, table.name, col.metadata.name)

  private def keyName(col: Column): String = {
    val table = tables((col.schema, col.table))
    if (table.pkColumns.size == 1) "PK"
    else componentName(table.pkColumns.find(_.metadata.name == col.name).get)
  }

  private def typeRef(owner: Column, fromSchema: String): Type = {
    val table = tables((owner.schema, owner.table))
    val prefix =
      if (owner.schema == fromSchema || packages.isEmpty) rowName(table)
      else s"_root_.${packages(owner.schema)}.models.${rowName(table)}"
    s"$prefix.${keyName(owner)}".parse[Type].get
  }

  private def resolve(col: Column, path: Vector[Column]): Column = {
    val cycleStart = path.indexOf(col)
    if (cycleStart >= 0) path.drop(cycleStart).minBy(c => (c.schema, c.table, c.name))
    else {
      val targets = references.getOrElse(col, Seq.empty).map(c => resolve(c, path :+ col)).distinct
      if (targets.size > 1)
        throw new IllegalArgumentException(
          s"Ambiguous Iron key for ${col.schema}.${col.table}.${col.name}: it references distinct primary keys."
        )
      targets.headOption.getOrElse(col)
    }
  }
}
