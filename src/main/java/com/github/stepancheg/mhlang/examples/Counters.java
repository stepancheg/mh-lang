package com.github.stepancheg.mhlang.examples;

import com.github.stepancheg.mhlang.Closure;
import com.github.stepancheg.mhlang.MhBuilder;
import com.github.stepancheg.mhlang.Var;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class Counters {

  private static class MyCounter {
    long bytes;
    int users;
    int connections;
    int errors;

    private static final MethodHandle UPDATER = buildAdd(MyCounter.class);

    /** Add countes from given object to this counters. */
    public void add(MyCounter delta) {
      try {
        UPDATER.invokeExact(this, delta);
      } catch (Throwable throwable) {
        throw new RuntimeException(throwable);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> MethodHandle buildAdd(Class<T> clazz) {
    MhBuilder b = new MhBuilder();
    Var<T> t = b.addParam(clazz);
    Var<T> delta = b.addParam(clazz);

    for (Field field : clazz.getDeclaredFields()) {
      if ((field.getModifiers() & Modifier.STATIC) != 0) {
        continue;
      }

      field.setAccessible(true);

      // t.f = t.f + delta.f
      Closure<?> oldValue = Closure.getField(field, t);
      Closure<?> addValue = Closure.getField(field, delta);
      Closure<?> sum = Closure.plus((Closure<Object>) oldValue, (Closure<Object>) addValue);
      b.assign(Closure.setField(field, t, sum));
    }

    return b.buildReturnVoid();
  }

  public static void main(String[] args) {
    MyCounter total = new MyCounter();

    MyCounter delta1 = new MyCounter();
    delta1.bytes = 100;
    delta1.connections = 2;
    delta1.errors = 0;
    delta1.users = 3;
    total.add(delta1);

    MyCounter delta2 = new MyCounter();
    delta2.bytes = 200;
    delta2.connections = 4;
    delta2.errors = 1;
    delta2.users = 0;
    total.add(delta2);

    System.out.println(
        total.bytes + " " + total.users + " " + total.connections + " " + total.errors);
  }
}
