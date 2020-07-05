package com.github.stepancheg.mhlang;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

class MhUtil {

  static final MethodHandle NOP = MethodHandles.zero(void.class);

  static MethodHandle returnParam(Class<?>[] params, int i) {
    MethodHandle mh = MethodHandles.identity(params[i]);
    mh = MethodHandles.dropArguments(mh, 1, Arrays.copyOfRange(params, i + 1, params.length));
    mh = MethodHandles.dropArguments(mh, 0, Arrays.copyOfRange(params, 0, i));
    return mh;
  }

  static MethodHandle returnVoid(Class<?>[] params) {
    return MethodHandles.empty(MethodType.methodType(void.class, params));
  }
}
