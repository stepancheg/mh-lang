package com.github.stepancheg.mhlang;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Function;

/**
 * An utility to build {@link MethodHandle}.
 *
 * <p>An entry point to this library.
 */
public class MhBuilder extends Builder {

  private ArrayList<Var.Param<?>> params = new ArrayList<>();

  /** Create a fresh new builder for {@link MethodHandle}. */
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

  /**
   * Finalize construction by creating a {@link MethodHandle} returning given
   * expression.
   */
  public MethodHandle buildReturn(Expr<?> returnValue) {
    Var<?> val = assign(returnValue.asClosure());
    return buildReturnImpl(val).mh;
  }

  /**
   * Finalize construction by creating a {@link MethodHandle} returning {@code
   * void}.
   */
  public MethodHandle buildReturnVoid() {
    return buildReturn(Closure.constantVoid());
  }

  public static <A, R> MethodHandle shortcut(Class<A> param, Function<Var<A>, Closure<R>> closure) {
    MhBuilder b = new MhBuilder();
    Var<A> p = b.addParam(param);
    return b.buildReturn(Objects.requireNonNull(closure.apply(p)));
  }
}
