package com.github.stepancheg.mhlang;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

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
    Preconditions.checkArgument(mh.type().parameterCount() == args.size(), "mh %s does not match args %s", mh, args);
    for (int i = 0; i != args.size(); ++i) {
      Preconditions.checkArgument(mh.type().parameterType(i).equals(args.get(i).type()), "mh %s does not match args %s", mh, args);
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
  public <S> Closure<S> cast(Class<S> clazz) {
    if (returnType() == clazz) {
      return (Closure<S>) this;
    } else {
      return new Closure<>(
          MethodHandles.explicitCastArguments(mh, mh.type().changeReturnType(clazz)), args);
    }
  }

  public <S> Closure<S> filterReturnValue(Function<R, S> f) {
    return new Closure<>(MethodHandles.filterReturnValue(mh, FunctionsMh.function(f)), args);
  }

  public Closure<Integer> filterReturnValueToInt(ToIntFunction<R> f) {
    return new Closure<>(MethodHandles.filterReturnValue(mh, FunctionsMh.toIntFunction(f)), args);
  }

  public Closure<Boolean> filterReturnValueToBool(Predicate<R> f) {
    return new Closure<>(MethodHandles.filterReturnValue(mh, FunctionsMh.predicate(f)), args);
  }

  public static <R> Closure<R> var(Var<R> v) {
    return new Closure<>(MethodHandles.identity(v.type()), ImmutableList.of(v));
  }

  public static <R> Closure<R> constant(Class<R> clazz, R r) {
    return new Closure<R>(MethodHandles.constant(clazz, r));
  }

  public static Closure<Void> constantVoid() {
    return new Closure<>(MhUtil.NOP);
  }

  public static <R> Closure<R> method(Method method, Var<?>... args) {
    try {
      MethodHandle mh = MethodHandles.publicLookup().unreflect(method);
      return new Closure<>(mh, args);
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
      return new Closure<>(mh, object, value);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  public static <AA, A> Closure<A> getArrayElement(Var<AA> array, Var<Integer> index) {
    MethodHandle mh = MethodHandles.arrayElementGetter(array.type());
    return new Closure<>(mh, array, index);
  }

  public static <AA, A> Closure<Void> setArrayElement(Var<AA> array, Var<Integer> index, Var<A> value) {
    MethodHandle mh = MethodHandles.arrayElementSetter(array.type());
    return new Closure<>(mh, array, index, value);
  }

  public static <A, R> Closure<R> function(Class<R> r, Var<A> a, Function<A, R> f) {
    MethodHandle mh = FunctionsMh.function(f);
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
    MethodHandle mh = FunctionsMh.predicate(predicate);
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
    return Closure.ifThenElse(cond, Closure.var(thenVal), Closure.var(elseVal));
  }

  public static Closure<Void> ifThen(Var<Boolean> cond, Closure<?> thenCl) {
    return ifThenElse(cond, thenCl.cast(void.class), constantVoid());
  }

  private Closure<R> moveParamTo0(Var<?>... vs) {
    ImmutableMap<Var<?>, Integer> vsIndex = CollectionUtil.index(vs);

    Var<?>[] newArgs =
        Stream.concat(Arrays.stream(vs), args.stream().filter(a -> !vsIndex.containsKey(a)))
            .toArray(Var[]::new);

    int[] reorder = new int[mh.type().parameterCount()];
    int j = vs.length;
    for (int i = 0; i != reorder.length; ++i) {
      Integer special = vsIndex.get(this.args.get(i));
      if (special != null) {
        reorder[i] = special;
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

  private static class VarUpdate<R> {
    private final ImmutableList<Var.Param<?>> param;
    private final Closure<R> closure;

    VarUpdate(ImmutableList<Var.Param<?>> params, Closure<R> closure) {
      this.param = params;
      this.closure = closure;

      Preconditions.checkArgument(closure.args.subList(0, params.size()).equals(params));
      Preconditions.checkArgument(
          closure.args.subList(params.size(), closure.args.size()).stream()
              .noneMatch(params::contains));
    }

    ImmutableList<Var<?>> argsWithoutParam() {
      return closure.args.subList(param.size(), closure.args.size());
    }
  }

  private static <A, R> VarUpdate<R> varUpdate(Class<A> arg, Function<Var<A>, Closure<R>> f) {
    Var.Param<A> param = new Var.Param<>(FunctionId.nextId(), 0, 0, arg);
    Closure<R> closure = f.apply(param);
    closure = closure.moveParamTo0(param);
    return new VarUpdate<>(ImmutableList.of(param), closure);
  }

  private static <A, B, R> VarUpdate<R> varUpdate(
      Class<A> a0, Class<B> a1, BiFunction<Var<A>, Var<B>, Closure<R>> f) {
    long functionId = FunctionId.nextId();
    Var.Param<A> p0 = new Var.Param<>(functionId, 0, 0, a0);
    Var.Param<B> p1 = new Var.Param<>(functionId, 1, 0, a1);
    Closure<R> closure = f.apply(p0, p1);
    closure = closure.moveParamTo0(p0, p1);
    return new VarUpdate<>(ImmutableList.of(p0, p1), closure);
  }

  public static <R> Closure<R> whileLoop(
      Closure<R> init, Function<Var<R>, Closure<Boolean>> pred, Function<Var<R>, Closure<R>> body) {
    Class<R> vt = init.returnType();

    VarUpdate<Boolean> predU = varUpdate(vt, pred);
    VarUpdate<R> bodyU = varUpdate(vt, body);

    SigUnifier sigUnifier =
        new SigUnifier(init.args, predU.argsWithoutParam(), bodyU.argsWithoutParam());

    Closure<R> initFull = sigUnifier.unify(init);

    Closure<Boolean> predFull = sigUnifier.unifyWithoutFirst(predU.closure, 1);
    Closure<R> bodyFull = sigUnifier.unifyWithoutFirst(bodyU.closure, 1);

    MethodHandle mh = MethodHandles.whileLoop(initFull.mh, predFull.mh, bodyFull.mh);

    return new Closure<>(mh, sigUnifier.allVars);
  }

  public static <R> Closure<R> countedLoop(
      Closure<Integer> start,
      Closure<Integer> end,
      Closure<R> init,
      BiFunction<Var<R>, Var<Integer>, Closure<R>> body) {
    Class<R> vt = init.returnType();

    VarUpdate<R> bodyU = varUpdate(vt, int.class, body);

    SigUnifier sigUnifier =
        new SigUnifier(start.args, end.args, init.args, bodyU.argsWithoutParam());

    Closure<Integer> startFull = sigUnifier.unify(start);
    Closure<Integer> endFull = sigUnifier.unify(end);
    Closure<R> initFull = sigUnifier.unify(init);
    Closure<R> bodyFull = sigUnifier.unifyWithoutFirst(bodyU.closure, 2);

    MethodHandle mh = MethodHandles.countedLoop(startFull.mh, endFull.mh, initFull.mh, bodyFull.mh);

    return new Closure<>(mh, sigUnifier.allVars);
  }
}
