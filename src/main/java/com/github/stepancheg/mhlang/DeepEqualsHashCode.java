package com.github.stepancheg.mhlang;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Stream;

/** Generate deep {@code equals} and {@code hashCode} for a given class. */
public class DeepEqualsHashCode {
  private static final MethodHandle GET_CLASS;

  static {
    try {
      GET_CLASS =
          MethodHandles.lookup()
              .findVirtual(Object.class, "getClass", MethodType.methodType(Class.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Generate equals for given type.
   *
   * <p>Roughly equivalent to generating this function:
   *
   * <pre>
   *     boolean equals(T thiz, Object that) {
   *         if (other == null || this.getClass() != that.getClass()) {
   *             return false;
   *         }
   *         if (this == that) {
   *             return true;
   *         }
   *         for (field in T.fields) {
   *             if (field.isStatic()) {
   *                 continue;
   *             }
   *
   *             thisField = field.get(thiz);
   *             thatField = field.get(that);
   *             // there `!=` is `Object.equals` for objects or `!=` for primitives
   *             if (thisField != thatField {
   *                 return false;
   *             }
   *         }
   *         return true;
   *     }
   * </pre>
   */
  public static <T> MethodHandle deepEquals(Class<T> clazz, MethodHandles.Lookup lookup) {
    Preconditions.checkArgument(!clazz.isPrimitive());

    return MhBuilder.p2(clazz, Object.class, (thiz, that) -> deepEquals(lookup, thiz, that));
  }

  /** Closure version of {@link #deepEquals(Class, MethodHandles.Lookup)}. */
  public static <T> Closure<Boolean> deepEquals(
      MethodHandles.Lookup lookup, Var<T> thiz, Var<Object> that) {
    Class<T> clazz = thiz.type();
    ClosureBuilder allFieldsEqB = new ClosureBuilder();
    Closure<T> thatDowncasted = that.asClosure().cast(thiz.type());
    Closure<Boolean> allFieldsEq =
        allFieldsEqB.buildReturn(
            Closure.and(
                Arrays.stream(ClassUtil.nonStaticDeclaredFields(clazz))
                    .flatMap(
                        f -> {
                          Closure<Object> thisField = Closure.getField(f, thiz, lookup);
                          Closure<Object> thatField = Closure.getField(f, thatDowncasted, lookup);

                          return Stream.of(Closure.equals(thisField, thatField));
                        })
                    .collect(ImmutableList.toImmutableList())));

    return Closure.and(
        that.asClosure().isNotNull(),
        Closure.same(Closure.constant(clazz), Closure.fold(GET_CLASS, that)),
        Closure.or(Closure.same(thiz, that.asClosure().cast(clazz)), allFieldsEq));
  }

  /**
   * Generate {@code hashCode} for given type.
   *
   * <p>Roughly equivalent to generating this function:
   *
   * <pre>
   *     boolean hashCode(T thiz) {
   *         int r = 0;
   *         for (field in T.fields) {
   *             if (field.isStatic()) {
   *                 continue;
   *             }
   *
   *             thisField = field.get(thiz);
   *             // where `.hashCode` is null-safe and works for primitive types
   *             r = r * 31 + thisField.hashCode();
   *         }
   *         return r;
   *     }
   * </pre>
   */
  public static <T> MethodHandle deepHashCode(Class<T> clazz, MethodHandles.Lookup lookup) {
    return MhBuilder.p1(clazz, thiz -> deepHashCode(lookup, thiz));
  }

  /** Closure version of {@link #deepHashCode(Class, MethodHandles.Lookup)}. */
  public static <T> Closure<Integer> deepHashCode(MethodHandles.Lookup lookup, Var<T> thiz) {
    Closure<Integer> hash = Closure.constant(0);
    for (Field field : ClassUtil.nonStaticDeclaredFields(thiz.type())) {
      Closure<Object> thisField = Closure.getField(field, thiz, lookup);
      Closure<Integer> fieldHash = Closure.hashCode(thisField);

      hash = Closure.mul(hash, Closure.constant(31));
      hash = Closure.plus(hash, fieldHash);
    }
    return hash;
  }
}
