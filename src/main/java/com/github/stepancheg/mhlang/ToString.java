package com.github.stepancheg.mhlang;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ToString {

  private static Closure<?> append(Expr<StringBuilder> sb, Expr<?> value) {
    if (value.type().isPrimitive()) {
      if (value.type() == byte.class && value.type() == short.class) {
        value = value.asClosure().cast(int.class);
      }
    } else {
      if (value.type() != String.class && value.type() != CharSequence.class) {
        value = value.asClosure().cast(Object.class);
      }
    }

    try {
      return Closure.fold(
          MethodHandles.publicLookup()
              .findVirtual(
                  StringBuilder.class,
                  "append",
                  MethodType.methodType(StringBuilder.class, value.type())),
          sb,
          value);
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> MethodHandle buildToString(Class<T> clazz, MethodHandles.Lookup lookup) {
    if (clazz.isPrimitive()) {
      return MhBuilder.shortcut(clazz, Closure::toString);
    } else {
      MhBuilder b = new MhBuilder();
      Var<T> t = b.addParam(clazz);
      Var<StringBuilder> sb =
          b.assign(
              Closure.newInstance(
                  StringBuilder.class, Closure.constant(clazz.getSimpleName() + "{")));
      Field[] declaredFields = clazz.getDeclaredFields();
      for (int i = 0; i < declaredFields.length; i++) {
        Field field = declaredFields[i];

        if ((field.getModifiers() & Modifier.STATIC) != 0) {
          continue;
        }

        String comma = i != 0 ? ", " : "";
        b.assign(append(sb, Closure.constant(comma + field.getName() + "=")));

        Closure<Object> fieldValue = Closure.getField(field, t, lookup);
        b.assign(append(sb, fieldValue));
      }
      b.assign(append(sb, Closure.constant("}")));
      return b.buildReturn(Closure.toString(sb));
    }
  }
}
