package com.github.stepancheg.mhlang;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * An utility to build {@link MethodHandle}.
 *
 * <p>An entry point to this library.
 */
public class MhBuilder extends Builder {

  private ArrayList<Var.Param<?>> params = new ArrayList<>();

  /**
   * Create a fresh new builder for {@link MethodHandle}.
   *
   * <p>This is needed mostly to create a stateful method handles (with single assignment multiple
   * use variables). Stateless closure can be also constructed with shortcuts like {@link #p1(Class,
   * Function)}.
   */
  public MhBuilder() {}

  @Override
  ImmutableList<Var<?>> paramsOrOuterVars() {
    return ImmutableList.copyOf(params);
  }

  @Override
  void addOuterVar(Var<?> outerVar) {
    throw new IllegalArgumentException("cannot reference outer var when building a function");
  }

  /**
   * Register a function ({@link MethodHandle}) param.
   *
   * <p>Returned {@link Var} object can be referenced when constructing {@link Closure} objects.
   */
  public <T> Var<T> addParam(Class<T> type) {
    Preconditions.checkState(type != void.class, "Parameter type must not be void");
    Preconditions.checkState(
        !bodyStarted(), "Cannot add function parameter after function body started");
    Var.Param<T> param = new Var.Param<>(functionId, params.size(), type);
    params.add(param);
    return param;
  }

  @SuppressWarnings("unchecked")
  public <T> Var<T> addParam(TypeToken<T> type) {
    return (Var<T>) addParam(type.getRawType());
  }

  /** {@inheritDoc} */
  @Override
  public <R> Var<R> assign(Closure<R> closure) {
    return super.assign(closure);
  }

  /** Finalize construction by creating a {@link MethodHandle} returning given expression. */
  public MethodHandle buildReturn(Expr<?> returnValue) {
    Var<?> val = assign(returnValue.asClosure());
    return buildReturnImpl(val).mh;
  }

  /** Finalize construction by creating a {@link MethodHandle} returning {@code void}. */
  public MethodHandle buildReturnVoid() {
    return buildReturn(Closure.constantVoid());
  }

  /**
   * Create a method handle from a parameterless closure.
   *
   * <p>If closure with state is needed, use {@link MhBuilder#MhBuilder()}.
   */
  public static <R> MethodHandle p0(Closure<R> closure) {
    MhBuilder b = new MhBuilder();
    return b.buildReturn(closure);
  }

  /**
   * Shortcut to create a single parameter method handle from a closure.
   *
   * <p>* If closure with state is needed, use {@link MhBuilder#MhBuilder()}.
   */
  public static <A, R> MethodHandle p1(Class<A> at, Function<Var<A>, Closure<R>> closure) {
    MhBuilder b = new MhBuilder();
    Var<A> ap = b.addParam(at);
    return b.buildReturn(closure.apply(ap));
  }

  /**
   * Shortcut to create a single parameter method handle from a closure.
   *
   * <p>* If closure with state is needed, use {@link MhBuilder#MhBuilder()}.
   */
  public static <A, R> MethodHandle p1(TypeToken<A> at, Function<Var<A>, Closure<R>> closure) {
    MhBuilder b = new MhBuilder();
    Var<A> ap = b.addParam(at);
    return b.buildReturn(closure.apply(ap));
  }

  /**
   * Shortcut to create a method handle from a two-parameter closure.
   *
   * <p>* If closure with state is needed, use {@link MhBuilder#MhBuilder()}.
   */
  public static <A, B, R> MethodHandle p2(
      Class<A> at, Class<B> bt, BiFunction<Var<A>, Var<B>, Closure<R>> closure) {
    MhBuilder b = new MhBuilder();
    Var<A> ap = b.addParam(at);
    Var<B> bp = b.addParam(bt);
    return b.buildReturn(closure.apply(ap, bp));
  }

  /**
   * Shortcut to create a method handle from a two-parameter closure.
   *
   * <p>* If closure with state is needed, use {@link MhBuilder#MhBuilder()}.
   */
  public static <A, B, R> MethodHandle p2(
      TypeToken<A> at, TypeToken<B> bt, BiFunction<Var<A>, Var<B>, Closure<R>> closure) {
    MhBuilder b = new MhBuilder();
    Var<A> ap = b.addParam(at);
    Var<B> bp = b.addParam(bt);
    return b.buildReturn(closure.apply(ap, bp));
  }
}
