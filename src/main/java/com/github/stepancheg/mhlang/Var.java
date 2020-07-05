package com.github.stepancheg.mhlang;

public abstract class Var<T> {
    final long functionId;
    final int varId;
    final int nonVoidVarIndex;

    private Var(long functionId, int varId, int nonVoidVarIndex) {
        this.functionId = functionId;
        this.varId = varId;
      this.nonVoidVarIndex = nonVoidVarIndex;
    }

    public abstract Class<T> type();

    public <R> Closure<R> cast(Class<R> clazz) {
        return Closure.constant(this).cast(clazz);
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
