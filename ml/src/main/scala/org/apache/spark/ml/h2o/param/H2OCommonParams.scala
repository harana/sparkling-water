/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.spark.ml.h2o.param

import org.apache.spark.internal.Logging
import org.apache.spark.ml.param._

/**
  * This trait contains parameters that are shared across all algorithms.
  */
trait H2OCommonParams extends Params with Logging {

  protected final val featuresCols = new StringArrayParam(this, "featuresCols", "Name of feature columns")
  protected final val labelCol = new Param[String](this, "labelCol", "Label column name")
  protected final val foldCol = new NullableStringParam(this, "foldCol", "Fold column name")
  protected final val weightCol = new NullableStringParam(this, "weightCol", "Weight column name")
  protected final val splitRatio = new DoubleParam(this, "splitRatio",
    "Accepts values in range [0, 1.0] which determine how large part of dataset is used for training and for validation. " +
      "For example, 0.8 -> 80% training 20% validation.")

  protected final val seed = new LongParam(this, "seed", "Used to specify seed to reproduce the model run")
  protected final val nfolds = new IntParam(this, "nfolds", "Number of fold columns")

  protected final val allStringColumnsToCategorical = new BooleanParam(this,
    "allStringColumnsToCategorical",
    "Transform all strings columns to categorical")

  protected final val columnsToCategorical = new StringArrayParam(this,
    "columnsToCategorical",
    "List of columns to convert to categorical before modelling")

  protected final val convertUnknownCategoricalLevelsToNa = new BooleanParam(this,
    "convertUnknownCategoricalLevelsToNa",
    "If set to 'true', the model converts unknown categorical levels to NA during making predictions.")

  protected final val convertInvalidNumbersToNa = new BooleanParam(this,
    "convertInvalidNumbersToNa",
    "If set to 'true', the model converts invalid numbers to NA during making predictions.")


  //
  // Default values
  //
  setDefault(
    featuresCols -> Array.empty[String],
    labelCol -> "label",
    foldCol -> null,
    weightCol -> null,
    splitRatio -> 1.0, // Use whole frame as training frame
    seed -> -1,
    nfolds -> 0,
    allStringColumnsToCategorical -> true,
    columnsToCategorical -> Array.empty[String],
    convertUnknownCategoricalLevelsToNa -> false,
    convertInvalidNumbersToNa -> false
  )

  //
  // Getters
  //
  def getFeaturesCols(): Array[String] = {
    val excludedCols = getExcludedCols()
    $(featuresCols).filter(c => excludedCols.forall(e => c.compareToIgnoreCase(e) != 0))
  }

  def getLabelCol(): String = $(labelCol)

  def getFoldCol(): String = $(foldCol)

  def getWeightCol(): String = $(weightCol)

  def getSplitRatio(): Double = $(splitRatio)

  def getSeed(): Long = $(seed)

  def getNfolds(): Int = $(nfolds)

  def getAllStringColumnsToCategorical(): Boolean = $(allStringColumnsToCategorical)

  def getColumnsToCategorical(): Array[String] = $(columnsToCategorical)

  def getConvertUnknownCategoricalLevelsToNa(): Boolean = $(convertUnknownCategoricalLevelsToNa)

  def getConvertInvalidNumbersToNa(): Boolean = $(convertInvalidNumbersToNa)

  //
  // Setters
  //
  def setFeaturesCol(first: String): this.type = setFeaturesCols(first)

  def setFeaturesCols(first: String, others: String*): this.type = set(featuresCols, Array(first) ++ others)

  def setFeaturesCols(columnNames: Array[String]): this.type = {
    require(columnNames.length > 0, "Array with feature columns must contain at least one column.")
    set(featuresCols, columnNames)
  }

  def setLabelCol(columnName: String): this.type = set(labelCol, columnName)

  def setFoldCol(columnName: String): this.type = set(foldCol, columnName)

  def setWeightCol(columnName: String): this.type = set(weightCol, columnName)

  def setSplitRatio(ratio: Double): this.type = set(splitRatio, ratio)

  def setSeed(value: Long): this.type = set(seed, value)

  def setNfolds(value: Int): this.type = set(nfolds, value)

  def setAllStringColumnsToCategorical(value: Boolean): this.type = set(allStringColumnsToCategorical, value)

  def setColumnsToCategorical(first: String, others: String*): this.type = set(columnsToCategorical, Array(first) ++ others)

  def setColumnsToCategorical(columns: Array[String]): this.type = set(columnsToCategorical, columns)

  def setConvertUnknownCategoricalLevelsToNa(value: Boolean): this.type = set(convertUnknownCategoricalLevelsToNa, value)

  def setConvertInvalidNumbersToNa(value: Boolean): this.type = set(convertInvalidNumbersToNa, value)

  //
  // Other methods
  //
  protected def getExcludedCols(): Seq[String] = {
    Seq(getLabelCol(), getFoldCol(), getWeightCol())
      .flatMap(Option(_)) // Remove nulls
  }
}
