package com.github.stepancheg.mhlang;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

class ClassUtil {

  static Field[] nonStaticDeclaredFields(Class<?> clazz) {
    return Arrays.stream(clazz.getDeclaredFields()).filter(ClassUtil::isNotStatic).toArray(Field[]::new);
  }

  static boolean isNotStatic(Field f) {
    return (f.getModifiers() & Modifier.STATIC) == 0;
  }

}
