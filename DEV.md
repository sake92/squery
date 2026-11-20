


```sh
brew install sake92/tap/deder

# clear local Deder state if needed
deder shutdown
rm -rf .deder

deder exec -t runMvnApp fmt
deder exec -t test

# for local dev/test
deder exec -t publishLocal

git diff
git commit -am "msg"

VERSION="0.13.0"
git commit --allow-empty -am "Release $VERSION"
git tag -a $VERSION -m "Release $VERSION"
git push --atomic origin main $VERSION
```

When releasing Squery, update the version in the README, documentation examples,
and `docs/_data/project.yaml`. Keep the Markdown examples ready to copy so Context7
can index them directly.

# TODOs

- matrix of supported features (or require an implicit.. e.g. Supports[ReturningGeneratedColumns])
- privatize stuff
- neo4j
- cassandra
- sql parser reorders OFFSET LIMIT ???
