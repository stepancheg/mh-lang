package com.github.stepancheg.mhlang;

import com.google.common.base.Preconditions;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class Builder {

  private static final AtomicLong FUNCTION_ID = new AtomicLong();

  private final long functionId = FUNCTION_ID.addAndGet(1);

  private ArrayList<Var.Param<?>> params = new ArrayList<>();

  private ArrayList<Var<?>> assignments = new ArrayList<>();

  private ArrayList<Var<?>> vars = new ArrayList<>();
  private ArrayList<Var<?>> nonVoidVars = new ArrayList<>();

  public Builder() {}

  private int nextVarId() {
    return vars.size();
  }

  private int nextNonVoidIndex() {
    return nonVoidVars.size();
  }

  private boolean bodyStarted() {
    return vars.size() != params.size();
  }

  public <T> Var<T> addParam(Class<T> type) {
    Preconditions.checkState(type != void.class, "Parameter type must not be void");
    Preconditions.checkState(
        !bodyStarted(), "Cannot add function parameter after function body started");
    Var.Param<T> param = new Var.Param<>(functionId, nextVarId(), nextNonVoidIndex(), type);
    params.add(param);
    vars.add(param);
    nonVoidVars.add(param);
    return param;
  }

  public <R> Var<R> assign(Closure<R> closure) {
    Var.Invoke<R> var = new Var.Invoke<>(functionId, nextVarId(), nextNonVoidIndex(), closure);
    vars.add(var);
    if (var.type() != void.class) {
      nonVoidVars.add(var);
    }
    assignments.add(var);
    return var;
  }

  private Class<?>[] mtAtStep(int step) {
    int count;
    if (step == vars.size() - params.size()) {
      count = nonVoidVars.size();
    } else {
      count = vars.get(step + params.size()).nonVoidVarIndex;
    }
    return nonVoidVars.stream().limit(count).map(Var::type).toArray(Class<?>[]::new);
  }

  private MethodHandle step(int step, MethodHandle next) {
    return assignments
        .get(step)
        .match(
            new Var.Matcher<>() {
              @Override
              public MethodHandle param(int i) {
                throw new IllegalStateException();
              }

              @Override
              public MethodHandle invoke(Closure<?> closure) {
                MethodHandle mh = closure.mh;
                mh =
                    MethodHandles.collectArguments(
                        next,
                        next.type().parameterCount()
                            - (mh.type().returnType() != void.class ? 1 : 0),
                        mh);
                MethodType resultType =
                    MethodType.methodType(mh.type().returnType(), mtAtStep(step));
                int[] reorder = new int[mh.type().parameterCount()];
                for (int i = 0; i != reorder.length; ++i) {
                  if (i < resultType.parameterCount()) {
                    reorder[i] = i;
                  } else {
                    reorder[i] = closure.args.get(i - resultType.parameterCount()).varId;
                  }
                }
                return MethodHandles.permuteArguments(mh, resultType, reorder);
              }
            });
  }

  public MethodHandle buildReturn(Var<?> returnValue) {
    MethodHandle mh;
    if (returnValue.type() != void.class) {
      mh = MhUtil.returnParam(mtAtStep(assignments.size()), returnValue.varId);
    } else {
      mh = MhUtil.returnVoid(mtAtStep(assignments.size()));
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
}
