package com.github.stepancheg.mhlang;

import com.google.common.collect.ImmutableMap;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

class CollectionUtil {

  static <A> ImmutableMap<A, Integer> index(List<A> items) {
    return IntStream.range(0, items.size())
        .mapToObj(i -> new AbstractMap.SimpleEntry<>(items.get(i), i))
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @SafeVarargs
  static <A> ImmutableMap<A, Integer> index(A... items) {
    return index(Arrays.asList(items));
  }
}
