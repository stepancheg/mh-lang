package com.github.stepancheg.mhlang.examples;

import java.util.List;

public class FlatArrayListBenchmark {

  private static volatile Object o;

  private static void run(String name, List<MyData> list) {
    long start = System.currentTimeMillis();
    for (int j = 0; j != 1000; ++j) {
      for (int i = 0; i != 10000; ++i) {
        list.add(new MyData(i, "", true, i + 1));
      }
      o = list;
    }
    System.out.printf("%-4s %4d%n", name, System.currentTimeMillis() - start);
  }

  private static final FlatArrayMhList.Factory<MyData> mhFactory = new FlatArrayMhList.Factory<>(MyData.class);
  private static final FlatArrayReflList.Factory<MyData> reflFactory = new FlatArrayReflList.Factory<>(MyData.class);

  private static void iter() {
    run("mh", mhFactory.newArrayList());
    run("refl", reflFactory.newArrayList());
    run("refl", reflFactory.newArrayList());
    run("mh", mhFactory.newArrayList());
    run("mh", mhFactory.newArrayList());
    run("refl", reflFactory.newArrayList());
  }

  public static void main(String[] args) {
    for (;;) {
      iter();
    }
  }
}
