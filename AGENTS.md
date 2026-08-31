# Agent Instructions

## Commands

| Task | Command |
|---|---|
| Format | `deder exec -t runMvnApp fmt` |
| Test | `deder exec -t test` |
| Publish locally | `deder exec -t publishLocal` |

## References

| Need | File |
|---|---|
| Development and release commands | `DEV.md` |
| User-facing usage | `README.md` |
| CI test command | `.github/workflows/ci.yml` |

## Conventions

- Keep database-specific codecs under `squery/src/ba/sake/squery/<database>/`.
- Keep database integration tests under `squery/test/src/ba/sake/squery/<database>/`.
- Do not commit specs, plans, or other agent planning artifacts, including `docs/superpowers/specs/` and `docs/superpowers/plans/`.
