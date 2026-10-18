---
title: Codegen CLI
description: Squery code generator command-line options
---

# {{ page.title }}

The `squery-cli_2.13` artifact runs `ba.sake.squery.cli.SqueryMain`. JDBC drivers
must be supplied on the launch classpath. The generated code targets Scala 3.

```shell
cs launch org.xerial:sqlite-jdbc:3.46.1.0 \
  ba.sake:squery-cli_2.13:{{site.data.project.artifact.version}} \
  -M ba.sake.squery.cli.SqueryMain -- --help
```

## Options

Names below match the current CLI's `--help`, including the mixed spelling of
explicitly named options. Camel-case aliases such as `--jdbcUrl` are not the
help-listed options. `--ironNewtypes` is not supported; use `--newtypes iron`.

| Option | Default | Value |
|---|---|---|
| `--jdbc-url` | Required | JDBC connection URL |
| `--schema-mappings` | Empty | Repeatable `schema:package` mapping; supply one mapping per occurrence |
| `--base-folder` | `src/main/scala` | Output source root |
| `--col-name-identifier-mapper` | `camelcase` | `camelcase` or `noop` |
| `--type-name-mapper` | `camelcase` | `camelcase` or `noop` |
| `--row-type-suffix` | `Row` | Row class suffix |
| `--dao-type-suffix` | `Dao` | DAO class suffix |
| `--typeMappingRule` | Empty | Repeatable `column-name-regex\|declared-type-regex\|Scala-type` rule |
| `--includeTables` | All tables | Repeatable regex matching `schema.table` |
| `--excludeTables` | Empty | Repeatable regex matching `schema.table`; exclusions win |
| `--newtypes` | `none` | `none` or `iron`; `iron` requires the application dependency `squery-iron` |
| `--help` | — | Display usage |

An empty schema mapping list generates no schemas. SQLite's schema name is `main`.
Output packages contain `models` and `daos` directories.

## Matching rules

Type mapping rules match the complete column name and the JDBC driver's complete
declared type. Matching is case-sensitive unless the regex enables an option
such as `(?i:...)`. The first matching rule wins, before built-in mappings.
Use fully qualified Scala type names for types outside the generated imports:

```shell
--typeMappingRule '.*_id|(?i:TEXT)|java.util.UUID'
```

Table filters also match complete names. A table must match an include pattern
and no exclude pattern. Omitting includes selects all tables. For example:

```shell
--includeTables 'main\..*' \
--excludeTables 'main\.flyway_schema_history' \
--excludeTables 'main\.sqlite_.*'
```

With Iron keys, referenced tables must be included so their PK types can be
resolved. Excluding a referenced table causes generation to fail before writing
files. Use `--newtypes none` when primitive keys are required instead.

See [Generating Code](../tutorials/codegen.html) for a complete SQLite example.
