package com.github.stepancheg.mhlang;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.*;
import java.util.stream.Stream;

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

  public static Closure<Integer> intUnaryOperator(Var<Integer> a, IntUnaryOperator f) {
    MethodHandle mh = FunctionsMh.intUnaryOperator(f);
    return new Closure<>(mh, a);
  }

  public static Closure<Boolean> intPredicate(Var<Integer> a, IntPredicate f) {
    MethodHandle mh = FunctionsMh.intPredicate(f);
    return new Closure<>(mh, a);
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

  private Closure<R> moveParamTo0(Var<?> v) {
    Var<?>[] newArgs =
        Stream.concat(Stream.of(v), args.stream().filter(a -> a != v)).toArray(Var[]::new);

    int[] reorder = new int[mh.type().parameterCount()];
    int j = 1;
    for (int i = 0; i != reorder.length; ++i) {
      if (this.args.get(i) == v) {
        reorder[i] = 0;
      } else {
        reorder[i] = j++;
      }
    }
    Preconditions.checkState(j == newArgs.length);

    MethodType t =
        MethodType.methodType(
            mh.type().returnType(), Arrays.stream(newArgs).map(Var::type).toArray(Class<?>[]::new));

    MethodHandle mh = MethodHandles.permuteArguments(this.mh, t, reorder);

    return new Closure<>(mh, newArgs);
  }

  private Closure<R> dropArguments(int pos, Var<?>... args) {
    Var[] newArgs =
        ArrayUtil.concat(
            this.args.subList(0, pos).toArray(new Var[0]),
            args,
            this.args.subList(pos, this.args.size()).toArray(new Var[0]));
    MethodHandle newMh =
        MethodHandles.dropArguments(
            this.mh, pos, Arrays.stream(args).map(Var::type).toArray(Class[]::new));
    return new Closure<R>(newMh, newArgs);
  }

  private Closure<R> dropArguments(int pos, ImmutableList<Var<?>> args) {
    return dropArguments(pos, args.toArray(Var[]::new));
  }

  private static class VarUpdate<A, R> {
    private final Var.Param<A> param;
    private final Closure<R> closure;

    public VarUpdate(Var.Param<A> param, Closure<R> closure) {
      this.param = param;
      this.closure = closure;

      Preconditions.checkArgument(closure.args.get(0) == param);
      Preconditions.checkArgument(
          closure.args.subList(1, closure.args.size()).stream().noneMatch(v -> v == param));
    }

    public ImmutableList<Var<?>> argsWithoutParam() {
      return closure.args.subList(1, closure.args.size());
    }
  }

  private static <A, R> VarUpdate<A, R> varUpdate(Class<A> arg, Function<Var<A>, Closure<R>> f) {
    Var.Param<A> param = new Var.Param<>(FunctionId.nextId(), 0, 0, arg);
    Closure<R> closure = f.apply(param);
    closure = closure.moveParamTo0(param);
    return new VarUpdate<>(param, closure);
  }

  public static <R> Closure<R> whileLoop(
      Closure<R> init, Function<Var<R>, Closure<Boolean>> pred, Function<Var<R>, Closure<R>> body) {
    Class<R> vt = init.returnType();

    VarUpdate<R, Boolean> predU = varUpdate(vt, pred);
    VarUpdate<R, R> bodyU = varUpdate(vt, body);

    ImmutableList.Builder<Var<?>> varsB = ImmutableList.builder();
    varsB.addAll(init.args);
    varsB.addAll(predU.argsWithoutParam());
    varsB.addAll(bodyU.argsWithoutParam());
    ImmutableList<Var<?>> vars = varsB.build();

    Closure<R> initFull =
        init.dropArguments(init.args.size(), bodyU.argsWithoutParam())
            .dropArguments(init.args.size(), predU.argsWithoutParam());

    Closure<Boolean> predFull =
        predU
            .closure
            .dropArguments(predU.closure.args.size(), bodyU.argsWithoutParam())
            .dropArguments(1, init.args);
    Closure<R> bodyFull =
        bodyU.closure.dropArguments(1, predU.argsWithoutParam()).dropArguments(1, init.args);

    MethodHandle mh = MethodHandles.whileLoop(initFull.mh, predFull.mh, bodyFull.mh);

    return new Closure<>(mh, vars);
  }
}
