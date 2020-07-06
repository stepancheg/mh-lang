package com.github.stepancheg.mhlang;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;

/**
 * An utility to build a {@link com.github.stepancheg.mhlang.Closure}. Statements in this builder
 * <b>can</b> refer outside variables, resulting closure will contain all the references.
 */
public class ClosureBuilder extends Builder {
  private ArrayList<Var<?>> outerVars = new ArrayList<>();

  /** Create a new builder for {@link Closure}. */
  public ClosureBuilder() {}

  /** {@inheritDoc} */
  @Override
  public <R> Var<R> assign(Closure<R> closure) {
    return super.assign(closure);
  }

  /** Finalize closure construction by returning given expression. */
  public <R> Closure<R> buildReturn(Expr<R> returnValue) {
    return buildReturnImpl(assign(returnValue.asClosure()));
  }

  /** Finalize closure construction by returning {@code void}. */
  public Closure<Void> buildReturnVoid() {
    return buildReturn(Closure.constantVoid());
  }

  @Override
  ImmutableList<Var<?>> paramsOrOuterVars() {
    return ImmutableList.copyOf(outerVars);
  }

  @Override
  void addOuterVar(Var<?> outerVar) {
    Preconditions.checkState(outerVar.functionId != functionId);
    outerVars.add(outerVar);
  }
}
