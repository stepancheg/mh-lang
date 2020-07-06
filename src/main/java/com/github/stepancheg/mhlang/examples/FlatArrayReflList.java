package com.github.stepancheg.mhlang.examples;

import org.objenesis.instantiator.ObjectInstantiator;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.AbstractList;
import java.util.Arrays;

/**
 * Reflective implementation of {@link com.github.stepancheg.mhlang.examples.FlatArrayMhList}.
 *
 * <p>Implemented to compare performance of plain reflection vs MH reflection.
 */
public class FlatArrayReflList<T> extends AbstractList<T> {

  private final Factory<T> factory;

  /** Array of arrays. */
  private Object[] fields;
  /** Current size. */
  private int size = 0;
  /** Current capacity. */
  private int capacity = 0;

  private FlatArrayReflList(Factory<T> factory, Object[] fields) {
    this.factory = factory;
    this.fields = fields;
  }

  @Override
  public T get(int index) {
    if (index < 0 || index >= size) {
      throw new IllegalArgumentException();
    }

    T t = factory.instantiator.newInstance();
    Field[] fields1 = factory.fields;
    for (int i = 0; i < fields1.length; i++) {
      Field field = fields1[i];
      try {
        field.set(t, Array.get(fields[i], index));
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    return t;
  }

  @Override
  public boolean add(T t) {
    if (size == capacity) {
      doubleCapacity();
    }

    Field[] fields1 = factory.fields;
    for (int i = 0; i < fields1.length; i++) {
      Field field = fields1[i];
      try {
        Array.set(fields[i], size, field.get(t));
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    size += 1;
    return true;
  }

  private void doubleCapacity() {
    int newCap = Math.max(size * 2, 10);
    Field[] fields1 = factory.fields;
    for (int i = 0; i < fields1.length; i++) {
      Object array = fields[i];
      Object newArray = copyOf(array, newCap);
      fields[i] = newArray;
    }
    this.capacity = newCap;
  }

  private static Object copyOf(Object array, int newCap) {
    return Array.newInstance(array.getClass().getComponentType(), newCap);
  }

  @Override
  public int size() {
    return size;
  }

  public static class Factory<T> {
    private final ObjectInstantiator<T> instantiator;
    private final Field[] fields;
    private final Object[] arrays;

    public Factory(Class<T> tClass) {

      Field[] fields = tClass.getDeclaredFields();

      for (Field field : fields) {
        field.setAccessible(true);
      }

      this.fields = fields;
      this.arrays = Arrays.stream(fields).map(f -> Array.newInstance(f.getType(), 0)).toArray();
      instantiator = new StdInstantiatorStrategy().newInstantiatorOf(tClass);
    }

    public FlatArrayReflList<T> newArrayList() {
      return new FlatArrayReflList<>(this, arrays.clone());
    }

    private static Class<?> fieldArrayComponentType(Field field) {
      if (field.getType().isPrimitive()) {
        return field.getType();
      } else {
        return Object.class;
      }
    }

    private static Class<?> fieldArrayType(Field field) {
      return Array.newInstance(fieldArrayComponentType(field), 0).getClass();
    }
  }
}
