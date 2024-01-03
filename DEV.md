


```sh


./mill clean

./mill __.reformat

./mill squery.test

./mill examples.runMain bla

# for local dev/test
./mill  squery.publishM2Local

git diff
git commit -am "msg"


$VERSION="0.0.19"
git commit --allow-empty -am "Release $VERSION"
git tag -a $VERSION -m "Release $VERSION"
git push  --atomic origin main $VERSION


```

# TODOs

- privatize stuff
- test more databases
- sql parser reorders OFFSET LIMIT ???
