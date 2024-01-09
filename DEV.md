


```sh


./mill clean

./mill __.reformat

./mill squery.test

./mill examples.runMain bla

# for local dev/test
./mill  squery.publishM2Local

git diff
git commit -am "msg"


$VERSION="0.2.0"
git commit --allow-empty -am "Release $VERSION"
git tag -a $VERSION -m "Release $VERSION"
git push  --atomic origin main $VERSION


```

# TODOs

- update scastie!
- more tutorials
- privatize stuff
- test more databases
- sql parser reorders OFFSET LIMIT ???
