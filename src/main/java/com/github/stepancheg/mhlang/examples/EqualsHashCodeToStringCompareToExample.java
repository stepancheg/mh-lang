package com.github.stepancheg.mhlang.examples;

import com.github.stepancheg.mhlang.DeepCompare;
import com.github.stepancheg.mhlang.DeepEqualsHashCode;
import com.github.stepancheg.mhlang.DeepToString;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * Example of hash code, equals, toString generators.
 *
 * <p>{@link DeepEqualsHashCode} contains an implementation of deep equals and hash code. {@link
 * DeepToString} contains an implementation of deep equals.
 *
 * <p>This example uses these utilities.
 */
public class EqualsHashCodeToStringCompareToExample {

  private static class MyData implements Comparable<MyData> {
    private final int i;
    private final String s;
    private final boolean b;

    public MyData(int i, String s, boolean b) {
      this.i = i;
      this.s = s;
      this.b = b;
    }

    private static final MethodHandle EQUALS =
        DeepEqualsHashCode.deepEquals(MyData.class, MethodHandles.lookup());
    private static final MethodHandle HASH_CODE =
        DeepEqualsHashCode.deepHashCode(MyData.class, MethodHandles.lookup());
    private static final MethodHandle TO_STRING =
        DeepToString.buildToString(MyData.class, MethodHandles.lookup());
    private static final MethodHandle COMPARE_TO =
        DeepCompare.deepCompare(MyData.class, MethodHandles.lookup());

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

    @Override
    public int compareTo(MyData o) {
      try {
        return (int) COMPARE_TO.invokeExact(this, o);
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
    System.out.println(d.compareTo(new MyData(1, "b", false)));
  }
}
