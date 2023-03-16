


```sh


./mill clean

./mill __.reformat

./mill squery.__.test

./mill examples.runMain bla

# for local dev/test
./mill  squery.publishM2Local

git diff
git commit -am "msg"

git tag -a 0.5.1 -m "Fix stuff"
git push origin 0.5.1
```

# TODOs

- sql parser reorder OFFSET LIMIT ???
