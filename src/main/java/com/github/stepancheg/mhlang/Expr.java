package com.github.stepancheg.mhlang;

public abstract class Expr<R> {

  public abstract Class<R> type();

  public abstract Closure<R> asClosure();
}
