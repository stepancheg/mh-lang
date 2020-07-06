package com.github.stepancheg.mhlang.examples;

import com.github.stepancheg.mhlang.Closure;
import com.github.stepancheg.mhlang.MhBuilder;
import com.github.stepancheg.mhlang.Var;
import org.objenesis.instantiator.ObjectInstantiator;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.AbstractList;
import java.util.Arrays;

/**
 * Method handles implementation of struct of arrays pattern.
 *
 * <p>Note this implements the same logic as {@link FlatArrayReflList}, but it is 20 times faster.
 */
public class FlatArrayMhList<T> extends AbstractList<T> {

  private final Factory<T> factory;

  /** Array of arrays. */
  private Object[] fields;
  /** Current size. */
  private int size = 0;
  /** Current capacity. */
  private int capacity = 0;

  private FlatArrayMhList(Factory<T> factory, Object[] fields) {
    this.factory = factory;
    this.fields = fields;
  }

  @SuppressWarnings("unchecked")
  @Override
  public T get(int index) {
    if (index < 0 || index >= size) {
      throw new IllegalArgumentException();
    }
    try {
      return (T) factory.getImpl.invokeExact(fields, index);
    } catch (Throwable throwable) {
      throw new RuntimeException(throwable);
    }
  }

  @Override
  public boolean add(T t) {
    if (size == capacity) {
      doubleCapacity();
    }
    try {
      factory.setImpl.invokeExact(fields, size, t);
      size += 1;
      return true;
    } catch (Throwable throwable) {
      throw new RuntimeException(throwable);
    }
  }

  private void doubleCapacity() {
    try {
      int newCap = Math.max(size * 2, 10);
      factory.resize.invokeExact(fields, newCap);
      this.capacity = newCap;
    } catch (Throwable throwable) {
      throw new RuntimeException(throwable);
    }
  }

  @Override
  public int size() {
    return size;
  }

  public static class Factory<T> {
    private final MethodHandle getImpl;
    private final MethodHandle setImpl;
    private final MethodHandle resize;
    private final Object[] fields;

    public Factory(Class<T> tClass) {

      Field[] fields = tClass.getDeclaredFields();

      for (Field field : fields) {
        field.setAccessible(true);
      }

      getImpl = getImpl(tClass, fields);
      setImpl = setImpl(tClass, fields);
      resize = resize(fields);

      this.fields = Arrays.stream(fields).map(f -> Array.newInstance(f.getType(), 0)).toArray();
    }

    public FlatArrayMhList<T> newArrayList() {
      return new FlatArrayMhList<>(this, fields.clone());
    }

    private static final MethodHandle NEW_INSTANCE;

    static {
      try {
        NEW_INSTANCE =
            MethodHandles.publicLookup()
                .findVirtual(
                    ObjectInstantiator.class, "newInstance", MethodType.methodType(Object.class));
      } catch (NoSuchMethodException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    private static <T> MethodHandle getImpl(Class<T> tClass, Field[] fields) {
      ObjectInstantiator<T> instantiator = new StdInstantiatorStrategy().newInstantiatorOf(tClass);

      MhBuilder b = new MhBuilder();
      Var<Object[]> pArrays = b.addParam(Object[].class);
      Var<Integer> pI = b.addParam(int.class);

      // Instance to be returned
      Var<T> instanceObject =
          b.assign(new Closure<>(MethodHandles.insertArguments(NEW_INSTANCE, 0, instantiator)));
      // Downcast
      Var<T> instance = b.assign(instanceObject.asClosure().cast(tClass));

      // For each field...
      for (int fieldIndex = 0; fieldIndex < fields.length; fieldIndex++) {
        Field field = fields[fieldIndex];
        Class<?> fieldArrayType = fieldArrayType(field);
        Closure<Integer> fieldIndexExpr = Closure.constant(int.class, fieldIndex);
        // Get array containing a field, e. g. for `int` field, it is `int[]` array.
        Closure<?> fieldArray =
            Closure.getArrayElement(pArrays, fieldIndexExpr).cast(fieldArrayType);
        Closure<?> fieldValue = Closure.getArrayElement(fieldArray, pI).cast(field.getType());
        b.assign(Closure.setField(field, instance, fieldValue));
      }

      return b.buildReturn(instanceObject);
    }

    @SuppressWarnings("unchecked")
    private static <T> MethodHandle setImpl(Class<T> tClass, Field[] fields) {
      MhBuilder b = new MhBuilder();
      Var<Object[]> pArrays = b.addParam(Object[].class);
      Var<Integer> pI = b.addParam(int.class);
      Var<T> pInstance = (Var<T>) b.addParam(Object.class);
      Var<T> pInstanceTyped = b.assign(pInstance.asClosure().cast(tClass));
      for (int fieldIndex = 0; fieldIndex < fields.length; fieldIndex++) {
        Field field = fields[fieldIndex];
        Class<?> fieldArrayType = fieldArrayType(field);
        Class<?> fieldArrayComponentType = fieldArrayComponentType(field);
        Closure<Integer> iv = Closure.constant(int.class, fieldIndex);
        Closure<?> fieldArray = Closure.getArrayElement(pArrays, iv).cast(fieldArrayType);
        Closure<?> fieldValue =
            Closure.getField(field, pInstanceTyped).cast(fieldArrayComponentType);
        b.assign(Closure.setArrayElement(fieldArray, pI, fieldValue));
      }
      return b.buildReturnVoid();
    }

    private static MethodHandle resize(Field[] fields) {
      MhBuilder b = new MhBuilder();
      Var<Object[]> pArrays = b.addParam(Object[].class);
      Var<Integer> pNewSize = b.addParam(int.class);
      for (int fi = 0; fi < fields.length; fi++) {
        Field field = fields[fi];
        Class<?> fieldArrayType = fieldArrayType(field);

        Closure<Integer> iv = Closure.constant(int.class, fi);
        Closure<?> fieldArray = Closure.getArrayElement(pArrays, iv).cast(fieldArrayType);
        MethodHandle copyOfMh;
        try {
          copyOfMh =
              MethodHandles.publicLookup()
                  .findStatic(
                      Arrays.class,
                      "copyOf",
                      MethodType.methodType(fieldArrayType, fieldArrayType, int.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
          throw new RuntimeException(e);
        }
        Closure<?> fieldArrayResized =
            Closure.fold(copyOfMh, fieldArray, pNewSize).cast(Object.class);
        b.assign(Closure.setArrayElement(pArrays, iv, fieldArrayResized));
      }
      return b.buildReturnVoid();
    }

    private static Class<?> fieldArrayComponentType(Field field) {
      if (field.getType().isPrimitive()) {
        return field.getType();
      } else {
        // Note we store any object fields in Object[] arrays to avoid paying for downcasting
        return Object.class;
      }
    }

    private static Class<?> fieldArrayType(Field field) {
      return Array.newInstance(fieldArrayComponentType(field), 0).getClass();
    }
  }
}
