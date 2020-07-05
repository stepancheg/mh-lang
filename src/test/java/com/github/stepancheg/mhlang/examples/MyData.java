package com.github.stepancheg.mhlang.examples;

import java.util.Objects;

class MyData {
  private final int i;
  private final String s;
  private final boolean b;
  private final long l;

  MyData(int i, String s, boolean b, long l) {
    this.i = i;
    this.s = s;
    this.b = b;
    this.l = l;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MyData myData = (MyData) o;
    return i == myData.i &&
      b == myData.b &&
      l == myData.l &&
      Objects.equals(s, myData.s);
  }

  @Override
  public int hashCode() {
    return Objects.hash(i, s, b, l);
  }
}
