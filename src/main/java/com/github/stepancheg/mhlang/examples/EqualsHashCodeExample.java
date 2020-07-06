package com.github.stepancheg.mhlang.examples;

import com.github.stepancheg.mhlang.EqualsHashCode;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * {@link com.github.stepancheg.mhlang.EqualsHashCode} contains an implementation of deep equals and
 * hash code.
 *
 * <p>This example uses these utilities.
 */
public class EqualsHashCodeExample {

  private static class MyData {
    private final int i;
    private final String s;
    private final boolean b;

    public MyData(int i, String s, boolean b) {
      this.i = i;
      this.s = s;
      this.b = b;
    }

    private static final MethodHandle EQUALS =
        EqualsHashCode.buildEquals(MyData.class, MethodHandles.lookup());
    private static final MethodHandle HASH_CODE =
        EqualsHashCode.buildHashCode(MyData.class, MethodHandles.lookup());

    @Override
    public int hashCode() {
      try {
        return (int) HASH_CODE.invokeExact(this);
      } catch (Throwable throwable) {
        throw new RuntimeException(throwable);
      }
    }

    @Override
    public boolean equals(Object obj) {
      try {
        return (boolean) EQUALS.invokeExact(this, obj);
      } catch (Throwable throwable) {
        throw new RuntimeException(throwable);
      }
    }
  }

  public static void main(String[] args) {
    MyData d = new MyData(1, "a", true);
    System.out.println(d.hashCode());
    System.out.println(d.equals(new MyData(1, "a", true)));
    System.out.println(d.equals(new MyData(1, "a", false)));
  }
}
