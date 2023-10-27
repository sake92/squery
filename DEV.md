


```sh


./mill clean

./mill __.reformat

./mill squery.test

./mill examples.runMain bla

# for local dev/test
./mill  squery.publishM2Local

git diff
git commit -am "msg"

$VERSION="0.0.6"
git tag -a $VERSION -m "Improve warnings"
git push origin $VERSION
```

# TODOs

- test more databases
- sql parser reorders OFFSET LIMIT ???
