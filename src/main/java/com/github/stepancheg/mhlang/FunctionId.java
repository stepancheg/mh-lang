package com.github.stepancheg.mhlang;

import java.util.concurrent.atomic.AtomicLong;

class FunctionId {
  private static final AtomicLong lastFunctionId = new AtomicLong();

  static long nextId() {
    return lastFunctionId.addAndGet(1);
  }
}
