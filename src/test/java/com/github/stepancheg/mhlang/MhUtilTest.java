package com.github.stepancheg.mhlang;

import org.junit.Test;

import java.lang.invoke.MethodHandle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MhUtilTest {
  @Test
  public void returnParam() throws Throwable {
    MethodHandle mh0 = MhUtil.returnParam(new Class[] {int.class, String.class, boolean.class}, 0);
    int r0 = (int) mh0.invokeExact(5, "a", true);
    assertEquals(5, r0);

    MethodHandle mh2 = MhUtil.returnParam(new Class[] {int.class, String.class, boolean.class}, 2);
    boolean r2 = (boolean) mh2.invokeExact(5, "a", true);
    assertTrue(r2);
  }
}
