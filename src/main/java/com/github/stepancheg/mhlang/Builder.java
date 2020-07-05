package com.github.stepancheg.mhlang;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;

public abstract class Builder {

  protected final long functionId = FunctionId.nextId();

  protected abstract ImmutableList<Var<?>> paramsOrOuterVars();

  private ArrayList<Var.Invoke<?>> assignments = new ArrayList<>();

  public Builder() {}

  private ArrayList<Var.Invoke<?>> nonVoidAssignments = new ArrayList<>();

  private static class Step {
    private final Class<?>[] localVarsWithout;
    private final Var.Invoke<?> assignment;
    private final Class<?>[] localVarsWith;

    Step(Step prev, Var.Invoke<?> assignment) {
      this.localVarsWithout = prev != null ? prev.localVarsWith : new Class<?>[0];
      this.assignment = assignment;
      this.localVarsWith =
          assignment.type() != void.class
              ? ArrayUtil.concat(localVarsWithout, new Class<?>[] {assignment.type()})
              : localVarsWithout;
    }

  }

  private ArrayList<Step> steps = new ArrayList<>();

  protected boolean bodyStarted() {
    return !assignments.isEmpty();
  }

  private Class<?>[] currentParams() {
    Class<?>[] params = this.paramsOrOuterVars().stream().map(Var::type).toArray(Class<?>[]::new);
    if (steps.isEmpty()) {
      return params;
    } else {
      return ArrayUtil.concat(params, steps.get(steps.size() - 1).localVarsWith);
    }
  }

  protected abstract void addOuterVar(Var<?> outerVar);

  public <R> Var<R> assign(Closure<R> closure) {
    for (Var<?> arg : closure.args) {
      if (arg.functionId != functionId) {
        addOuterVar(arg);
      }
    }

    Var.Invoke<R> var = new Var.Invoke<>(functionId, assignments.size(), closure);
    assignments.add(var);
    if (var.type() != void.class) {
      nonVoidAssignments.add(var);
    }
    steps.add(new Step(steps.isEmpty() ? null : steps.get(steps.size() - 1), var));
    return var;
  }

  private int varIndex(Var<?> var) {
    int i = 0;
    for (Var<?> param : paramsOrOuterVars()) {
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
    MethodType resultType = MethodType.methodType(mh.type().returnType(),
      ArrayUtil.concat(paramsOrOuterVars().stream().map(Var::type).toArray(Class<?>[]::new), step.localVarsWithout));
    int[] reorder = new int[mh.type().parameterCount()];
    for (int i = 0; i != reorder.length; ++i) {
      if (i < resultType.parameterCount()) {
        reorder[i] = i;
      } else {
        Var<?> var = assignment.closure.args.get(i - resultType.parameterCount());
        reorder[i] = varIndex(var);
      }
    }
    return MethodHandles.permuteArguments(mh, resultType, reorder);
  }

  protected <R> Closure<R> buildReturnImpl(Var<R> returnValue) {
    MethodHandle mh;
    if (returnValue.type() != void.class) {
      mh = MhUtil.returnParam(currentParams(), varIndex(returnValue));
    } else {
      mh = MhUtil.returnVoid(currentParams());
    }

    for (int i = assignments.size() - 1; i >= 0; --i) {
      mh = step(i, mh);
    }

    return new Closure<>(mh, paramsOrOuterVars());
  }
}
