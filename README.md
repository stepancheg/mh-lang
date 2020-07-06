# mh-lang

DSL for constructing Java MethodHandles.

Reflection is great, but slow. Single reflection operation can be fast, but when invoking a sequence of operations,
a user has to pay for reflective invocation overhead in each operation, which is:
* type checking of all arguments
* accessibility
* boxing/unboxing

Consider this use case: check that none of the class fields are not `null`. Pseudo code:

```
void checkAllNotNull(T t) {
  for (Field f in T.fields) {
    Object v = f.get(t);
    if (v == null) {
      throw new IllegalStateException();
    }
  }
}
```

`Field.get` is invoked multiple times, each time it performs type check.

Java `MethodHandle`s allow glueing multiple reflective operation together.
Each reflective operation (field get/set, method call, constructor) is a `MethodHandle`,
and `MethodHandles` class contains a lot of combinators to glue all of the operations together.

Type-checking happens only once, when combined `MethodHandle` is constructed, and only once
when glued `MethodHandles` is invoked.

The example above would be written as:

```
MethodHandle checkAllNotNull = ...;

void checkAllNotNull(T t) {
  checkAllNotNull.invokeExact(t);
}
```

The question is how to construct resulting `MethodHandle`. Raw combine operations (e. g. `collectArguments`)
are very hard in my experience, and writing stateful code

* when a computation result need to be used in multiple operations
* with proper order of side effects
* with conditional invocations

is very hard to a regular human.

mh-lang library provides a DSL for writing method handles in a friendly semi-functional style.

The `mh` described above could be written like this:

```
MhBuilder b = new MhBuilder();
Var<?> t = b.addParameter(tClass);
for (Field field : tClass.getDeclaredFields()) {
  Closure<?> fieldExpr = Closure.getField(field, t);
  b.assign(Closure.ifThen(
    fieldExpr.isNull(),
    Closure.throwException(void.class, Closure.supplier(IllegalStateException.class, () -> new IllegalStateException()));
  ));
}
checkAllNotNull = b.buildReturnVoid();
```

## Struct of arrays

Until Java gets proper value types, efficient memory usage is an issue.

One possible way to deal with this issue it to store lists of objects in
[structs of arrays](https://en.wikipedia.org/wiki/AoS_and_SoA):
for each field allocate an array, and store each struct fields in own arrays.

As a demo for this library I have written to implementation of struct of arrays array lists:
* the one with `MethodHandle`s ([FlatArrayMhList](https://github.com/stepancheg/mh-lang/blob/master/src/main/java/com/github/stepancheg/mhlang/examples/FlatArrayMhList.java))
* the one with regular reflection ([FlatArrayReflList](https://github.com/stepancheg/mh-lang/blob/master/src/main/java/com/github/stepancheg/mhlang/examples/FlatArrayReflList.java))

MH version is
[twenty times faster](https://github.com/stepancheg/mh-lang/blob/master/src/test/java/com/github/stepancheg/mhlang/examples/FlatArrayListBenchmark.java)
than reflective version.

## Maven and other feedback

This library is not published in Maven or elsewhere.
Feel free to just copy sources to your project or to you private storage.
If you think that it need to be published on Maven, open an issue in GitHub issue tracker.

Any other feedback? Open an issue in GitHub issue tracker.

Thanks!
