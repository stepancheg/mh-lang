package com.github.stepancheg.mhlang;

import com.google.common.base.Preconditions;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

enum PrimitiveType {
  INT(int.class, Integer.class),
  LONG(long.class, Long.class),
  SHORT(short.class, Short.class),
  BYTE(byte.class, Byte.class),
  BOOLEAN(boolean.class, Boolean.class),
  FLOAT(float.class, Float.class),
  DOUBLE(double.class, Double.class),
  CHAR(char.class, Character.class),
  ;

  final Class<?> primitiveType;
  final Class<?> wrapperType;

  final MethodHandle hashCodeMh;

  PrimitiveType(Class<?> primitiveType, Class<?> wrapperType) {
    this.primitiveType = primitiveType;
    this.wrapperType = wrapperType;

    try {
      this.hashCodeMh = MethodHandles.publicLookup().findStatic(wrapperType, "hashCode", MethodType.methodType(int.class, primitiveType));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  @Nullable
  static PrimitiveType forClass(Class<?> clazz) {
    for (PrimitiveType value : values()) {
      if (value.primitiveType == clazz || value.wrapperType == clazz) {
        return value;
      }
    }
    return null;
  }

  static PrimitiveType forPrimitiveClass(Class<?> clazz) {
    Preconditions.checkArgument(clazz.isPrimitive());
    Preconditions.checkArgument(clazz != void.class);
    for (PrimitiveType value : values()) {
      if (value.primitiveType == clazz) {
        return value;
      }
    }
    throw new RuntimeException("unreachable");
  }
}
