package com.github.stepancheg.mhlang;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class ClosureTest {
  @Test
  public void function() throws Throwable {
    Builder b = new Builder();
    Var<String> p = b.addParam(String.class);
    Var r = b.assign(Closure.function(String.class, p, s -> s + "!"));
    MethodHandle mh = b.buildReturn(r);

    String x = (String) mh.invokeExact("a");
    assertEquals("a!", x);
  }

  @Test
  public void biFunction() throws Throwable {
    Builder b = new Builder();
    Var<String> p0 = b.addParam(String.class);
    Var<String> p1 = b.addParam(String.class);
    Var r = b.assign(Closure.biFunction(String.class, p0, p1, (s1, s2) -> s1 + "-" + s2));
    MethodHandle mh = b.buildReturn(r);

    String x = (String) mh.invokeExact("a", "b");
    assertEquals("a-b", x);
  }

  @Test
  public void predicate() throws Throwable {
    Builder b = new Builder();
    Var<String> p = b.addParam(String.class);
    MethodHandle mh = b.buildReturn(Closure.predicate(p, s -> !s.isEmpty()));
    boolean b1 = (boolean) mh.invokeExact("a");
    boolean b2 = (boolean) mh.invokeExact("");
    assertTrue(b1);
    assertFalse(b2);
  }

  @Test
  public void biPredicate() throws Throwable {
    Builder b = new Builder();
    Var<String> p0 = b.addParam(String.class);
    Var<String> p1 = b.addParam(String.class);
    Var<Boolean> shorter = b.assign(Closure.biPredicate(p0, p1, (x, y) -> x.length() < y.length()));
    MethodHandle mh = b.buildReturn(shorter);

    boolean r = (boolean) mh.invokeExact("x", "yy");
    boolean t = (boolean) mh.invokeExact("xx", "y");

    assertTrue(r);
    assertFalse(t);
  }

  @Test
  public void supplier() throws Throwable {
    Builder b = new Builder();
    int[] i = new int[] {0};
    MethodHandle mh = b.buildReturn(Closure.supplier(String.class, () -> "s" + i[0]++));

    String s0 = (String) mh.invokeExact();
    String s1 = (String) mh.invokeExact();

    assertEquals("s0", s0);
    assertEquals("s1", s1);
  }

  @Test
  public void runnable() throws Throwable {
    Builder b = new Builder();
    ArrayList<Object> l = new ArrayList<>();
    b.assign(Closure.runnable(() -> l.add("a")));
    MethodHandle mh = b.buildReturnVoid();
    mh.invokeExact();
    assertEquals(ImmutableList.of("a"), l);
  }

  @Test
  public void ifThenElse() throws Throwable {
    Builder b = new Builder();
    Var<Boolean> p0 = b.addParam(boolean.class);
    Var<String> p1 = b.addParam(String.class);
    Var<String> p2 = b.addParam(String.class);
    Var<String> r = b.assign(Closure.ifThenElse(p0, p1, p2));
    MethodHandle mh = b.buildReturn(r);

    String x = (String) mh.invokeExact(true, "x", "y");
    String y = (String) mh.invokeExact(false, "x", "y");

    assertEquals("x", x);
    assertEquals("y", y);
  }

  @Test
  public void ifThen() throws Throwable {
    int[] i = new int[1];

    Builder b = new Builder();
    Var<Boolean> p = b.addParam(boolean.class);
    b.assign(Closure.ifThen(p, Closure.runnable(() -> ++i[0])));
    MethodHandle mh = b.buildReturnVoid();

    mh.invokeExact(false);
    assertEquals(0, i[0]);
    mh.invokeExact(true);
    assertEquals(1, i[0]);
  }

  @Test
  public void whileLoop() throws Throwable {
    Builder b = new Builder();
    Var<Integer> p = b.addParam(int.class);
    MethodHandle mh =
        b.buildReturn(
            Closure.whileLoop(
                Closure.constant(String.class, ""),
                v -> Closure.biFunction(boolean.class, p, v, (l, s) -> s.length() < l),
                v -> Closure.function(String.class, v, s -> s + "a")));
    assertEquals("aaa", (String) mh.invokeExact(3));
  }

  @Test
  public void countedLoop() throws Throwable {
    Builder b = new Builder();
    Var<Long> begin = b.addParam(long.class);
    Var<Long> end = b.addParam(long.class);
    MethodHandle mh =
        b.buildReturn(
            Closure.countedLoop(
                begin.cast(int.class),
                end.cast(int.class),
                Closure.constant(String.class, ""),
                (vv, vi) ->
                    Closure.biFunction(
                        String.class,
                        vv,
                        vi,
                        (v, i) -> {
                          return v + (v.isEmpty() ? "" : " ") + i;
                        })));
    String r = (String) mh.invokeExact(3L, 7L);
    assertEquals("3 4 5 6", r);
  }

  @Test
  public void castBox() throws Throwable {
    Builder b = new Builder();
    Var<Integer> p = b.addParam(int.class);
    MethodHandle mh = b.buildReturn(p.cast(Integer.class));

    Integer i = (Integer) mh.invokeExact(17);
    assertEquals(17, i.intValue());
  }

  @Test
  public void castUnbox() throws Throwable {
    Builder b = new Builder();
    Var<Integer> p = b.addParam(Integer.class);
    MethodHandle mh = b.buildReturn(p.cast(Integer.class));

    Integer pi = 17;
    int i = (Integer) mh.invokeExact(pi);
    assertEquals(17, i);
  }

  @Test
  public void constructor() throws Throwable {
    Constructor<StringBuilder> constructor = StringBuilder.class.getConstructor(String.class);
    Builder b = new Builder();
    Var<String> p = b.addParam(String.class);
    MethodHandle mh = b.buildReturn(Closure.constructor(constructor, p));

    StringBuilder sb = (StringBuilder) mh.invokeExact("ab");
    assertEquals("ab", sb.toString());
  }

  @Test
  public void method() throws Throwable {
    Method m = String.class.getMethod("length");
    Builder b = new Builder();
    Var<String> p = b.addParam(String.class);
    MethodHandle mh = b.buildReturn(Closure.method(m, p));

    int l = (int) mh.invokeExact("ab");
    assertEquals(2, l);
  }

  @Test
  public void getArrayElement() throws Throwable {
    Builder b = new Builder();
    Var<long[]> pa = b.addParam(long[].class);
    Var<Integer> pi = b.addParam(int.class);
    MethodHandle mh = b.buildReturn(Closure.getArrayElement(pa, pi));
    assertEquals(20, (long) mh.invokeExact(new long[] { 10, 20, 30 }, 1));
  }

  @Test
  public void setArrayElement() throws Throwable {
    Builder b = new Builder();
    Var<String[]> pa = b.addParam(String[].class);
    Var<Integer> pi = b.addParam(int.class);
    Var<String> pv = b.addParam(String.class);
    MethodHandle mh = b.buildReturn(Closure.setArrayElement(pa, pi, pv));
    String[] a = new String[10];
    mh.invokeExact(a, 4, "a");
    assertEquals("a", a[4]);
  }

  @Test
  public void fold() throws Throwable {
    Closure<Object> cl = Closure.fold(FunctionsMh.biFunction((String a, String b) -> a + b), Closure.constant(Object.class, "a"), Closure.constant(Object.class, "b"));
    assertEquals("ab", cl.mh.invokeExact());
  }

  @Test
  public void foldOrder() throws Throwable {
    // Assert fold evaluates the arguments in argument order
    ArrayList<String> calls = new ArrayList<>();
    Closure<Object> cl = Closure.fold(FunctionsMh.biFunction((String a, String b) -> a + b),
      Closure.supplier(Object.class, () -> { calls.add("a"); return "a"; }),
      Closure.supplier(Object.class, () -> { calls.add("b"); return "b"; })
      );
    assertEquals("ab", cl.mh.invokeExact());
    assertEquals(ImmutableList.of("a", "b"), calls);
  }
}
