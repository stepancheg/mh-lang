package com.github.stepancheg.mhlang;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class BuilderTest {

  @Test
  public void simpleReturn() throws Throwable {
    MhBuilder b = new MhBuilder();
    b.addParam(String.class);
    Var<Integer> p = b.addParam(int.class);
    b.addParam(boolean.class);
    MethodHandle mh = b.buildReturn(p);
    int r = (int) mh.invokeExact("", 17, true);
    assertEquals(17, r);
  }

  @Test
  public void voidHandle() throws Throwable {
    MhBuilder b = new MhBuilder();
    MethodHandle mh = b.buildReturnVoid();
    mh.invokeExact();
  }

  @Test
  public void sideEffectsInOrder() throws Throwable {
    MhBuilder b = new MhBuilder();
    ArrayList<String> l = new ArrayList<>();
    b.assign(Closure.runnable(() -> l.add("a")));
    b.assign(Closure.runnable(() -> l.add("b")));
    MethodHandle mh = b.buildReturnVoid();
    mh.invokeExact();
    assertEquals(ImmutableList.of("a", "b"), l);
  }
}
