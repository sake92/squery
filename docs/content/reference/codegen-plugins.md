---
title: Codegen build plugins
description: Mill, sbt, and Deder source generation configuration
---

# {{ page.title }}

The build plugins launch `squery-cli_2.13` with the configured JDBC driver.
Application dependencies on `squery`, the runtime JDBC driver, and optionally
`squery-iron` are separate from the generator's classpath.

Installation and build-file examples are maintained in the plugin repositories:

- [mill-squery](https://github.com/sake92/mill-squery): `SqueryGeneratorModule`
- [sbt-squery](https://github.com/sake92/sbt-squery): sbt 2 `SqueryPlugin`
- [deder-squery](https://github.com/sake92/deder-plugins/tree/main/deder-squery): `SqueryPlugin.pkl`

## Configuration names

Mill uses method overrides (`def squeryJdbcUrl = ...`); sbt uses settings
(`squeryJdbcUrl := ...`). Deder uses fields inside `config.defaults` or per-module
`config.modules["module-id"]` overrides.

| Purpose | Mill / sbt | Deder |
|---|---|---|
| JDBC URL | `squeryJdbcUrl` | `jdbcUrl` |
| Generator driver dependencies | `squeryJdbcDeps` | `jdbcDeps` |
| Schema-to-package mappings | `squerySchemaMappings` | `schemaMappings` |
| Column identifier mapper | `squeryColNameIdentifierMapper` | `colNameIdentifierMapper` |
| Type name mapper | `squeryTypeNameMapper` | `typeNameMapper` |
| Row suffix | `squeryRowTypeSuffix` | `rowTypeSuffix` |
| DAO suffix | `squeryDaoTypeSuffix` | `daoTypeSuffix` |
| Ordered type mapping rules | `squeryTypeMappingRules` | `typeMappingRules` |
| Include table regexes | `squeryIncludeTables` | `includeTables` |
| Exclude table regexes | `squeryExcludeTables` | `excludeTables` |
| Generator version | `squeryVersion` | `squeryVersion` |
| Output source root | `squeryTargetDir` | Deder source generator output |

Type rules and table regexes have the same semantics as the
[CLI options](codegen-cli.html). Scala string literals need doubled backslashes:
`Seq("main\\.flyway_schema_history", "main\\.sqlite_.*")`.
Deder uses Pkl listings for dependency/rule/filter collections and a mapping for
`schemaMappings`, for example `schemaMappings { ["main"] = "example.db" }`.

The plugin interfaces listed here do not currently expose `newtypes`.
Use the CLI's `--newtypes iron` or the generator library's `Newtypes.Iron` for
Iron keys until the plugin used by your build supports that setting. The
consuming application then needs the matching `squery-iron` dependency.

## Generation commands and output

| Build tool | Command | Output / integration |
|---|---|---|
| Mill | `./mill myapp.squeryGenerate` | Writes to `squeryTargetDir`, default `moduleDir / "src"`; explicit generation command |
| sbt | `sbt squeryGenerate` | Writes to `squeryTargetDir`, default `Compile / scalaSource` (`src/main/scala`); explicit generation task |
| Deder | `deder exec -t squeryGenerate -m <module-id>` | Writes to source generator output; also runs before compilation |

The generator refactors existing row and DAO files in place while preserving
custom members. Mill and sbt default to ordinary source directories.

## Version defaults

The inspected plugin implementations default to Squery CLI `0.10.0` (Mill) and
`0.12.0` (sbt and Deder). These defaults are independent of your application's
Squery version. `squeryVersion` selects the generator version, so features added
in newer CLI releases require a corresponding version override. Consult the
linked plugin repositories for the settings available in the release you use.
The inspected adapters also pass older camel-case arguments such as `--jdbcUrl`;
compatibility with the current CLI requires updating those to the spellings in
its `--help` (for example, `--jdbc-url`) as well as adding any new configuration.
