package com.github.stepancheg.mhlang;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class SigUnifier {

  final ImmutableList<Var<?>> allVars;
  private final ImmutableMap<Var<?>, Integer> varToIndex;

  @SafeVarargs
  SigUnifier(ImmutableList<Var<?>>... varss) {
    allVars =
        Arrays.stream(varss)
            .flatMap(Collection::stream)
            .distinct()
            .collect(ImmutableList.toImmutableList());

    varToIndex = CollectionUtil.index(allVars);
  }

  <R> Closure<R> unify(Closure<R> closure) {
    return unifyWithoutFirst(closure, 0);
  }

  <R> Closure<R> unifyWithoutFirst(Closure<R> closure, int count) {
    int[] reorder =
        IntStream.range(0, closure.args.size())
            .map(
                i -> {
                  if (i < count) {
                    return i;
                  } else {
                    return varToIndex.get(closure.args.get(i)) + count;
                  }
                })
            .toArray();

    MethodHandle permuted =
        MethodHandles.permuteArguments(
            closure.mh,
            MethodType.methodType(
                    closure.returnType(), allVars.stream().map(Var::type).toArray(Class<?>[]::new))
                .insertParameterTypes(0, Arrays.copyOf(closure.mh.type().parameterArray(), count)),
            reorder);

    return new Closure<>(
        permuted,
        Stream.concat(closure.args.subList(0, count).stream(), allVars.stream())
            .toArray(Var[]::new));
  }
}
