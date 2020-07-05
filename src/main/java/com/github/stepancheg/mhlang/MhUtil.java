package com.github.stepancheg.mhlang;

import com.google.common.base.Preconditions;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Objects;

class MhUtil {

  static MethodHandle returnParam(Class<?>[] params, int i) {
    MethodHandle mh = MethodHandles.identity(params[i]);
    mh = MethodHandles.dropArguments(mh, 1, Arrays.copyOfRange(params, i + 1, params.length));
    mh = MethodHandles.dropArguments(mh, 0, Arrays.copyOfRange(params, 0, i));
    return mh;
  }

  static MethodHandle returnVoid(Class<?>[] params) {
    return MethodHandles.empty(MethodType.methodType(void.class, params));
  }

  private static int plus(int a, int b) {
    return a + b;
  }

  private static long plus(long a, long b) {
    return a + b;
  }

  private static int mul(int a, int b) {
    return a * b;
  }

  private static long mul(long a, long b) {
    return a * b;
  }

  static MethodHandle plus(Class<?> argType) {
    try {
      return MethodHandles.lookup().findStatic(MhUtil.class, "plus", MethodType.methodType(argType, argType, argType));
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException("+ is not implemented for " + argType);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  static MethodHandle mul(Class<?> argType) {
    try {
      return MethodHandles.lookup().findStatic(MhUtil.class, "mul", MethodType.methodType(argType, argType, argType));
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException("* is not implemented for " + argType);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private static boolean isNotNull(Object o) {
    return o != null;
  }

  private static boolean not(boolean b) {
    return !b;
  }

  static final MethodHandle IS_NOT_NULL;
  static final MethodHandle NOT;

  static {
    try {
      IS_NOT_NULL = MethodHandles.lookup().findStatic(MhUtil.class, "isNotNull", MethodType.methodType(boolean.class, Object.class));
      NOT = MethodHandles.lookup().findStatic(MhUtil.class, "not", MethodType.methodType(boolean.class, boolean.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private static boolean eq(byte a, byte b) {
    return a == b;
  }

  private static boolean eq(int a, int b) {
    return a == b;
  }

  private static boolean eq(short a, short b) {
    return a == b;
  }

  private static boolean eq(long a, long b) {
    return a == b;
  }

  private static boolean eq(float a, float b) {
    return a == b;
  }

  private static boolean eq(double a, double b) {
    return a == b;
  }

  private static boolean eq(char a, char b) {
    return a == b;
  }

  private static boolean same(Object a, Object b) {
    return a == b;
  }

  private static boolean eq(Object a, Object b) {
    if (a == null || b == null || a == b) {
      return a == b;
    } else {
      return a.equals(b);
    }
  }

  static MethodHandle eq(Class<?> type) {
    Class<?> pType;
    if (!type.isPrimitive()) {
      pType = Object.class;
    } else {
      pType = type;
    }
    MethodHandle mh;
    try {
      mh = MethodHandles.lookup().findStatic(MhUtil.class, "eq", MethodType.methodType(boolean.class, pType, pType));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    if (!type.isPrimitive()) {
      mh = MethodHandles.explicitCastArguments(mh, MethodType.methodType(boolean.class, type, type));
    }
    return mh;
  }

  private static final MethodHandle SAME;

  static {
    try {
      SAME = MethodHandles.lookup().findStatic(MhUtil.class, "same", MethodType.methodType(boolean.class, Object.class, Object.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  static MethodHandle same(Class<?> type) {
    if (type.isPrimitive()) {
      return eq(type);
    } else {
      return MethodHandles.explicitCastArguments(SAME, MethodType.methodType(boolean.class, type, type));
    }
  }

  private static final MethodHandle OBJECTS_HASH_CODE;

  static {
    try {
      OBJECTS_HASH_CODE = MethodHandles.publicLookup().findStatic(Objects.class, "hashCode", MethodType.methodType(int.class, Object.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  static MethodHandle hashCode(Class<?> type) {
    Preconditions.checkArgument(type != void.class);
    if (type.isPrimitive()) {
      return PrimitiveType.forPrimitiveClass(type).hashCodeMh;
    } else {
      return MethodHandles.explicitCastArguments(OBJECTS_HASH_CODE, MethodType.methodType(int.class, type));
    }
  }
}
