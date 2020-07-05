package com.github.stepancheg.mhlang;

import com.google.common.base.Preconditions;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;

public class Builder {

  private final long functionId = FunctionId.nextId();

  private ArrayList<Var.Param<?>> params = new ArrayList<>();

  private ArrayList<Var.Invoke<?>> assignments = new ArrayList<>();
  private ArrayList<Var.Invoke<?>> nonVoidAssignments = new ArrayList<>();

  private static class Step {
    private final Class<?>[] signatureWithout;
    private final Var.Invoke<?> assignment;
    private final Class<?>[] signatureWith;

    public Step(Class<?>[] signatureWithout, Var.Invoke<?> assignment) {
      this.signatureWithout = signatureWithout;
      this.assignment = assignment;
      this.signatureWith = assignment.type() != void.class ?
        ArrayUtil.concat(signatureWithout, new Class<?>[] { assignment.type() }) : signatureWithout;
    }
  }

  private ArrayList<Step> steps = new ArrayList<>();

  public Builder() {}

  private int nextVarId() {
    return params.size() + assignments.size();
  }

  private boolean bodyStarted() {
    return !assignments.isEmpty();
  }

  public <T> Var<T> addParam(Class<T> type) {
    Preconditions.checkState(type != void.class, "Parameter type must not be void");
    Preconditions.checkState(
        !bodyStarted(), "Cannot add function parameter after function body started");
    Var.Param<T> param = new Var.Param<>(functionId, nextVarId(), type);
    params.add(param);
    return param;
  }

  private Class<?>[] currentParams() {
    if (steps.isEmpty()) {
      return params.stream().map(Var.Param::type).toArray(Class<?>[]::new);
    } else {
      return steps.get(steps.size() - 1).signatureWith;
    }
  }

  public <R> Var<R> assign(Closure<R> closure) {
    Var.Invoke<R> var = new Var.Invoke<>(functionId, nextVarId(), closure);
    assignments.add(var);
    if (var.type() != void.class) {
      nonVoidAssignments.add(var);
    }
    steps.add(new Step(currentParams(), var));
    return var;
  }

  private int varIndex(Var<?> var) {
    int i = 0;
    for (Var.Param<?> param : params) {
      if (var == param) {
        return i;
      }
      ++i;
    }
    for (Var.Invoke<?> assignment : nonVoidAssignments) {
      if (assignment == var) {
        return i;
      }
      ++i;
    }
    throw new IllegalStateException();
  }

  private MethodHandle step(int stepIndex, MethodHandle next) {
    Preconditions.checkArgument(stepIndex >= 0 && stepIndex < steps.size());

    Step step = steps.get(stepIndex);

    Var.Invoke<?> assignment = step.assignment;
    MethodHandle mh = assignment.closure.mh;
    mh =
        MethodHandles.collectArguments(
            next,
            next.type().parameterCount() - (mh.type().returnType() != void.class ? 1 : 0),
            mh);
    MethodType resultType = MethodType.methodType(mh.type().returnType(), step.signatureWithout);
    int[] reorder = new int[mh.type().parameterCount()];
    for (int i = 0; i != reorder.length; ++i) {
      if (i < resultType.parameterCount()) {
        reorder[i] = i;
      } else {
        Var<?> var = assignment.closure.args.get(i - resultType.parameterCount());
        Preconditions.checkState(var.functionId == functionId);
        reorder[i] = varIndex(var);
      }
    }
    return MethodHandles.permuteArguments(mh, resultType, reorder);
  }

  public MethodHandle buildReturn(Var<?> returnValue) {
    MethodHandle mh;
    if (returnValue.type() != void.class) {
      mh = MhUtil.returnParam(currentParams(), varIndex(returnValue));
    } else {
      mh = MhUtil.returnVoid(currentParams());
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
