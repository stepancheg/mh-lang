package com.github.stepancheg.mhlang.examples;

import com.github.stepancheg.mhlang.Builder;
import com.github.stepancheg.mhlang.Var;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class AutoEqualsHashCode {

  private static class Data {
    private final int i;
    private final String s;

    public Data(int i, String s) {
      this.i = i;
      this.s = s;
    }

    private static final MethodHandle EQUALS = buildEquals(Data.class);
    private static final MethodHandle HASH_CODE = buildHashCode(Data.class);

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

  private static <T> MethodHandle buildEquals(Class<T> clazz) {
    Builder b = new Builder();
    Var<T> thiz = b.addParam(clazz);
    Var<Object> that = b.addParam(Object.class);
    for (Field field : clazz.getDeclaredFields()) {
      if ((field.getModifiers() & Modifier.STATIC) != 0) {
        continue;
      }
    }
    throw new RuntimeException();
  }

  private static <T> MethodHandle buildHashCode(Class<T> clazz) {
    Builder b = new Builder();
    for (Field field : clazz.getDeclaredFields()) {
      if ((field.getModifiers() & Modifier.STATIC) != 0) {
        continue;
      }
    }
    throw new RuntimeException();
  }

}
