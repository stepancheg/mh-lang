package com.github.stepancheg.mhlang;

import com.google.common.base.Preconditions;

import java.lang.reflect.Array;
import java.util.Arrays;

/** Array utilities. */
class ArrayUtil {

  /** Concatenate arrays. */
  @SuppressWarnings("unchecked")
  @SafeVarargs
  static <A> A[] concat(A[]... as) {
    Preconditions.checkArgument(as.length != 0);
    int len = Arrays.stream(as).mapToInt(a -> a.length).sum();
    A[] r = (A[]) Array.newInstance(as[0].getClass().getComponentType(), len);
    int i = 0;
    for (A[] a : as) {
      System.arraycopy(a, 0, r, i, a.length);
      i += a.length;
    }
    // Self-check
    Preconditions.checkState(i == len);
    return r;
  }
}
