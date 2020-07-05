package com.github.stepancheg.mhlang;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;

public class MhBuilder extends Builder {

  private ArrayList<Var.Param<?>> params = new ArrayList<>();

  @Override
  protected ImmutableList<Var<?>> paramsOrOuterVars() {
    return ImmutableList.copyOf(params);
  }

  @Override
  protected void addOuterVar(Var<?> outerVar) {
    throw new IllegalArgumentException("cannot reference outer var when building a function");
  }

  public <T> Var<T> addParam(Class<T> type) {
    Preconditions.checkState(type != void.class, "Parameter type must not be void");
    Preconditions.checkState(
        !bodyStarted(), "Cannot add function parameter after function body started");
    Var.Param<T> param = new Var.Param<>(functionId, params.size(), type);
    params.add(param);
    return param;
  }

  public MethodHandle buildReturn(Expr<?> returnValue) {
    Var<?> val = assign(returnValue.asClosure());
    return buildReturnImpl(val).mh;
  }

  public MethodHandle buildReturnVoid() {
    return buildReturn(Closure.constantVoid());
  }
}
