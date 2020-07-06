package com.github.stepancheg.mhlang;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import org.junit.Test;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

public class ClosureTest {
  @Test
  public void function() throws Throwable {
    MhBuilder b = new MhBuilder();
    Var<String> p = b.addParam(String.class);
    Var r = b.assign(Closure.function(String.class, p, s -> s + "!"));
    MethodHandle mh = b.buildReturn(r);

    String x = (String) mh.invokeExact("a");
    assertEquals("a!", x);
  }

  @Test
  public void biFunction() throws Throwable {
    MhBuilder b = new MhBuilder();
    Var<String> p0 = b.addParam(String.class);
    Var<String> p1 = b.addParam(String.class);
    Var r = b.assign(Closure.biFunction(String.class, p0, p1, (s1, s2) -> s1 + "-" + s2));
    MethodHandle mh = b.buildReturn(r);

    String x = (String) mh.invokeExact("a", "b");
    assertEquals("a-b", x);
  }

  @Test
  public void predicate() throws Throwable {
    MhBuilder b = new MhBuilder();
    Var<String> p = b.addParam(String.class);
    MethodHandle mh = b.buildReturn(Closure.predicate(p, s -> !s.isEmpty()));
    boolean b1 = (boolean) mh.invokeExact("a");
    boolean b2 = (boolean) mh.invokeExact("");
    assertTrue(b1);
    assertFalse(b2);
  }

  @Test
  public void biPredicate() throws Throwable {
    MhBuilder b = new MhBuilder();
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
    MhBuilder b = new MhBuilder();
    int[] i = new int[] {0};
    MethodHandle mh = b.buildReturn(Closure.supplier(String.class, () -> "s" + i[0]++));

    String s0 = (String) mh.invokeExact();
    String s1 = (String) mh.invokeExact();

    assertEquals("s0", s0);
    assertEquals("s1", s1);
  }

  @Test
  public void runnable() throws Throwable {
    MhBuilder b = new MhBuilder();
    ArrayList<Object> l = new ArrayList<>();
    b.assign(Closure.runnable(() -> l.add("a")));
    MethodHandle mh = b.buildReturnVoid();
    mh.invokeExact();
    assertEquals(ImmutableList.of("a"), l);
  }

  @Test
  public void ifThenElse() throws Throwable {
    MhBuilder b = new MhBuilder();
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

    MhBuilder b = new MhBuilder();
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
    MhBuilder b = new MhBuilder();
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
  public void doWhileLoop() throws Throwable {
    MhBuilder b = new MhBuilder();
    Var<Integer> p = b.addParam(int.class);
    MethodHandle mh =
        b.buildReturn(
            Closure.doWhileLoop(
                Closure.constant("!"),
                v -> Closure.plus(v, Closure.constant(".")),
                v -> Closure.biPredicate(v, p, (vv, pp) -> vv.length() < pp)));

    assertEquals("!.", (String) mh.invokeExact(0));
    assertEquals("!..", (String) mh.invokeExact(3));
  }

  @Test
  public void throwException() throws Throwable {
    RuntimeException exception = new RuntimeException();

    MhBuilder b = new MhBuilder();
    Var<Boolean> cond = b.addParam(boolean.class);
    MethodHandle mh =
        b.buildReturn(
            Closure.ifThen(cond, Closure.throwException(void.class, Closure.constant(exception))));

    mh.invokeExact(false);

    try {
      mh.invokeExact(true);
      fail();
    } catch (Exception e) {
      assertSame(exception, e);
    }
  }

  @Test
  public void catchException() throws Throwable {
    MethodHandle mh =
        MhBuilder.shortcut(
            boolean.class,
            p -> {
              return Closure.catchException(
                  Closure.ifThenElse(
                      p,
                      Closure.throwException(
                          String.class,
                          Closure.supplier(Exception.class, () -> new RuntimeException("x"))),
                      Closure.constant("no")),
                  Exception.class,
                  e ->
                      Closure.plus(
                          Closure.constant("caught: "),
                          Closure.function(String.class, e, Throwable::getMessage)));
            });

    assertEquals("caught: x", (String) mh.invokeExact(true));
    assertEquals("no", (String) mh.invokeExact(false));
  }

  @Test
  public void tryFinally() throws Throwable {
    MethodHandle mh =
        MhBuilder.shortcut(
            boolean.class,
            p -> {
              return Closure.tryFinally(
                  Closure.ifThenElse(
                      p,
                      Closure.throwException(
                          String.class,
                          Closure.supplier(Exception.class, () -> new RuntimeException("x"))),
                      Closure.constant("no")),
                  (t, v) -> Closure.plus(v, Closure.constant("!")));
            });
    assertEquals("no!", (String) mh.invokeExact(false));
    try {
      String x = (String) mh.invokeExact(true);
      fail("returned: " + x);
    } catch (RuntimeException e) {
      assertEquals("x", e.getMessage());
    }
  }

  @Test
  public void countedLoop() throws Throwable {
    MhBuilder b = new MhBuilder();
    Var<Long> begin = b.addParam(long.class);
    Var<Long> end = b.addParam(long.class);
    MethodHandle mh =
        b.buildReturn(
            Closure.countedLoop(
                begin.asClosure().cast(int.class),
                end.asClosure().cast(int.class),
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
  public void iteratorLoop() throws Throwable {
    MhBuilder b = new MhBuilder();
    Var<List<Integer>> p = b.addParam(new TypeToken<List<Integer>>() {});
    MethodHandle mh =
        b.buildReturn(
            Closure.iteratorLoop(
                int.class,
                Closure.function(Iterator.class, p, List::iterator)
                    .cast(new TypeToken<Iterator<Integer>>() {}),
                Closure.constant(0),
                Closure::plus));

    assertEquals(10, (int) mh.invokeExact((List) ImmutableList.of(1, 2, 3, 4)));
  }

  @Test
  public void iterableLoop() throws Throwable {
    MhBuilder b = new MhBuilder();
    Var<List<Integer>> p = b.addParam(new TypeToken<List<Integer>>() {});
    MethodHandle mh =
        b.buildReturn(Closure.iterableLoop(int.class, p, Closure.constant(0), Closure::plus));

    assertEquals(10, (int) mh.invokeExact((List) ImmutableList.of(1, 2, 3, 4)));
  }

  @Test
  public void castBox() throws Throwable {
    MhBuilder b = new MhBuilder();
    Var<Integer> p = b.addParam(int.class);
    MethodHandle mh = b.buildReturn(p.asClosure().cast(Integer.class));

    Integer i = (Integer) mh.invokeExact(17);
    assertEquals(17, i.intValue());
  }

  @Test
  public void castUnbox() throws Throwable {
    MhBuilder b = new MhBuilder();
    Var<Integer> p = b.addParam(Integer.class);
    MethodHandle mh = b.buildReturn(p.asClosure().cast(Integer.class));

    Integer pi = 17;
    int i = (Integer) mh.invokeExact(pi);
    assertEquals(17, i);
  }

  @Test
  public void constructor() throws Throwable {
    Constructor<StringBuilder> constructor = StringBuilder.class.getConstructor(String.class);
    MhBuilder b = new MhBuilder();
    Var<String> p = b.addParam(String.class);
    MethodHandle mh = b.buildReturn(Closure.constructor(constructor, p));

    StringBuilder sb = (StringBuilder) mh.invokeExact("ab");
    assertEquals("ab", sb.toString());
  }

  @Test
  public void method() throws Throwable {
    Method m = String.class.getMethod("length");
    MhBuilder b = new MhBuilder();
    Var<String> p = b.addParam(String.class);
    MethodHandle mh = b.buildReturn(Closure.method(m, p));

    int l = (int) mh.invokeExact("ab");
    assertEquals(2, l);
  }

  @Test
  public void getArrayElement() throws Throwable {
    MhBuilder b = new MhBuilder();
    Var<long[]> pa = b.addParam(long[].class);
    Var<Integer> pi = b.addParam(int.class);
    MethodHandle mh = b.buildReturn(Closure.getArrayElement(pa, pi));
    assertEquals(20, (long) mh.invokeExact(new long[] {10, 20, 30}, 1));
  }

  @Test
  public void setArrayElement() throws Throwable {
    MhBuilder b = new MhBuilder();
    Var<String[]> pa = b.addParam(String[].class);
    Var<Integer> pi = b.addParam(int.class);
    Var<String> pv = b.addParam(String.class);
    MethodHandle mh = b.buildReturn(Closure.setArrayElement(pa, pi, pv));
    String[] a = new String[10];
    mh.invokeExact(a, 4, "a");
    assertEquals("a", a[4]);
  }

  @Test
  public void newArray() throws Throwable {
    MethodHandle mh = MhBuilder.shortcut(int.class, p -> Closure.newArray(long[].class, p));
    long[] aa = (long[]) mh.invokeExact(2);
    assertArrayEquals(new long[2], aa);
  }

  @Test
  public void arrayLength() throws Throwable {
    assertEquals(5, (int) Closure.arrayLength(Closure.constant(new double[5])).mh.invokeExact());
  }

  @Test
  public void fold() throws Throwable {
    Closure<Object> cl =
        Closure.fold(
            FunctionsMh.biFunction((String a, String b) -> a + b),
            Closure.constant(Object.class, "a"),
            Closure.constant(Object.class, "b"));
    assertEquals("ab", cl.mh.invokeExact());
  }

  @Test
  public void foldOrder() throws Throwable {
    // Assert fold evaluates the arguments in argument order
    ArrayList<String> calls = new ArrayList<>();
    Closure<Object> cl =
        Closure.fold(
            FunctionsMh.biFunction((String a, String b) -> a + b),
            Closure.supplier(
                Object.class,
                () -> {
                  calls.add("a");
                  return "a";
                }),
            Closure.supplier(
                Object.class,
                () -> {
                  calls.add("b");
                  return "b";
                }));
    assertEquals("ab", cl.mh.invokeExact());
    assertEquals(ImmutableList.of("a", "b"), calls);
  }

  @Test
  public void or() throws Throwable {
    Closure<Boolean> orTrue =
        Closure.or(
            Closure.constant(true),
            Closure.throwException(boolean.class, Closure.constant(new RuntimeException())));
    assertTrue((boolean) orTrue.mh.invokeExact());
  }

  @Test
  public void plus() throws Throwable {
    assertEquals(5, (int) Closure.plus(Closure.constant(2), Closure.constant(3)).mh.invokeExact());
    assertEquals(
        5L, (long) Closure.plus(Closure.constant(2L), Closure.constant(3L)).mh.invokeExact());
    assertEquals(
        "23", (String) Closure.plus(Closure.constant("2"), Closure.constant("3")).mh.invokeExact());
  }
}
