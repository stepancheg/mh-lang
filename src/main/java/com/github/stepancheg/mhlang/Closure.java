package com.github.stepancheg.mhlang;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.*;

public class Closure<R> {

  final MethodHandle mh;
  final ImmutableList<Var<?>> args;

  public Closure(MethodHandle mh, ImmutableList<Var<?>> args) {
    if (mh.type().parameterCount() != args.size()) {
      throw new IllegalArgumentException();
    }
    for (int i = 0; i != args.size(); ++i) {
      if (!mh.type().parameterType(i).equals(args.get(i).type())) {
        throw new IllegalArgumentException();
      }
    }

    this.mh = mh;
    this.args = args;
  }

  public Closure(MethodHandle mh, Var<?>... args) {
    this(mh, ImmutableList.copyOf(args));
  }

  public MethodType methodType() {
    return mh.type();
  }

  @SuppressWarnings("unchecked")
  public Class<R> returnType() {
    return (Class<R>) mh.type().returnType();
  }

  @SuppressWarnings("unchecked")
  public <R> Closure<R> cast(Class<R> clazz) {
    if (returnType() == clazz) {
      return (Closure<R>) this;
    } else {
      return new Closure<>(
          MethodHandles.explicitCastArguments(
              mh, MethodType.methodType(clazz, mh.type().parameterArray())),
          args);
    }
  }

  public static <R> Closure<R> constant(Var<R> v) {
    return new Closure<>(MethodHandles.identity(v.type()), ImmutableList.of(v));
  }

  public static Closure<Void> constantVoid() {
    return new Closure<>(MhUtil.NOP);
  }

  public static <R> Closure<R> method(Method method, Var<?>... args) {
    try {
      MethodHandle mh = MethodHandles.publicLookup().unreflect(method);
      return new Closure<R>(mh, args);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  public static <R> Closure<R> constructor(Constructor<R> constructor, Var<?>... args) {
    try {
      MethodHandle mh = MethodHandles.publicLookup().unreflectConstructor(constructor);
      return new Closure<>(mh, args);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  public static <R> Closure<R> getField(Field field, Var<?> object) {
    try {
      MethodHandle mh = MethodHandles.publicLookup().unreflectGetter(field);
      return new Closure<>(mh, object);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  public static Closure<Void> setField(Field field, Var<?> object, Var<?> value) {
    try {
      MethodHandle mh = MethodHandles.publicLookup().unreflectSetter(field);
      return new Closure<>(mh, object);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  public static <A, R> Closure<R> function(Class<R> r, Var<A> a, Function<A, R> f) {
    MethodHandle mh = FunctionsMh.functionApply(f);
    mh = MethodHandles.explicitCastArguments(mh, MethodType.methodType(r, a.type()));
    return new Closure<R>(mh, a);
  }

  public static <A, B, R> Closure<R> biFunction(
      Class<R> r, Var<A> a, Var<B> b, BiFunction<A, B, R> function) {
    MethodHandle mh = FunctionsMh.biFunctionApply(function);
    mh = MethodHandles.explicitCastArguments(mh, MethodType.methodType(r, a.type(), b.type()));
    return new Closure<>(mh, a, b);
  }

  public static <A> Closure<Boolean> predicate(Var<A> a, Predicate<A> predicate) {
    MethodHandle mh = FunctionsMh.predicateTest(predicate);
    mh = MethodHandles.explicitCastArguments(mh, MethodType.methodType(boolean.class, a.type()));
    return new Closure<>(mh, a);
  }

  public static <A, B> Closure<Boolean> biPredicate(
      Var<A> a, Var<B> b, BiPredicate<A, B> predicate) {
    MethodHandle mh = FunctionsMh.biPredicateTest(predicate);
    mh =
        MethodHandles.explicitCastArguments(
            mh, MethodType.methodType(boolean.class, a.type(), b.type()));
    return new Closure<>(mh, a, b);
  }

  public static Closure<Void> runnable(Runnable runnable) {
    MethodHandle mh = FunctionsMh.runnableRun(runnable);
    return new Closure<>(mh);
  }

  public static <R> Closure<R> supplier(Class<R> rType, Supplier<R> supplier) {
    MethodHandle mh = FunctionsMh.supplierGet(supplier);
    mh = MethodHandles.explicitCastArguments(mh, MethodType.methodType(rType));
    return new Closure<>(mh);
  }

  public static <R> Closure<R> ifThenElse(Var<Boolean> cond, Closure<R> thenCl, Closure<R> elseCl) {
    Preconditions.checkArgument(thenCl.returnType() == elseCl.returnType());

    MethodHandle thenUnifMh =
        MethodHandles.dropArguments(
            thenCl.mh, thenCl.methodType().parameterCount(), elseCl.methodType().parameterArray());
    MethodHandle elseUnifMh =
        MethodHandles.dropArguments(elseCl.mh, 0, thenCl.methodType().parameterArray());

    MethodHandle thenWithBMh = MethodHandles.dropArguments(thenUnifMh, 0, boolean.class);
    MethodHandle elseWithBMh = MethodHandles.dropArguments(elseUnifMh, 0, boolean.class);

    MethodHandle mh =
        MethodHandles.guardWithTest(
            MethodHandles.identity(boolean.class), thenWithBMh, elseWithBMh);
    return new Closure<>(
        mh,
        ArrayUtil.concat(
            new Var<?>[] {cond},
            thenCl.args.toArray(Var<?>[]::new),
            elseCl.args.toArray(Var<?>[]::new)));
  }

  public static <R> Closure<R> ifThenElse(Var<Boolean> cond, Var<R> thenVal, Var<R> elseVal) {
    Preconditions.checkArgument(thenVal.type().equals(elseVal.type()));
    return Closure.ifThenElse(cond, Closure.constant(thenVal), Closure.constant(elseVal));
  }

  public static Closure<Void> ifThen(Var<Boolean> cond, Closure<?> thenCl) {
    return ifThenElse(cond, thenCl.cast(void.class), constantVoid());
  }

}
