package com.github.stepancheg.mhlang.examples;

import com.github.stepancheg.mhlang.EqualsHashCode;
import com.github.stepancheg.mhlang.ToString;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * Example of hash code, equals, toString generators.
 *
 * <p>{@link EqualsHashCode} contains an implementation of deep equals and hash code. {@link
 * ToString} contains an implementation of deep equals.
 *
 * <p>This example uses these utilities.
 */
public class EqualsHashCodeToStringExample {

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
    private static final MethodHandle TO_STRING =
        ToString.buildToString(MyData.class, MethodHandles.lookup());

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

    @Override
    public String toString() {
      try {
        return (String) TO_STRING.invokeExact(this);
      } catch (Throwable throwable) {
        throw new RuntimeException(throwable);
      }
    }
  }

  public static void main(String[] args) {
    MyData d = new MyData(1, "a", true);
    System.out.println(d);
    System.out.println(d.hashCode());
    System.out.println(d.equals(new MyData(1, "a", true)));
    System.out.println(d.equals(new MyData(1, "a", false)));
  }
}
