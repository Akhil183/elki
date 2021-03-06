package de.lmu.ifi.dbs.elki.data;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.BitSet;
import java.util.Comparator;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;

/**
 * Utility functions for use with vectors.
 * 
 * Note: obviously, many functions are class methods or database related.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses NumberVector
 */
public final class VectorUtil {
  /**
   * Fake constructor. Do not instantiate, use static methods.
   */
  private VectorUtil() {
    // Do not instantiate - utility class.
  }

  /**
   * Return the range across all dimensions. Useful in particular for time
   * series.
   * 
   * @param vec Vector to process.
   * @return [min, max]
   */
  public static DoubleMinMax getRangeDouble(NumberVector<?> vec) {
    DoubleMinMax minmax = new DoubleMinMax();

    for(int i = 0; i < vec.getDimensionality(); i++) {
      minmax.put(vec.doubleValue(i));
    }

    return minmax;
  }

  /**
   * Produce a new vector based on random numbers in [0:1].
   * 
   * @param factory Vector factory
   * @param dim desired dimensionality
   * @param r Random generator
   * @param <V> vector type
   * @return new instance
   */
  public static <V extends NumberVector<?>> V randomVector(NumberVector.Factory<V, ?> factory, int dim, Random r) {
    return factory.newNumberVector(MathUtil.randomDoubleArray(dim, r));
  }

  /**
   * Produce a new vector based on random numbers in [0:1].
   * 
   * @param factory Vector factory
   * @param dim desired dimensionality
   * @param <V> vector type
   * @return new instance
   */
  public static <V extends NumberVector<?>> V randomVector(NumberVector.Factory<V, ?> factory, int dim) {
    return randomVector(factory, dim, new Random());
  }

  /**
   * Compute the angle for sparse vectors.
   * 
   * @param v1 First vector
   * @param v2 Second vector
   * @return angle
   */
  public static double angleSparse(SparseNumberVector<?> v1, SparseNumberVector<?> v2) {
    // TODO: exploit precomputed length, when available?
    // Length of first vector
    double l1 = 0., l2 = 0., cross = 0.;
    int i1 = v1.iter(), i2 = v2.iter();
    while(v1.iterValid(i1) && v2.iterValid(i2)) {
      final int d1 = v1.iterDim(i1), d2 = v2.iterDim(i2);
      if(d1 < d2) {
        final double val = v1.iterDoubleValue(i1);
        l1 += val * val;
        i1 = v1.iterAdvance(i1);
      }
      else if(d2 < d1) {
        final double val = v2.iterDoubleValue(i2);
        l2 += val * val;
        i2 = v2.iterAdvance(i2);
      }
      else { // d1 == d2
        final double val1 = v1.iterDoubleValue(i1);
        final double val2 = v2.iterDoubleValue(i2);
        l1 += val1 * val1;
        l2 += val2 * val2;
        cross += val1 * val2;
        i1 = v1.iterAdvance(i1);
        i2 = v2.iterAdvance(i2);
      }
    }
    while(v1.iterValid(i1)) {
      final double val = v1.iterDoubleValue(i1);
      l1 += val * val;
      i1 = v1.iterAdvance(i1);
    }
    while(v2.iterValid(i2)) {
      final double val = v2.iterDoubleValue(i2);
      l2 += val * val;
      i2 = v2.iterAdvance(i2);
    }

    final double a = cross / (Math.sqrt(l1) * Math.sqrt(l2));
    return (a > 1.) ? 1. : a;
  }

  /**
   * Compute the angle between two vectors.
   * 
   * @param v1 first vector
   * @param v2 second vector
   * @param o Origin
   * @return Angle
   */
  public static double angle(NumberVector<?> v1, NumberVector<?> v2, Vector o) {
    final int dim1 = v1.getDimensionality(), dim2 = v1.getDimensionality(), dimo = v1.getDimensionality();
    final int mindim = (dim1 <= dim2) ? dim1 : dim2;
    // Essentially, we want to compute this:
    // v1' = v1 - o, v2' = v2 - o
    // v1'.transposeTimes(v2') / (v1'.euclideanLength()*v2'.euclideanLength());
    // We can just compute all three in parallel.
    double[] oe = o.getArrayRef();
    double s = 0, e1 = 0, e2 = 0;
    for(int k = 0; k < mindim; k++) {
      final double dk = k < dimo ? oe[k] : 0.;
      final double r1 = v1.doubleValue(k) - dk;
      final double r2 = v2.doubleValue(k) - dk;
      s += r1 * r2;
      e1 += r1 * r1;
      e2 += r2 * r2;
    }
    for(int k = mindim; k < dim1; k++) {
      final double dk = k < dimo ? oe[k] : 0.;
      final double r1 = v1.doubleValue(k) - dk;
      e1 += r1 * r1;
    }
    for(int k = mindim; k < dim2; k++) {
      final double dk = k < dimo ? oe[k] : 0.;
      final double r2 = v2.doubleValue(k) - dk;
      e2 += r2 * r2;
    }
    final double a = Math.sqrt((s / e1) * (s / e2));
    return (a > 1.) ? 1. : a;
  }

  /**
   * Compute the angle between two vectors.
   * 
   * @param v1 first vector
   * @param v2 second vector
   * @param o Origin
   * @return Angle
   */
  public static double angle(NumberVector<?> v1, NumberVector<?> v2, NumberVector<?> o) {
    final int dim1 = v1.getDimensionality(), dim2 = v1.getDimensionality(), dimo = o.getDimensionality();
    final int mindim = (dim1 <= dim2) ? dim1 : dim2;
    // Essentially, we want to compute this:
    // v1' = v1 - o, v2' = v2 - o
    // v1'.transposeTimes(v2') / (v1'.euclideanLength()*v2'.euclideanLength());
    // We can just compute all three in parallel.
    double s = 0, e1 = 0, e2 = 0;
    for(int k = 0; k < mindim; k++) {
      final double ok = k < dimo ? o.doubleValue(k) : 0.;
      final double r1 = v1.doubleValue(k) - ok;
      final double r2 = v2.doubleValue(k) - o.doubleValue(k);
      s += r1 * r2;
      e1 += r1 * r1;
      e2 += r2 * r2;
    }
    for(int k = mindim; k < dim1; k++) {
      final double ok = k < dimo ? o.doubleValue(k) : 0.;
      final double r1 = v1.doubleValue(k) - ok;
      e1 += r1 * r1;
    }
    for(int k = mindim; k < dim2; k++) {
      final double ok = k < dimo ? o.doubleValue(k) : 0.;
      final double r2 = v2.doubleValue(k) - ok;
      e2 += r2 * r2;
    }
    final double a = Math.sqrt((s / e1) * (s / e2));
    return (a > 1.) ? 1. : a;
  }

  /**
   * Compute the absolute cosine of the angle between two vectors.
   * 
   * To convert it to radians, use <code>Math.acos(angle)</code>!
   * 
   * @param v1 first vector
   * @param v2 second vector
   * @return Angle
   */
  public static double cosAngle(NumberVector<?> v1, NumberVector<?> v2) {
    if(v1 instanceof SparseNumberVector<?> && v2 instanceof SparseNumberVector<?>) {
      return angleSparse((SparseNumberVector<?>) v1, (SparseNumberVector<?>) v2);
    }
    final int dim1 = v1.getDimensionality(), dim2 = v1.getDimensionality();
    final int mindim = (dim1 <= dim2) ? dim1 : dim2;
    // Essentially, we want to compute this:
    // v1.transposeTimes(v2) / (v1.euclideanLength() * v2.euclideanLength());
    // We can just compute all three in parallel.
    double s = 0, e1 = 0, e2 = 0;
    for(int k = 0; k < mindim; k++) {
      final double r1 = v1.doubleValue(k);
      final double r2 = v2.doubleValue(k);
      s += r1 * r2;
      e1 += r1 * r1;
      e2 += r2 * r2;
    }
    for(int k = mindim; k < dim1; k++) {
      final double r1 = v1.doubleValue(k);
      e1 += r1 * r1;
    }
    for(int k = mindim; k < dim2; k++) {
      final double r2 = v2.doubleValue(k);
      e2 += r2 * r2;
    }
    final double a = Math.sqrt((s / e1) * (s / e2));
    return (a > 1.) ? 1. : a;
  }

  // TODO: add more precise but slower O(n^2) angle computation according to:
  // Computing the Angle between Vectors, P. Schatte
  // Journal of Computing, Volume 63, Number 1 (1999)

  /**
   * Compute the minimum angle between two rectangles.
   * 
   * @param v1 first rectangle
   * @param v2 second rectangle
   * @return Angle
   */
  public static double minCosAngle(SpatialComparable v1, SpatialComparable v2) {
    if(v1 instanceof NumberVector<?> && v2 instanceof NumberVector<?>) {
      return cosAngle((NumberVector<?>) v1, (NumberVector<?>) v2);
    }
    final int dim1 = v1.getDimensionality(), dim2 = v1.getDimensionality();
    final int mindim = (dim1 <= dim2) ? dim1 : dim2;
    // Essentially, we want to compute this:
    // absmax(v1.transposeTimes(v2))/(min(v1.euclideanLength())*min(v2.euclideanLength()));
    // We can just compute all three in parallel.
    double s1 = 0, s2 = 0, e1 = 0, e2 = 0;
    for(int k = 0; k < mindim; k++) {
      final double min1 = v1.getMin(k), max1 = v1.getMax(k);
      final double min2 = v2.getMin(k), max2 = v2.getMax(k);
      final double p1 = min1 * min2, p2 = min1 * max2;
      final double p3 = max1 * min2, p4 = max1 * max2;
      s1 += Math.max(Math.max(p1, p2), Math.max(p3, p4));
      s2 += Math.min(Math.min(p1, p2), Math.min(p3, p4));
      if(max1 < 0) {
        e1 += max1 * max1;
      }
      else if(min1 > 0) {
        e1 += min1 * min1;
      } // else: 0
      if(max2 < 0) {
        e2 += max2 * max2;
      }
      else if(min2 > 0) {
        e2 += min2 * min2;
      } // else: 0
    }
    for(int k = mindim; k < dim1; k++) {
      final double min1 = v1.getMin(k), max1 = v1.getMax(k);
      if(max1 < 0.) {
        e1 += max1 * max1;
      }
      else if(min1 > 0.) {
        e1 += min1 * min1;
      } // else: 0
    }
    for(int k = mindim; k < dim2; k++) {
      final double min2 = v2.getMin(k), max2 = v2.getMax(k);
      if(max2 < 0.) {
        e2 += max2 * max2;
      }
      else if(min2 > 0.) {
        e2 += min2 * min2;
      } // else: 0
    }
    final double s = Math.max(s1, Math.abs(s2));
    final double a = Math.sqrt((s / e1) * (s / e2));
    return (a > 1.) ? 1. : a;
  }

  /**
   * Provides the scalar product (inner product) of this and the given
   * DoubleVector.
   * 
   * @param d1 the first vector to compute the scalar product for
   * @param d2 the second vector to compute the scalar product for
   * @return the scalar product (inner product) of this and the given
   *         DoubleVector
   */
  public static double scalarProduct(NumberVector<?> d1, NumberVector<?> d2) {
    final int dim = d1.getDimensionality();
    double result = 0.;
    for(int i = 0; i < dim; i++) {
      result += d1.doubleValue(i) * d2.doubleValue(i);
    }
    return result;
  }

  /**
   * Compute medoid for a given subset.
   * 
   * @param relation Relation to process
   * @param sample Sample set
   * @return Medoid vector
   */
  public static Vector computeMedoid(Relation<? extends NumberVector<?>> relation, DBIDs sample) {
    final int dim = RelationUtil.dimensionality(relation);
    ArrayModifiableDBIDs mids = DBIDUtil.newArray(sample);
    SortDBIDsBySingleDimension s = new SortDBIDsBySingleDimension(relation);
    Vector medoid = new Vector(dim);
    for(int d = 0; d < dim; d++) {
      s.setDimension(d);
      medoid.set(d, relation.get(QuickSelect.median(mids, s)).doubleValue(d));
    }
    return medoid;
  }

  /**
   * This is an ugly hack, but we don't want to have the {@link Matrix} class
   * depend on {@link NumberVector}. Maybe a future version will no longer need
   * this.
   * 
   * @param mat Matrix
   * @param v Vector
   * @return {@code mat * v}, as double array.
   */
  public static double[] fastTimes(Matrix mat, NumberVector<?> v) {
    final double[][] elements = mat.getArrayRef();
    final int cdim = mat.getColumnDimensionality();
    final double[] X = new double[elements.length];
    // multiply it with each row from A
    for(int i = 0; i < elements.length; i++) {
      final double[] Arowi = elements[i];
      double s = 0;
      for(int k = 0; k < cdim; k++) {
        s += Arowi[k] * v.doubleValue(k);
      }
      X[i] = s;
    }
    return X;
  }

  /**
   * Compare number vectors by a single dimension.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class SortDBIDsBySingleDimension implements Comparator<DBIDRef> {
    /**
     * Dimension to sort with.
     */
    private int d;

    /**
     * The relation to sort.
     */
    private Relation<? extends NumberVector<?>> data;

    /**
     * Constructor.
     * 
     * @param data Vector data source
     * @param dim Dimension to sort by
     */
    public SortDBIDsBySingleDimension(Relation<? extends NumberVector<?>> data, int dim) {
      super();
      this.data = data;
      this.d = dim;
    };

    /**
     * Constructor.
     * 
     * @param data Vector data source
     */
    public SortDBIDsBySingleDimension(Relation<? extends NumberVector<?>> data) {
      super();
      this.data = data;
    };

    /**
     * Get the dimension to sort by.
     * 
     * @return Dimension to sort with
     */
    public int getDimension() {
      return this.d;
    }

    /**
     * Set the dimension to sort by.
     * 
     * @param d2 Dimension to sort with
     */
    public void setDimension(int d2) {
      this.d = d2;
    }

    @Override
    public int compare(DBIDRef id1, DBIDRef id2) {
      return Double.compare(data.get(id1).doubleValue(d), data.get(id2).doubleValue(d));
    }
  }

  /**
   * Compare number vectors by a single dimension.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class SortVectorsBySingleDimension implements Comparator<NumberVector<?>> {
    /**
     * Dimension to sort with.
     */
    private int d;

    /**
     * Constructor.
     * 
     * @param dim Dimension to sort by.
     */
    public SortVectorsBySingleDimension(int dim) {
      super();
      this.d = dim;
    };

    /**
     * Constructor.
     */
    public SortVectorsBySingleDimension() {
      super();
    };

    /**
     * Get the dimension to sort by.
     * 
     * @return Dimension to sort with
     */
    public int getDimension() {
      return this.d;
    }

    /**
     * Set the dimension to sort by.
     * 
     * @param d2 Dimension to sort with
     */
    public void setDimension(int d2) {
      this.d = d2;
    }

    @Override
    public int compare(NumberVector<?> o1, NumberVector<?> o2) {
      return Double.compare(o1.doubleValue(d), o2.doubleValue(d));
    }
  }

  /**
   * Provides a new NumberVector as a projection on the specified attributes.
   * 
   * @param v a NumberVector to project
   * @param selectedAttributes the attributes selected for projection
   * @param factory Vector factory
   * @param <V> Vector type
   * @return a new NumberVector as a projection on the specified attributes
   */
  public static <V extends NumberVector<?>> V project(V v, BitSet selectedAttributes, NumberVector.Factory<V, ?> factory) {
    if(factory instanceof SparseNumberVector.Factory) {
      final SparseNumberVector.Factory<?, ?> sfactory = (SparseNumberVector.Factory<?, ?>) factory;
      TIntDoubleHashMap values = new TIntDoubleHashMap(selectedAttributes.cardinality(), 1);
      for(int d = selectedAttributes.nextSetBit(0); d >= 0; d = selectedAttributes.nextSetBit(d + 1)) {
        if(v.doubleValue(d) != 0.0) {
          values.put(d, v.doubleValue(d));
        }
      }
      // We can't avoid this cast, because Java doesn't know that V is a
      // SparseNumberVector:
      @SuppressWarnings("unchecked")
      V projectedVector = (V) sfactory.newNumberVector(values, selectedAttributes.cardinality());
      return projectedVector;
    }
    else {
      double[] newAttributes = new double[selectedAttributes.cardinality()];
      int i = 0;
      for(int d = selectedAttributes.nextSetBit(0); d >= 0; d = selectedAttributes.nextSetBit(d + 1)) {
        newAttributes[i] = v.doubleValue(d);
        i++;
      }
      return factory.newNumberVector(newAttributes);
    }
  }
}
