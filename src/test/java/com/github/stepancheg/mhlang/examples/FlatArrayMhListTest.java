package com.github.stepancheg.mhlang.examples;

public class FlatArrayMhListTest extends FlatArrayListTestBase {

  @Override
  protected FlatArrayMhList<MyData> newArray() {
    return new FlatArrayMhList.Factory<>(MyData.class).newArrayList();
  }
}
