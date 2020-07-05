package com.github.stepancheg.mhlang;

import com.google.common.collect.ImmutableList;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Stream;

public class EqualsHashCode {
  private static final MethodHandle GET_CLASS;

  static {
    try {
      GET_CLASS = MethodHandles.lookup().findVirtual(Object.class, "getClass", MethodType.methodType(Class.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }


  public static <T> MethodHandle buildEquals(Class<T> clazz, MethodHandles.Lookup lookup) {
    MhBuilder b = new MhBuilder();
    Var<T> thiz = b.addParam(clazz);
    Var<Object> that = b.addParam(Object.class);

    ClosureBuilder allFieldsEqB = new ClosureBuilder();
    Closure<T> thatDowncasted = that.cast(clazz);
    Closure<Boolean> allFieldsEq = allFieldsEqB.buildReturn(Closure.and(Arrays.stream(clazz.getDeclaredFields()).flatMap(f -> {
      if ((f.getModifiers() & Modifier.STATIC) != 0) {
        return Stream.empty();
      }

      Closure<Object> thisField = Closure.getField(f, thiz, lookup);
      Closure<Object> thatField = Closure.getField(f, thatDowncasted, lookup);

      return Stream.of(Closure.equals(thisField, thatField));
    }).collect(ImmutableList.toImmutableList())));

    return b.buildReturn(Closure.and(
      that.asClosure().isNotNull(),
      Closure.same(Closure.constant(clazz), Closure.fold(GET_CLASS, that)),
      allFieldsEq
      ));
  }

  public static <T> MethodHandle buildHashCode(Class<T> clazz, MethodHandles.Lookup lookup) {
    MhBuilder b = new MhBuilder();
    Var<T> thiz = b.addParam(clazz);
    Closure<Integer> hash = Closure.constant(0);
    for (Field field : clazz.getDeclaredFields()) {
      if ((field.getModifiers() & Modifier.STATIC) != 0) {
        continue;
      }

      Closure<Object> thisField = Closure.getField(field, thiz, lookup);
      Closure<Integer> fieldHash = Closure.hashCode(thisField);

      hash = Closure.mul(hash, Closure.constant(31));
      hash = Closure.plus(hash, fieldHash);
    }
    return b.buildReturn(hash);
  }
}
