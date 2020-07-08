package com.github.stepancheg.mhlang;

import org.junit.Assert;
import org.junit.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

public class DeepCompareTest {

  private static class MyData implements Comparable<MyData> {
    private final int i;
    private final String s;
    private final long l;

    public MyData(int i, String s, long l) {
      this.i = i;
      this.s = s;
      this.l = l;
    }

    private static final MethodHandle COMPARE_TO =
        DeepCompare.deepCompare(MyData.class, MethodHandles.lookup());

    @Override
    public int compareTo(MyData o) {
      try {
        return (int) COMPARE_TO.invokeExact(this, o);
      } catch (Throwable throwable) {
        throw new RuntimeException(throwable);
      }
    }
  }

  @Test
  public void test() {
    Assert.assertTrue(new MyData(10, "a", 20).compareTo(new MyData(10, "a", 20)) == 0);

    Assert.assertTrue(new MyData(10, "a", 20).compareTo(new MyData(11, "a", 20)) < 0);
    Assert.assertTrue(new MyData(11, "a", 20).compareTo(new MyData(10, "a", 20)) > 0);

    Assert.assertTrue(new MyData(10, "asd", 20).compareTo(new MyData(11, "d", 20)) < 0);
    Assert.assertTrue(new MyData(11, "g", 20).compareTo(new MyData(10, "b", 20)) > 0);

    Assert.assertTrue(new MyData(10, "a", 20).compareTo(new MyData(10, "b", 20)) < 0);
    Assert.assertTrue(new MyData(10, "b", 20).compareTo(new MyData(10, "a", 13)) > 0);
  }
}
