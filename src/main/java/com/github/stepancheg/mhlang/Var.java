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

        @Override
        public <R> R match(Matcher<R> matcher) {
            return matcher.param(varId);
        }
    }

    static class Invoke<T> extends Var<T> {
        private final Closure<T> closure;

        Invoke(long functionId, int varId, int nonVoidVarIndex, Closure<T> closure) {
            super(functionId, varId, nonVoidVarIndex);

            this.closure = closure;
        }

        @Override
        public Class<T> type() {
            return closure.returnType();
        }

        @Override
        public <R> R match(Matcher<R> matcher) {
            return matcher.invoke(closure);
        }
    }

    interface Matcher<R> {
        R param(int i);
        R invoke(Closure<?> closure);
    }

    public abstract <R> R match(Matcher<R> matcher);
}
