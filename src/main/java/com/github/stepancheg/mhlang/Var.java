package com.github.stepancheg.mhlang;

public abstract class Var<R> extends Expr<R> {
  final long functionId;

  private Var(long functionId) {
    this.functionId = functionId;
  }

  @Override
  public abstract Class<R> type();

  @Override
  public Closure<R> asClosure() {
    return Closure.var(this);
  }

  public <S> Closure<S> cast(Class<S> clazz) {
    return asClosure().cast(clazz);
  }

  static class Param<T> extends Var<T> {
    private final int varId;
    private final Class<T> type;

    Param(long functionId, int varId, Class<T> type) {
      super(functionId);
      this.varId = varId;
      this.type = type;
    }

    @Override
    public Class<T> type() {
      return type;
    }

    @Override
    public String toString() {
      return String.format("p%s: %s", varId, type.getSimpleName());
    }
  }

  static class Invoke<R> extends Var<R> {
    private final int varId;
    final Closure<R> closure;

    Invoke(long functionId, int varId, Closure<R> closure) {
      super(functionId);
      this.varId = varId;

      this.closure = closure;
    }

    @Override
    public Class<R> type() {
      return closure.type();
    }

    @Override
    public String toString() {
      return String.format("v%d: %s", varId, type().getSimpleName());
    }
  }
}
