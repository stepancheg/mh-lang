package com.github.stepancheg.mhlang;

/**
 * Language expression node.
 *
 * <p>Can be either:
 * <ul>
 *     <li>Function parameter</li>
 *     <li>Local variable</li>
 *     <li>{@link Closure}</li>
 * </ul>
 */
public abstract class Expr<R> {

  /** Package-private: cannot be implemented outside. */
  Expr() {
  }

  /** Type of this expression. E. g. {@code int.class} for {@code a + 1}. */
  public abstract Class<R> type();

  /** This variable as a trivial closure. */
  public abstract Closure<R> asClosure();
}
