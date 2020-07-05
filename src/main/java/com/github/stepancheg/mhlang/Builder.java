package com.github.stepancheg.mhlang;

import com.google.common.base.Preconditions;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.stream.Stream;

public class Builder {

  private final long functionId = FunctionId.nextId();

  private ArrayList<Var.Param<?>> params = new ArrayList<>();

  private ArrayList<Var.Invoke<?>> assignments = new ArrayList<>();
  private ArrayList<Var.Invoke<?>> nonVoidAssignments = new ArrayList<>();

  public Builder() {}

  private int nextVarId() {
    return params.size() + assignments.size();
  }

  private int nextNonVoidIndex() {
    return params.size() + nonVoidAssignments.size();
  }

  private boolean bodyStarted() {
    return !assignments.isEmpty();
  }

  public <T> Var<T> addParam(Class<T> type) {
    Preconditions.checkState(type != void.class, "Parameter type must not be void");
    Preconditions.checkState(
        !bodyStarted(), "Cannot add function parameter after function body started");
    Var.Param<T> param = new Var.Param<>(functionId, nextVarId(), nextNonVoidIndex(), type);
    params.add(param);
    return param;
  }

  public <R> Var<R> assign(Closure<R> closure) {
    Var.Invoke<R> var = new Var.Invoke<>(functionId, nextVarId(), nextNonVoidIndex(), closure);
    assignments.add(var);
    if (var.type() != void.class) {
      nonVoidAssignments.add(var);
    }
    return var;
  }

  private Class<?>[] paramsAtStep(int step) {
    int count;
    if (step == assignments.size()) {
      count = params.size() + nonVoidAssignments.size();
    } else {
      count = assignments.get(step).nonVoidVarIndex;
    }
    return Stream.concat(
      params.stream().map(Var.Param::type),
      nonVoidAssignments.stream().map(Var.Invoke::type)
      ).limit(count).toArray(Class<?>[]::new);
  }

  private MethodHandle step(int step, MethodHandle next) {
    Var.Invoke<?> assignment = assignments.get(step);
    MethodHandle mh = assignment.closure.mh;
    mh =
        MethodHandles.collectArguments(
            next,
            next.type().parameterCount() - (mh.type().returnType() != void.class ? 1 : 0),
            mh);
    MethodType resultType = MethodType.methodType(mh.type().returnType(), paramsAtStep(step));
    int[] reorder = new int[mh.type().parameterCount()];
    for (int i = 0; i != reorder.length; ++i) {
      if (i < resultType.parameterCount()) {
        reorder[i] = i;
      } else {
        Var<?> var = assignment.closure.args.get(i - resultType.parameterCount());
        Preconditions.checkState(var.functionId == functionId);
        reorder[i] = var.nonVoidVarIndex;
      }
    }
    return MethodHandles.permuteArguments(mh, resultType, reorder);
  }

  public MethodHandle buildReturn(Var<?> returnValue) {
    MethodHandle mh;
    if (returnValue.type() != void.class) {
      mh = MhUtil.returnParam(paramsAtStep(assignments.size()), returnValue.nonVoidVarIndex);
    } else {
      mh = MhUtil.returnVoid(paramsAtStep(assignments.size()));
    }

    for (int i = assignments.size() - 1; i >= 0; --i) {
      mh = step(i, mh);
    }

    return mh;
  }

  public MethodHandle buildReturn(Closure<?> returnValue) {
    Var<?> val = assign(returnValue);
    return buildReturn(val);
  }

  public MethodHandle buildReturnVoid() {
    return buildReturn(Closure.constantVoid());
  }

  public Closure<Void> buildReturnVoidClosure() {
    throw new RuntimeException("not impl");
  }

  public Closure<Void> buildReturnClosure() {
    throw new RuntimeException("not impl");
  }
}
