package com.github.stepancheg.mhlang.examples;

import com.github.stepancheg.mhlang.Closure;
import com.github.stepancheg.mhlang.MhBuilder;
import com.github.stepancheg.mhlang.Var;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/** Example, how to reflectively compute a sum of fields. */
public class SumFields {

  private static class Data {
    int i;
    int j;
    int k;
    int l;
    int m;
    int n;
    int o;
    int p;

    private static final MethodHandle SUM = sumMh();

    public int sum() {
      try {
        return (int) SUM.invokeExact(this);
      } catch (Throwable throwable) {
        throw new RuntimeException(throwable);
      }
    }
  }

  private static int plus(int a, int b) {
    return a + b;
  }

  /** Construct a {@link java.lang.invoke.MethodHandle} which computes a sum of all fields. */
  private static MethodHandle sumMh() {
    // To access private fields.
    MethodHandles.Lookup lookup = MethodHandles.lookup();

    MethodHandle plus;
    try {
      plus =
          lookup.findStatic(
              SumFields.class, "plus", MethodType.methodType(int.class, int.class, int.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }

    MhBuilder b = new MhBuilder();
    // Declare a parameter, which is `Data`.
    Var<Data> data = b.addParam(Data.class);

    Closure<Integer> sum = Closure.constant(0);
    for (Field field : Data.class.getDeclaredFields()) {
      // Skip static fields
      if ((field.getModifiers() & Modifier.STATIC) != 0) {
        continue;
      }

      Var<Integer> fieldValue = b.assign(Closure.getField(field, data, lookup));
      sum = Closure.fold(plus, sum, fieldValue);
    }
    return b.buildReturn(sum);
  }

  public static void main(String[] args) {
    Data data = new Data();
    data.i = 1;
    data.j = 2;
    data.k = 2;
    data.l = 3;
    data.m = 3;
    data.n = 3;
    data.o = 3;
    data.p = 3;
    System.out.println(data.sum());
  }
}
