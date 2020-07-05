package com.github.stepancheg.mhlang.examples;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public abstract class FlatArrayListTestBase {
  @Test
  public void test() {
    List<MyData> l = newArray();
    MyData data = new MyData(10, "a", true, 17);
    l.add(data);

    assertEquals(data, l.get(0));
    assertNotSame(data, l.get(0));
  }

  protected abstract List<MyData> newArray();
}
