package com.github.stepancheg.mhlang;

import org.junit.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import static org.junit.Assert.*;

public class ToStringTest {

  private static class MyData {
    private final int i;
    private final String s;

    public MyData(int i, String s) {
      this.i = i;
      this.s = s;
    }

    private static final MethodHandle TO_STRING = ToString.buildToString(MyData.class, MethodHandles.lookup());

    @Override
    public String toString() {
      try {
        return (String) TO_STRING.invokeExact(this);
      } catch (Throwable throwable) {
        throw new RuntimeException(throwable);
      }
    }
  }

  @Test
  public void test() {
    MyData d = new MyData(1, "a");
    assertEquals("MyData{i=1, s=a}", d.toString());
  }
}
