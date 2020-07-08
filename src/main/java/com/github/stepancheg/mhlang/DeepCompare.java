package com.github.stepancheg.mhlang;

import com.google.common.base.Preconditions;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

/** Utility to build a comparator by lexicographically comparing fields of a class. */
public class DeepCompare {

  private interface FakeComparable extends Comparable<FakeComparable> {}

  public static <A> MethodHandle deepCompare(Class<A> at, MethodHandles.Lookup lookup) {
    return MhBuilder.p2(at, at, (thiz, that) -> deepCompare(thiz, that, lookup));
  }

  @SuppressWarnings("unchecked")
  public static <A> Closure<Integer> deepCompare(
      Var<A> thiz, Var<A> that, MethodHandles.Lookup lookup) {
    Preconditions.checkArgument(thiz.type() == that.type());

    Class<A> t = thiz.type();
    if (t.isPrimitive()) {
      return Closure.compare((Expr<FakeComparable>) thiz, (Expr<FakeComparable>) that);
    }

    Closure<Integer> r = Closure.constant(0);

    // NOTE: this implementation assumes fields are returned in declaration order,
    //  but JVM does not guarantee that.
    Field[] nonStaticDeclaredFields = ClassUtil.nonStaticDeclaredFields(t);
    for (int i = nonStaticDeclaredFields.length - 1; i >= 0; i--) {
      Field field = nonStaticDeclaredFields[i];

      Closure<FakeComparable> thizField = Closure.getField(field, thiz, lookup);
      Closure<FakeComparable> thatField = Closure.getField(field, that, lookup);

      ClosureBuilder b = new ClosureBuilder();
      Var<Integer> cmp = b.assign(Closure.compare(thizField, thatField));
      r = b.buildReturn(Closure.ifThenElse(Closure.equals(cmp, Closure.constant(0)), r, cmp));
    }

    // if (this == that) {
    //   return 0;
    // } else {
    //   ...
    // }
    return Closure.ifThenElse(Closure.same(thiz, that), Closure.constant(0), r);
  }
}
