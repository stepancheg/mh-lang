package com.github.stepancheg.mhlang;

import org.junit.Test;

import java.lang.invoke.MethodHandle;

import static org.junit.Assert.*;

public class ClosureBuilderTest {

  @Test
  public void test() throws Throwable {
    MhBuilder b = new MhBuilder();
    Var<Boolean> bp = b.addParam(boolean.class);
    Var<Integer> ip = b.addParam(int.class);

    ClosureBuilder thenBlock = new ClosureBuilder();
    Var<Integer> v = thenBlock.assign(Closure.plus(ip, ip));
    Closure<Integer> thenCl = thenBlock.buildReturn(v);

    MethodHandle mh = b.buildReturn(Closure.ifThenElse(bp, thenCl, Closure.constant(0)));

    assertEquals(0, (int) mh.invokeExact(false, 10));
    assertEquals(20, (int) mh.invokeExact(true, 10));
  }

}
