package com.github.stepancheg.mhlang.examples;

public class FlatArrayReflListTest extends FlatArrayListTestBase {

  @Override
  protected FlatArrayReflList<MyData> newArray() {
    return new FlatArrayReflList.Factory<>(MyData.class).newArrayList();
  }
}
