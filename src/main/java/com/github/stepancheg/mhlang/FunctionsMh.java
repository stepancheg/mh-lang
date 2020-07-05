package com.github.stepancheg.mhlang;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.*;

class FunctionsMh {
    private static final MethodHandle FUNCTION_APPLY;
    private static final MethodHandle BI_FUNCTION_APPLY;
    private static final MethodHandle PREDICATE_TEST;
    private static final MethodHandle BI_PREDICATE_TEST;
    private static final MethodHandle SUPPLIER_GET;
    private static final MethodHandle RUNNABLE_RUN;
    private static final MethodHandle INT_UNARY_OPERATOR;
    private static final MethodHandle INT_PREDICATE;

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            FUNCTION_APPLY = lookup.findVirtual(Function.class, "apply", MethodType.methodType(Object.class, Object.class));
            BI_FUNCTION_APPLY = lookup.findVirtual(BiFunction.class, "apply", MethodType.methodType(Object.class, Object.class, Object.class));
            PREDICATE_TEST = lookup.findVirtual(Predicate.class, "test", MethodType.methodType(boolean.class, Object.class));
            BI_PREDICATE_TEST = lookup.findVirtual(BiPredicate.class, "test", MethodType.methodType(boolean.class, Object.class, Object.class));
            SUPPLIER_GET = lookup.findVirtual(Supplier.class, "get", MethodType.methodType(Object.class));
            RUNNABLE_RUN = lookup.findVirtual(Runnable.class, "run", MethodType.methodType(void.class));
            INT_UNARY_OPERATOR = lookup.findVirtual(IntUnaryOperator.class, "applyAsInt", MethodType.methodType(int.class, int.class));
            INT_PREDICATE = lookup.findVirtual(IntPredicate.class, "test", MethodType.methodType(boolean.class, int.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static MethodHandle functionApply(Function<?, ?> f) {
        return MethodHandles.insertArguments(FUNCTION_APPLY, 0, f);
    }

    static MethodHandle biFunctionApply(BiFunction<?, ?, ?> f) {
        return MethodHandles.insertArguments(BI_FUNCTION_APPLY, 0, f);
    }

    static MethodHandle predicateTest(Predicate<?> f) {
        return MethodHandles.insertArguments(PREDICATE_TEST, 0, f);
    }

    static MethodHandle biPredicateTest(BiPredicate<?, ?> f) {
        return MethodHandles.insertArguments(BI_PREDICATE_TEST, 0, f);
    }

    static MethodHandle supplierGet(Supplier<?> supplier) {
        return MethodHandles.insertArguments(SUPPLIER_GET, 0, supplier);
    }

    static MethodHandle runnableRun(Runnable runnable) {
        return MethodHandles.insertArguments(RUNNABLE_RUN, 0, runnable);
    }

    static MethodHandle intUnaryOperator(IntUnaryOperator op) {
      return MethodHandles.insertArguments(INT_UNARY_OPERATOR, 0, op);
    }

    static MethodHandle intPredicate(IntPredicate p) {
      return MethodHandles.insertArguments(INT_PREDICATE, 0, p);
    }
}
