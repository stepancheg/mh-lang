package com.github.stepancheg.mhlang;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.*;
import java.util.stream.Stream;

/**
 * Closure is a pair of {@link MethodHandle} and a list of {@link Var} for each method handle
 * parameter.
 */
public class Closure<R> extends Expr<R> {

  final MethodHandle mh;
  final ImmutableList<Var<?>> args;

  /**
   * Construct a closure. This is a low-level operation.
   *
   * @see #fold(MethodHandle, Expr[]) for more convenient constructor accepting any expressions.
   */
  public Closure(MethodHandle mh, ImmutableList<Var<?>> args) {
    Preconditions.checkArgument(
        mh.type().parameterCount() == args.size(), "mh %s does not match args %s", mh, args);
    for (int i = 0; i != args.size(); ++i) {
      Preconditions.checkArgument(
          mh.type().parameterType(i).equals(args.get(i).type()),
          "mh %s does not match args %s",
          mh,
          args);
    }

    this.mh = mh;
    this.args = args;
  }

  /**
   * Construct a closure. This is a low-level operation.
   *
   * @see #fold(MethodHandle, Expr[]) for more convenient constructor accepting any expressions.
   */
  public Closure(MethodHandle mh, Var<?>... args) {
    this(mh, ImmutableList.copyOf(args));
  }

  public static <R> Closure<R> fold(MethodHandle mh, Expr<?>... args) {
    Preconditions.checkArgument(
        mh.type().parameterCount() == args.length, "mh %s does not match args %s", mh, args);
    for (int i = 0; i != args.length; ++i) {
      Preconditions.checkArgument(
          mh.type().parameterType(i).equals(args[i].type()),
          "mh %s does not match args %s",
          mh,
          args);
    }

    MethodHandle collectedMh = mh;

    ImmutableList.Builder<Var<?>> vars = ImmutableList.builder();

    for (int i = args.length - 1; i >= 0; --i) {
      Expr<?> arg = args[i];
      if (arg instanceof Var<?>) {
        vars.add((Var<?>) arg);
      } else if (arg instanceof Closure<?>) {
        collectedMh = MethodHandles.collectArguments(collectedMh, i, ((Closure<?>) arg).mh);
        vars.addAll(((Closure<?>) arg).args.reverse());
      } else {
        throw new IllegalArgumentException("unknown arg: " + arg.getClass().getName());
      }
    }

    return new Closure<>(collectedMh, vars.build().reverse());
  }

  /** For a closure {@code c(...)} return a closure {@code !c(...)}. */
  public static Closure<Boolean> not(Closure<Boolean> expr) {
    return expr.asClosure().filterReturnValue(MhUtil.NOT);
  }

  /**
   * For a closure {@code c(...)} return a closure {@code c(...) != null}. This operation also works
   * for closures returning primitive types, in this case resulting closure records the original
   * closure side effect and return {@code true}.
   */
  public Closure<Boolean> isNotNull() {
    if (type().isPrimitive()) {
      ClosureBuilder b = new ClosureBuilder();
      // preserve side effect
      b.assign(asClosure());
      return b.buildReturn(constant(true));
    } else {
      return asClosure().cast(Object.class).filterReturnValue(MhUtil.IS_NOT_NULL);
    }
  }

  /**
   * For a closure {@code c(...)} return a closure {@code c(...) == null}. This operation also works
   * for closures returning primitive types, in this case resulting closure records the original
   * closure side effect and return {@code false}.
   */
  public Closure<Boolean> isNull() {
    return Closure.not(isNotNull());
  }

  @SuppressWarnings("unchecked")
  @Override
  public Class<R> type() {
    return (Class<R>) mh.type().returnType();
  }

  @Override
  public Closure<R> asClosure() {
    return this;
  }

  /**
   * Dynamically cast return value to a given type.
   *
   * <p>This operation is a wrapper to {@link MethodHandles#explicitCastArguments(MethodHandle,
   * MethodType)}.
   */
  @SuppressWarnings("unchecked")
  public <S> Closure<S> cast(Class<S> clazz) {
    if (type() == clazz) {
      return (Closure<S>) this;
    } else {
      return new Closure<>(
          MethodHandles.explicitCastArguments(mh, mh.type().changeReturnType(clazz)), args);
    }
  }

  /**
   * Dynamically cast return value to a {@link TypeToken#getRawType()} of given type.
   *
   * <p>This operation is a wrapper to {@link MethodHandles#explicitCastArguments(MethodHandle,
   * MethodType)}.
   */
  @SuppressWarnings("unchecked")
  public <S> Closure<S> cast(TypeToken<S> type) {
    return (Closure<S>) cast(type.getRawType());
  }

  /** Shortcut for {@link MethodHandles#filterReturnValue(MethodHandle, MethodHandle)}. */
  public <S> Closure<S> filterReturnValue(MethodHandle filter) {
    Preconditions.checkArgument(
        filter.type().parameterCount() == 1, "filter mh must have single arg: %s", filter);
    Preconditions.checkArgument(
        filter.type().parameterType(0) == mh.type().returnType(),
        "filter %s parameter 0 must match this return %s",
        filter,
        this);
    return new Closure<>(MethodHandles.filterReturnValue(this.mh, filter), args);
  }

  public <S> Closure<S> filterReturnValue(Function<R, S> f) {
    return filterReturnValue(FunctionsMh.function(f));
  }

  public Closure<Integer> filterReturnValueToInt(ToIntFunction<R> f) {
    return filterReturnValue(FunctionsMh.toIntFunction(f));
  }

  public Closure<Boolean> filterReturnValueToBool(Predicate<R> f) {
    return filterReturnValue(FunctionsMh.predicate(f));
  }

  /** Variable as a {@link com.github.stepancheg.mhlang.Closure}. */
  public static <R> Closure<R> var(Var<R> v) {
    return new Closure<>(MethodHandles.identity(v.type()), ImmutableList.of(v));
  }

  /** Closure which returns a constant of specified type. */
  public static <R> Closure<R> constant(Class<R> clazz, R r) {
    return new Closure<>(MethodHandles.constant(clazz, r));
  }

  /**
   * Closure which returns a constant, the type of closure is obtained using {@link
   * Object#getClass()}.
   */
  @SuppressWarnings("unchecked")
  public static <R> Closure<R> constant(R r) {
    Preconditions.checkArgument(r != null, "constant must not be null");
    return constant((Class<R>) r.getClass(), r);
  }

  /** A constant. */
  public static Closure<Boolean> constant(boolean b) {
    return constant(boolean.class, b);
  }

  /** A constant. */
  public static Closure<Integer> constant(int i) {
    return constant(int.class, i);
  }

  /** A constant. */
  public static Closure<Short> constant(short i) {
    return constant(short.class, i);
  }

  /** A constant. */
  public static Closure<Character> constant(char c) {
    return constant(char.class, c);
  }

  /** A constant. */
  public static Closure<Long> constant(long i) {
    return constant(long.class, i);
  }

  /** A constant. */
  public static Closure<Float> constant(float f) {
    return constant(float.class, f);
  }

  /** A constant. */
  public static Closure<Double> constant(double f) {
    return constant(double.class, f);
  }

  /** No-op closure. */
  public static Closure<Void> constantVoid() {
    return new Closure<>(MethodHandles.zero(void.class));
  }

  /**
   * {@link #method(Method, MethodHandles.Lookup, Expr[])} with lookup parameter is {@link
   * MethodHandles#publicLookup()}
   */
  public static <R> Closure<R> method(Method method, Expr<?>... args) {
    return method(method, MethodHandles.publicLookup(), args);
  }

  /** Wrapper for {@link java.lang.invoke.MethodHandles.Lookup#unreflect(Method)}. */
  public static <R> Closure<R> method(Method method, MethodHandles.Lookup lookup, Expr<?>[] args) {
    try {
      MethodHandle mh = lookup.unreflect(method);
      return Closure.fold(mh, args);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * {@link #constructor(Constructor, MethodHandles.Lookup, Expr[])} with {@link
   * MethodHandles#publicLookup()}.
   */
  public static <R> Closure<R> constructor(Constructor<R> constructor, Expr<?>... args) {
    return constructor(constructor, MethodHandles.publicLookup(), args);
  }

  /** Wrapper for {@link MethodHandles.Lookup#unreflectConstructor(Constructor)}. */
  public static <R> Closure<R> constructor(
      Constructor<R> constructor, MethodHandles.Lookup lookup, Expr<?>[] args) {
    try {
      MethodHandle mh = lookup.unreflectConstructor(constructor);
      return Closure.fold(mh, args);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * {@link #getField(Field, Expr, MethodHandles.Lookup)} with {@link MethodHandles#publicLookup()}.
   */
  public static <R> Closure<R> getField(Field field, Expr<?> object) {
    MethodHandles.Lookup lookup = MethodHandles.publicLookup();
    return getField(field, object, lookup);
  }

  /** Wrapper for {@link MethodHandles.Lookup#unreflectGetter(java.lang.reflect.Field)}. */
  public static <R> Closure<R> getField(Field field, Expr<?> object, MethodHandles.Lookup lookup) {
    Preconditions.checkArgument(
        (field.getModifiers() & Modifier.STATIC) == 0, "field should not be static: %s", field);
    try {
      MethodHandle mh = lookup.unreflectGetter(field);
      return Closure.fold(mh, object);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * {@link #setField(Field, Expr, Expr, MethodHandles.Lookup)} with {@link
   * MethodHandles#publicLookup()}.
   */
  public static Closure<Void> setField(Field field, Expr<?> object, Expr<?> value) {
    MethodHandles.Lookup lookup = MethodHandles.publicLookup();
    return setField(field, object, value, lookup);
  }

  /** Wrapper for {@link MethodHandles.Lookup#unreflectSetter(Field)}. */
  public static Closure<Void> setField(
      Field field, Expr<?> object, Expr<?> value, MethodHandles.Lookup lookup) {
    Preconditions.checkArgument(
        (field.getModifiers() & Modifier.STATIC) == 0, "field should not be static: %s", field);
    try {
      MethodHandle mh = lookup.unreflectSetter(field);
      return Closure.fold(mh, object, value);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  /** Wrapper for {@link MethodHandles#arrayElementGetter(Class)}. */
  public static <AA, A> Closure<A> getArrayElement(Expr<AA> array, Expr<Integer> index) {
    MethodHandle mh = MethodHandles.arrayElementGetter(array.type());
    return Closure.fold(mh, array, index);
  }

  /** Wrapper for {@link MethodHandles#arrayElementSetter(Class)}. */
  public static <AA, A> Closure<Void> setArrayElement(
      Expr<AA> array, Expr<Integer> index, Expr<A> value) {
    MethodHandle mh = MethodHandles.arrayElementSetter(array.type());
    return Closure.fold(mh, array, index, value);
  }

  /** Wrapper for {@link MethodHandles#arrayConstructor(Class)}. */
  public static <AA> Closure<AA> newArray(Class<AA> arrayType, Expr<Integer> size) {
    MethodHandle mh = MethodHandles.arrayConstructor(arrayType);
    return Closure.fold(mh, size);
  }

  /** Wrapper for {@link MethodHandles#arrayLength(Class)}. */
  public static <AA> Closure<Integer> arrayLength(Expr<AA> array) {
    MethodHandle mh = MethodHandles.arrayLength(array.type());
    return Closure.fold(mh, array);
  }

  /** Make a closure from given function. */
  public static <A, R> Closure<R> function(Class<R> r, Expr<A> a, Function<A, R> f) {
    MethodHandle mh = FunctionsMh.function(f);
    mh = MethodHandles.explicitCastArguments(mh, MethodType.methodType(r, a.type()));
    return Closure.fold(mh, a);
  }

  /** Make a closure from given function. */
  public static <A, B, R> Closure<R> biFunction(
      Class<R> r, Expr<A> a, Expr<B> b, BiFunction<A, B, R> function) {
    MethodHandle mh = FunctionsMh.biFunction(function);
    mh = MethodHandles.explicitCastArguments(mh, MethodType.methodType(r, a.type(), b.type()));
    return Closure.fold(mh, a, b);
  }

  /** Make a closure from given function. */
  public static <A, B> Closure<Void> biConsumer(Expr<A> a, Expr<B> b, BiConsumer<A, B> function) {
    MethodHandle mh = FunctionsMh.biConsumer(function);
    mh =
        MethodHandles.explicitCastArguments(
            mh, MethodType.methodType(void.class, a.type(), b.type()));
    return Closure.fold(mh, a, b);
  }

  /** Make a closure from given function. */
  public static <A> Closure<Boolean> predicate(Expr<A> a, Predicate<A> predicate) {
    MethodHandle mh = FunctionsMh.predicate(predicate);
    mh = MethodHandles.explicitCastArguments(mh, MethodType.methodType(boolean.class, a.type()));
    return Closure.fold(mh, a);
  }

  /** Make a closure from given function. */
  public static <A, B> Closure<Boolean> biPredicate(
      Expr<A> a, Expr<B> b, BiPredicate<A, B> predicate) {
    MethodHandle mh = FunctionsMh.biPredicateTest(predicate);
    mh =
        MethodHandles.explicitCastArguments(
            mh, MethodType.methodType(boolean.class, a.type(), b.type()));
    return Closure.fold(mh, a, b);
  }

  /** Make a closure from given function. */
  public static Closure<Void> runnable(Runnable runnable) {
    MethodHandle mh = FunctionsMh.runnableRun(runnable);
    return new Closure<>(mh);
  }

  /** Make a closure from given function. */
  public static <R> Closure<R> supplier(Class<R> rType, Supplier<R> supplier) {
    MethodHandle mh = FunctionsMh.supplierGet(supplier);
    mh = MethodHandles.explicitCastArguments(mh, MethodType.methodType(rType));
    return new Closure<>(mh);
  }

  /** Make a closure from given function. */
  public static Closure<Integer> intUnaryOperator(Expr<Integer> a, IntUnaryOperator f) {
    MethodHandle mh = FunctionsMh.intUnaryOperator(f);
    return Closure.fold(mh, a);
  }

  /** Make a closure from given function. */
  public static Closure<Boolean> intPredicate(Expr<Integer> a, IntPredicate f) {
    MethodHandle mh = FunctionsMh.intPredicate(f);
    return Closure.fold(mh, a);
  }

  /** Return a closure {@code a == b}. */
  @SuppressWarnings("unchecked")
  public static <A> Closure<Boolean> same(Expr<A> a, Expr<A> b) {
    Preconditions.checkArgument(a.type().isPrimitive() == b.type().isPrimitive());
    Class<?> type;
    if (a.type().isPrimitive()) {
      Preconditions.checkArgument(a.type() == b.type());
      type = a.type();
    } else {
      Preconditions.checkArgument(
          a.type().isAssignableFrom(b.type()) || b.type().isAssignableFrom(a.type()));

      type = Object.class;
      a = (Expr<A>) a.asClosure().cast(Object.class);
      b = (Expr<A>) b.asClosure().cast(Object.class);
    }
    return Closure.fold(MhUtil.same(type), a, b);
  }

  /**
   * Return a closure {@code a == b} for primitive types or {@link Objects#equals(Object, Object)}
   * for object types.
   */
  public static <A> Closure<Boolean> equals(Expr<A> a, Expr<A> b) {
    Preconditions.checkArgument(a.type() == b.type());
    return Closure.fold(MhUtil.eq(a.type()), a, b);
  }

  /** Hash code for a value. Return 0 for {@code null}. */
  public static <A> Closure<Integer> hashCode(Expr<A> a) {
    Preconditions.checkArgument(a.type() != void.class);
    return Closure.fold(MhUtil.hashCode(a.type()), a);
  }

  /** {@link Objects#toString(Object)}. */
  public static <A> Closure<String> toString(Expr<A> a) {
    if (a.type() == void.class) {
      return constant("void");
    } else {
      return Closure.fold(MhUtil.toString(a.type()), a);
    }
  }

  /**
   * {@code a + b} where {@code a} and {@code b} have the same type {@code int}, {@link long} or
   * {@link String}.
   */
  public static <R> Closure<R> plus(Expr<R> a, Expr<R> b) {
    Preconditions.checkArgument(a.type() == b.type());
    return Closure.fold(MhUtil.plus(a.type()), a, b);
  }

  /** {@code a * b} where {@code a} and {@code b} have the same type {@code int} or {@link long}. */
  public static <R> Closure<R> mul(Expr<R> a, Expr<R> b) {
    Preconditions.checkArgument(a.type() == b.type());
    return Closure.fold(MhUtil.mul(a.type()), a, b);
  }

  /** Wrap {@link MethodHandles#throwException(Class, Class)}. */
  public static <R> Closure<R> throwException(
      Class<R> returnType, Expr<? extends Throwable> exception) {
    return Closure.fold(MethodHandles.throwException(returnType, exception.type()), exception);
  }

  /** {@code cond ? thenExpr() : elseExpr()}. */
  public static <R> Closure<R> ifThenElse(Expr<Boolean> cond, Expr<R> thenExpr, Expr<R> elseExpr) {
    Closure<R> thenCl = thenExpr.asClosure();
    Closure<R> elseCl = elseExpr.asClosure();

    Preconditions.checkArgument(thenCl.type() == elseCl.type());

    MethodHandle thenUnifMh =
        MethodHandles.dropArguments(
            thenCl.mh, thenCl.mh.type().parameterCount(), elseCl.mh.type().parameterArray());
    MethodHandle elseUnifMh =
        MethodHandles.dropArguments(elseCl.mh, 0, thenCl.mh.type().parameterArray());

    MethodHandle thenWithBMh = MethodHandles.dropArguments(thenUnifMh, 0, boolean.class);
    MethodHandle elseWithBMh = MethodHandles.dropArguments(elseUnifMh, 0, boolean.class);

    MethodHandle mh =
        MethodHandles.guardWithTest(
            MethodHandles.identity(boolean.class), thenWithBMh, elseWithBMh);
    return Closure.fold(
        mh,
        ArrayUtil.concat(
            new Expr<?>[] {cond},
            thenCl.args.toArray(Var<?>[]::new),
            elseCl.args.toArray(Var<?>[]::new)));
  }

  /** {@code cond ? (void) thenExpr(...) : void}. */
  public static Closure<Void> ifThen(Expr<Boolean> cond, Expr<?> thenCl) {
    return ifThenElse(cond, thenCl.asClosure().cast(void.class), constantVoid());
  }

  /** {@code a(...) || b(...)}. */
  public static Closure<Boolean> or(Expr<Boolean> a, Expr<Boolean> b) {
    return ifThenElse(a, Closure.constant(true), b.asClosure());
  }

  /** {@code a(...) && b(...)}. */
  public static Closure<Boolean> and(Expr<Boolean> a, Expr<Boolean> b) {
    return ifThenElse(a, b.asClosure(), Closure.constant(false));
  }

  /** {@code a(...) || ...}. */
  @SafeVarargs
  public static Closure<Boolean> or(Expr<Boolean>... as) {
    if (as.length == 0) {
      return constant(false);
    } else {
      return or(as[0], or(Arrays.copyOfRange(as, 1, as.length)));
    }
  }

  /** {@code a(...) || ...}. */
  public static Closure<Boolean> or(List<Expr<Boolean>> as) {
    if (as.isEmpty()) {
      return constant(false);
    } else {
      return or(as.get(0), or(as.subList(1, as.size())));
    }
  }

  /** {@code a(...) && ...}. */
  @SafeVarargs
  public static Closure<Boolean> and(Expr<Boolean>... as) {
    if (as.length == 0) {
      return constant(true);
    } else {
      return and(as[0], and(Arrays.copyOfRange(as, 1, as.length)));
    }
  }

  /** {@code a(...) && ...}. */
  public static Closure<Boolean> and(List<Expr<Boolean>> args) {
    if (args.isEmpty()) {
      return constant(true);
    } else {
      return and(args.get(0), and(args.subList(1, args.size())));
    }
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
    Var.Param<A> param = new Var.Param<>(FunctionId.nextId(), 0, arg);
    Closure<R> closure = f.apply(param);
    closure = closure.moveParamTo0(param);
    return new VarUpdate<>(ImmutableList.of(param), closure);
  }

  private static <A, B, R> VarUpdate<R> varUpdate(
      Class<A> a0, Class<B> a1, BiFunction<Var<A>, Var<B>, Closure<R>> f) {
    long functionId = FunctionId.nextId();
    Var.Param<A> p0 = new Var.Param<>(functionId, 0, a0);
    Var.Param<B> p1 = new Var.Param<>(functionId, 1, a1);
    Closure<R> closure = f.apply(p0, p1);
    closure = closure.moveParamTo0(p0, p1);
    return new VarUpdate<>(ImmutableList.of(p0, p1), closure);
  }

  /**
   * White loop.
   *
   * <pre>
   *     v = init(...)
   *     while (pred(v, ...)) {
   *         v = body(v, ...)
   *     }
   *     return v;
   * </pre>
   *
   * Note {@link ClosureBuilder} can be used to build a closure.
   */
  public static <R> Closure<R> whileLoop(
      Closure<R> init, Function<Var<R>, Closure<Boolean>> pred, Function<Var<R>, Closure<R>> body) {
    Class<R> vt = init.type();

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

  /**
   * White loop.
   *
   * <pre>
   *     v = init(...)
   *     do {
   *         v = body(v, ...)
   *     } while (pred(v, ...));
   *     return v;
   * </pre>
   *
   * Note {@link ClosureBuilder} can be used to build a closure.
   */
  public static <R> Closure<R> doWhileLoop(
      Closure<R> init, Function<Var<R>, Closure<R>> body, Function<Var<R>, Closure<Boolean>> pred) {
    Class<R> vt = init.type();

    VarUpdate<R> bodyU = varUpdate(vt, body);
    VarUpdate<Boolean> predU = varUpdate(vt, pred);

    SigUnifier sigUnifier =
        new SigUnifier(init.args, bodyU.argsWithoutParam(), predU.argsWithoutParam());

    Closure<R> initFull = sigUnifier.unify(init);
    Closure<R> bodyFull = sigUnifier.unifyWithoutFirst(bodyU.closure, 1);
    Closure<Boolean> predFull = sigUnifier.unifyWithoutFirst(predU.closure, 1);

    MethodHandle mh = MethodHandles.doWhileLoop(initFull.mh, bodyFull.mh, predFull.mh);

    return new Closure<R>(mh, sigUnifier.allVars);
  }

  /**
   * Counted loop.
   *
   * <pre>
   *     s = start(...)
   *     e = end(...)
   *     v = init(...)
   *     for (int i = s; i < e; ++i) {
   *         v = body(v, i, ...)
   *     }
   *     return v;
   * </pre>
   *
   * Note {@link ClosureBuilder} can be used to build a closure.
   */
  public static <R> Closure<R> countedLoop(
      Closure<Integer> start,
      Closure<Integer> end,
      Closure<R> init,
      BiFunction<Var<R>, Var<Integer>, Closure<R>> body) {
    Class<R> vt = init.type();

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

  /**
   * Iterator loop.
   *
   * <pre>
   *     i = iterator(...)
   *     v = init(...)
   *     while (i.hasNext()) {
   *         t = i.next();
   *         v = body(v, t, ...);
   *     }
   *     return v;
   * </pre>
   *
   * Note {@link ClosureBuilder} can be used to build a closure.
   */
  public static <T, V> Closure<V> iteratorLoop(
      Class<T> tt,
      Closure<Iterator<T>> iterator,
      Closure<V> init,
      BiFunction<Var<V>, Var<T>, Closure<V>> body) {
    Class<V> vt = init.type();

    VarUpdate<V> bodyU = varUpdate(vt, tt, body);

    SigUnifier sigUnifier = new SigUnifier(iterator.args, init.args, bodyU.argsWithoutParam());

    Closure<Iterator<T>> iteratorFull = sigUnifier.unify(iterator);
    Closure<V> initFull = sigUnifier.unify(init);
    Closure<V> bodyFull = sigUnifier.unifyWithoutFirst(bodyU.closure, 2);

    MethodHandle mh = MethodHandles.iteratedLoop(iteratorFull.mh, initFull.mh, bodyFull.mh);

    return new Closure<>(mh, sigUnifier.allVars);
  }

  private static final MethodHandle ITERABLE_ITERATOR;

  static {
    try {
      ITERABLE_ITERATOR =
          MethodHandles.publicLookup()
              .findVirtual(Iterable.class, "iterator", MethodType.methodType(Iterator.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Iterator loop.
   *
   * <pre>
   *     i = iteratble(...).iterator()
   *     v = init(...)
   *     while (i.hasNext()) {
   *         t = i.next();
   *         v = body(v, t, ...);
   *     }
   *     return v;
   * </pre>
   *
   * Note {@link ClosureBuilder} can be used to build a closure.
   */
  public static <T, V, C extends Iterable<T>> Closure<V> iterableLoop(
      Class<T> tt, Expr<C> iterable, Closure<V> init, BiFunction<Var<V>, Var<T>, Closure<V>> body) {
    Preconditions.checkArgument(Iterable.class.isAssignableFrom(iterable.type()));

    return iteratorLoop(
        tt,
        iterable.asClosure().cast(Iterable.class).filterReturnValue(ITERABLE_ITERATOR),
        init,
        body);
  }

  @Override
  public String toString() {
    return args + " -> " + mh.type().returnType().getSimpleName();
  }
}
