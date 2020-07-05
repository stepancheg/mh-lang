package com.github.stepancheg.mhlang;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;

public class ClosureBuilder extends Builder {
  private ArrayList<Var<?>> outerVars = new ArrayList<>();

  public <R> Closure<R> buildReturn(Expr<R> returnValue) {
    return buildReturnImpl(assign(returnValue.asClosure()));
  }

  public Closure<Void> buildReturnVoid() {
    return buildReturn(Closure.constantVoid());
  }

  @Override
  protected ImmutableList<Var<?>> paramsOrOuterVars() {
    return ImmutableList.copyOf(outerVars);
  }

  @Override
  protected void addOuterVar(Var<?> outerVar) {
    Preconditions.checkState(outerVar.functionId != functionId);
    outerVars.add(outerVar);
  }
}
