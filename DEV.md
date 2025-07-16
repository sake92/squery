


```sh


./mill -i clean

./mill -i mill.scalalib.scalafmt/

./mill -i squery.test

./mill examples.runMain bla

# for local dev/test
./mill -i squery.publishLocal

git diff
git commit -am "msg"


VERSION="0.8.0"
git commit --allow-empty -am "Release $VERSION"
git tag -a $VERSION -m "Release $VERSION"
git push  --atomic origin main $VERSION


```

# TODOs

- matrix of supported features (or require an implicit.. e.g. Supports[ReturningGeneratedColumns])
- privatize stuff
- neo4j
- cassandra
- sql parser reorders OFFSET LIMIT ???
