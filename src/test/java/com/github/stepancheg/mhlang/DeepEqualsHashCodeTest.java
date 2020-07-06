package com.github.stepancheg.mhlang;

import org.junit.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import static org.junit.Assert.assertFalse;

public class DeepEqualsHashCodeTest {

  private static class Data {
    private final int i;
    private final String s;

    public Data(int i, String s) {
      this.i = i;
      this.s = s;
    }

    private static final MethodHandle EQUALS = DeepEqualsHashCode.deepEquals(Data.class, MethodHandles.lookup());
    private static final MethodHandle HASH_CODE = DeepEqualsHashCode.deepHashCode(Data.class, MethodHandles.lookup());

    @Override
    public boolean equals(Object obj) {
      try {
        return (boolean) EQUALS.invokeExact(this, obj);
      } catch (Throwable throwable) {
        throw new RuntimeException(throwable);
      }
    }

    @Override
    public int hashCode() {
      try {
        return (int) HASH_CODE.invokeExact(this);
      } catch (Throwable throwable) {
        throw new RuntimeException(throwable);
      }
    }
  }

  @Test
  public void testEquals() {
    assertFalse(new Data(1, "a").equals(null));
  }
}
