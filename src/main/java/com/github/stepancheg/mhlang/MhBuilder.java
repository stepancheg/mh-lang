package com.github.stepancheg.mhlang;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;

/**
 * An utility to build {@link java.lang.invoke.MethodHandle}.
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
   * Register a function ({@link java.lang.invoke.MethodHandle}) param.
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

  /** {@inheritDoc} */
  @Override
  public <R> Var<R> assign(Closure<R> closure) {
    return super.assign(closure);
  }

  /**
   * Finalize construction by creating a {@link java.lang.invoke.MethodHandle} returning given
   * expression.
   */
  public MethodHandle buildReturn(Expr<?> returnValue) {
    Var<?> val = assign(returnValue.asClosure());
    return buildReturnImpl(val).mh;
  }

  /**
   * Finalize construction by creating a {@link java.lang.invoke.MethodHandle} returning {@code
   * void}.
   */
  public MethodHandle buildReturnVoid() {
    return buildReturn(Closure.constantVoid());
  }
}
