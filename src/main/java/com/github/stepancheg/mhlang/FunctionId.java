package com.github.stepancheg.mhlang;

import java.util.concurrent.atomic.AtomicLong;

/** Each variable belongs to some function. This utility generates identifiers for functions. */
class FunctionId {
  private static final AtomicLong lastFunctionId = new AtomicLong();

  /** Generate next unique function id. */
  static long nextId() {
    return lastFunctionId.addAndGet(1);
  }
}
