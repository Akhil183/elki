package de.lmu.ifi.dbs.elki.index.preprocessed.localpca;

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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.distance.KNNList;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredRunner;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Provides the local neighborhood to be considered in the PCA as the k nearest
 * neighbors of an object.
 * 
 * @author Elke Achtert
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.uses KNNQuery
 * 
 * @param <NV> Vector type
 */
// TODO: loosen DoubleDistance restriction.
@Title("Knn Query Based Local PCA Preprocessor")
@Description("Materializes the local PCA and the locally weighted matrix of objects of a database. The PCA is based on k nearest neighbor queries.")
public class KNNQueryFilteredPCAIndex<NV extends NumberVector<?>> extends AbstractFilteredPCAIndex<NV> {
  /**
   * Logger.
   */
  private static final Logging LOG = Logging.getLogger(KNNQueryFilteredPCAIndex.class);

  /**
   * The kNN query instance we use.
   */
  private final KNNQuery<NV, DoubleDistance> knnQuery;

  /**
   * Query k.
   */
  private final int k;

  /**
   * Constructor.
   * 
   * @param relation Database to use
   * @param pca PCA Runner to use
   * @param knnQuery KNN Query to use
   * @param k k value
   */
  public KNNQueryFilteredPCAIndex(Relation<NV> relation, PCAFilteredRunner<NV> pca, KNNQuery<NV, DoubleDistance> knnQuery, int k) {
    super(relation, pca);
    this.knnQuery = knnQuery;
    this.k = k;
    // Sanity check:
    int dim = RelationUtil.dimensionality(relation);
    if(dim > 0 && k <= dim) {
      LOG.warning("PCA results with k < dim are meaningless. Choose k much larger than the dimensionality.");
    }
  }

  @Override
  protected KNNList<DoubleDistance> objectsForPCA(DBIDRef id) {
    return knnQuery.getKNNForDBID(id, k);
  }

  @Override
  public String getLongName() {
    return "kNN-based local filtered PCA";
  }

  @Override
  public String getShortName() {
    return "kNNFilteredPCA";
  }

  @Override
  public Logging getLogger() {
    return LOG;
  }

  @Override
  public void logStatistics() {
    // No statistics to log.
  }

  /**
   * Factory class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.landmark
   * @apiviz.uses KNNQueryFilteredPCAIndex oneway - - «create»
   */
  public static class Factory<V extends NumberVector<?>> extends AbstractFilteredPCAIndex.Factory<V, KNNQueryFilteredPCAIndex<V>> {
    /**
     * Optional parameter to specify the number of nearest neighbors considered
     * in the PCA, must be an integer greater than 0. If this parameter is not
     * set, k is set to three times of the dimensionality of the database
     * objects.
     * <p>
     * Key: {@code -localpca.k}
     * </p>
     * <p>
     * Default value: three times of the dimensionality of the database objects
     * </p>
     */
    public static final OptionID K_ID = new OptionID("localpca.k", "The number of nearest neighbors considered in the PCA. " + "If this parameter is not set, k ist set to three " + "times of the dimensionality of the database objects.");

    /**
     * Holds the value of {@link #K_ID}.
     */
    private Integer k = null;

    /**
     * Constructor.
     * 
     * @param pcaDistanceFunction distance
     * @param pca PCA class
     * @param k k
     */
    public Factory(DistanceFunction<V, DoubleDistance> pcaDistanceFunction, PCAFilteredRunner<V> pca, Integer k) {
      super(pcaDistanceFunction, pca);
      this.k = k;
    }

    @Override
    public KNNQueryFilteredPCAIndex<V> instantiate(Relation<V> relation) {
      // TODO: set bulk flag, once the parent class supports bulk.
      KNNQuery<V, DoubleDistance> knnquery = QueryUtil.getKNNQuery(relation, pcaDistanceFunction, k);
      return new KNNQueryFilteredPCAIndex<>(relation, pca, knnquery, k);
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static class Parameterizer<NV extends NumberVector<?>> extends AbstractFilteredPCAIndex.Factory.Parameterizer<NV, KNNQueryFilteredPCAIndex<NV>> {
      protected int k = 0;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        final IntParameter kP = new IntParameter(K_ID);
        kP.addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
        if(config.grab(kP)) {
          k = kP.getValue();
        }
      }

      @Override
      protected Factory<NV> makeInstance() {
        return new Factory<>(pcaDistanceFunction, pca, k);
      }
    }
  }
}
