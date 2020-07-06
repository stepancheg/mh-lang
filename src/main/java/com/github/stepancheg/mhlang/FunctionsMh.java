package com.github.stepancheg.mhlang;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Comparator;
import java.util.function.*;

class FunctionsMh {
  private static final MethodHandle FUNCTION;
  private static final MethodHandle BI_FUNCTION;
  private static final MethodHandle BI_CONSUMER;
  private static final MethodHandle PREDICATE;
  private static final MethodHandle BI_PREDICATE;
  private static final MethodHandle COMPARATOR;
  private static final MethodHandle SUPPLIER;
  private static final MethodHandle RUNNABLE;
  private static final MethodHandle INT_UNARY_OPERATOR;
  private static final MethodHandle INT_PREDICATE;
  private static final MethodHandle TO_INT_FUNCTION;

  static {
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    try {
      FUNCTION =
          lookup.findVirtual(
              Function.class, "apply", MethodType.methodType(Object.class, Object.class));
      BI_FUNCTION =
          lookup.findVirtual(
              BiFunction.class,
              "apply",
              MethodType.methodType(Object.class, Object.class, Object.class));
      BI_CONSUMER =
        lookup.findVirtual(
          BiConsumer.class,
          "accept", MethodType.methodType(void.class, Object.class, Object.class));
      PREDICATE =
          lookup.findVirtual(
              Predicate.class, "test", MethodType.methodType(boolean.class, Object.class));
      BI_PREDICATE =
          lookup.findVirtual(
              BiPredicate.class,
              "test",
              MethodType.methodType(boolean.class, Object.class, Object.class));
      COMPARATOR =
        lookup.findVirtual(
          Comparator.class,
          "compare", MethodType.methodType(int.class, Object.class, Object.class));
      SUPPLIER = lookup.findVirtual(Supplier.class, "get", MethodType.methodType(Object.class));
      RUNNABLE = lookup.findVirtual(Runnable.class, "run", MethodType.methodType(void.class));
      INT_UNARY_OPERATOR =
          lookup.findVirtual(
              IntUnaryOperator.class, "applyAsInt", MethodType.methodType(int.class, int.class));
      INT_PREDICATE =
          lookup.findVirtual(
              IntPredicate.class, "test", MethodType.methodType(boolean.class, int.class));
      TO_INT_FUNCTION =
          lookup.findVirtual(
              ToIntFunction.class, "applyAsInt", MethodType.methodType(int.class, Object.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  static MethodHandle function(Function<?, ?> f) {
    return MethodHandles.insertArguments(FUNCTION, 0, f);
  }

  static MethodHandle biFunction(BiFunction<?, ?, ?> f) {
    return MethodHandles.insertArguments(BI_FUNCTION, 0, f);
  }

  static MethodHandle biConsumer(BiConsumer<?, ?> f) {
    return MethodHandles.insertArguments(BI_CONSUMER, 0, f);
  }

  static MethodHandle predicate(Predicate<?> f) {
    return MethodHandles.insertArguments(PREDICATE, 0, f);
  }

  static MethodHandle biPredicate(BiPredicate<?, ?> f) {
    return MethodHandles.insertArguments(BI_PREDICATE, 0, f);
  }

  static MethodHandle comparator(Comparator<?> f) {
    return MethodHandles.insertArguments(COMPARATOR, 0, f);
  }

  static MethodHandle supplierGet(Supplier<?> supplier) {
    return MethodHandles.insertArguments(SUPPLIER, 0, supplier);
  }

  static MethodHandle runnableRun(Runnable runnable) {
    return MethodHandles.insertArguments(RUNNABLE, 0, runnable);
  }

  static MethodHandle intUnaryOperator(IntUnaryOperator op) {
    return MethodHandles.insertArguments(INT_UNARY_OPERATOR, 0, op);
  }

  static MethodHandle intPredicate(IntPredicate p) {
    return MethodHandles.insertArguments(INT_PREDICATE, 0, p);
  }

  static MethodHandle toIntFunction(ToIntFunction<?> f) {
    return MethodHandles.insertArguments(TO_INT_FUNCTION, 0, f);
  }
}
