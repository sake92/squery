


```sh


./mill clean

./mill __.reformat

./mill squery.test

./mill examples.runMain bla

# for local dev/test
./mill  squery.publishLocal

git diff
git commit -am "msg"


$VERSION="0.5.0"
git commit --allow-empty -am "Release $VERSION"
git tag -a $VERSION -m "Release $VERSION"
git push  --atomic origin main $VERSION


```

# TODOs

- matrix of supported features (or require an implicit.. e.g. Supports[ReturningGeneratedColumns])
- update scastie!
- write tutorials
- privatize stuff
- neo4j
- cassandra
- sql parser reorders OFFSET LIMIT ???
- generate (id, name) + corresponding insert (?, ?)
    depending on autoincrement/serial stuff