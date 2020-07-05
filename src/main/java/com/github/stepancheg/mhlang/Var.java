package com.github.stepancheg.mhlang;

public abstract class Var<R> {
  final long functionId;
  final int varId;
  final int nonVoidVarIndex;

  private Var(long functionId, int varId, int nonVoidVarIndex) {
    this.functionId = functionId;
    this.varId = varId;
    this.nonVoidVarIndex = nonVoidVarIndex;
  }

  public abstract Class<R> type();

  public Closure<R> asClosure() {
    return Closure.var(this);
  }

  public <S> Closure<S> cast(Class<S> clazz) {
    return asClosure().cast(clazz);
  }

  static class Param<T> extends Var<T> {
    private final Class<T> type;

    Param(long functionId, int varId, int nonVoidVarIndex, Class<T> type) {
      super(functionId, varId, nonVoidVarIndex);
      this.type = type;
    }

    @Override
    public Class<T> type() {
      return type;
    }

    @Override
    public String toString() {
      return "p0: " + type;
    }
  }

  static class Invoke<R> extends Var<R> {
    final Closure<R> closure;

    Invoke(long functionId, int varId, int nonVoidVarIndex, Closure<R> closure) {
      super(functionId, varId, nonVoidVarIndex);

      this.closure = closure;
    }

    @Override
    public Class<R> type() {
      return closure.returnType();
    }
  }
}
