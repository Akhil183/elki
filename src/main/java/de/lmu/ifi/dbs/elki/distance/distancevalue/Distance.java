package de.lmu.ifi.dbs.elki.distance.distancevalue;

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

import java.io.Externalizable;

/**
 * The interface Distance defines the requirements of any instance class.
 * 
 * See {@link de.lmu.ifi.dbs.elki.distance.DistanceUtil} for related utility
 * functions such as <code>min</code>, <code>max</code>.
 * 
 * @author Arthur Zimek
 * 
 * @see de.lmu.ifi.dbs.elki.distance.DistanceUtil
 * 
 * @apiviz.landmark
 * 
 * @param <D> the type of Distance used
 */
public interface Distance<D extends Distance<D>> extends Comparable<D>, Externalizable {
  /**
   * Any implementing class should implement a proper toString-method for
   * printing the result-values.
   * 
   * @return String a human-readable representation of the Distance
   */
  @Override
  String toString();

  /**
   * Provides a measurement suitable to this measurement function based on the
   * given pattern.
   * 
   * @param pattern a pattern defining a similarity suitable to this measurement
   *        function
   * @return a measurement suitable to this measurement function based on the
   *         given pattern
   * @throws IllegalArgumentException if the given pattern is not compatible
   *         with the requirements of this measurement function
   */
  D parseString(String pattern) throws IllegalArgumentException;

  /**
   * Returns a String as description of the required input format.
   * 
   * @return a String as description of the required input format
   */
  String requiredInputPattern();

  /**
   * Returns the number of Bytes this distance uses if it is written to an
   * external file.
   * 
   * @return the number of Bytes this distance uses if it is written to an
   *         external file
   */
  int externalizableSize();

  /**
   * Provides an infinite distance.
   * 
   * @return an infinite distance
   */
  D infiniteDistance();

  /**
   * Provides a null distance.
   * 
   * @return a null distance
   */
  D nullDistance();

  /**
   * Provides an undefined distance.
   * 
   * @return an undefined distance
   */
  D undefinedDistance();

  /**
   * Returns true, if the distance is an infinite distance, false otherwise.
   * 
   * @return true, if the distance is an infinite distance, false otherwise
   */
  boolean isInfiniteDistance();

  /**
   * Returns true, if the distance is a null distance, false otherwise.
   * 
   * @return true, if the distance is a null distance, false otherwise
   */
  boolean isNullDistance();

  /**
   * Returns true, if the distance is an undefined distance, false otherwise.
   * 
   * @return true, if the distance is an undefined distance, false otherwise
   */
  boolean isUndefinedDistance();
}