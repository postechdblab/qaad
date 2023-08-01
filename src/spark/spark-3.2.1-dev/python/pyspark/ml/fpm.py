#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

from pyspark import keyword_only, since
#from pyspark.rdd import ignore_unicode_prefix
from pyspark.sql import DataFrame
from pyspark.ml.util import *
from pyspark.ml.wrapper import JavaEstimator, JavaModel, JavaParams
from pyspark.ml.param.shared import *

__all__ = ["FPGrowth", "MyFPGrowth", "FPGrowthModel", "MyFPGrowthModel", "PrefixSpan", "CSpade"]


class _FPGrowthParams(HasPredictionCol):
    """
    Params for :py:class:`FPGrowth` and :py:class:`FPGrowthModel`.

    .. versionadded:: 3.0.0
    """

    itemsCol = Param(Params._dummy(), "itemsCol",
                     "items column name", typeConverter=TypeConverters.toString)
    minSupport = Param(
        Params._dummy(),
        "minSupport",
        "Minimal support level of the frequent pattern. [0.0, 1.0]. " +
        "Any pattern that appears more than (minSupport * size-of-the-dataset) " +
        "times will be output in the frequent itemsets.",
        typeConverter=TypeConverters.toFloat)
    numPartitions = Param(
        Params._dummy(),
        "numPartitions",
        "Number of partitions (at least 1) used by parallel FP-growth. " +
        "By default the param is not set, " +
        "and partition number of the input dataset is used.",
        typeConverter=TypeConverters.toInt)
    minConfidence = Param(
        Params._dummy(),
        "minConfidence",
        "Minimal confidence for generating Association Rule. [0.0, 1.0]. " +
        "minConfidence will not affect the mining for frequent itemsets, " +
        "but will affect the association rules generation.",
        typeConverter=TypeConverters.toFloat)

    def getItemsCol(self):
        """
        Gets the value of itemsCol or its default value.
        """
        return self.getOrDefault(self.itemsCol)

    def getMinSupport(self):
        """
        Gets the value of minSupport or its default value.
        """
        return self.getOrDefault(self.minSupport)

    def getNumPartitions(self):
        """
        Gets the value of :py:attr:`numPartitions` or its default value.
        """
        return self.getOrDefault(self.numPartitions)

    def getMinConfidence(self):
        """
        Gets the value of minConfidence or its default value.
        """
        return self.getOrDefault(self.minConfidence)

class _MyFPGrowthParams(HasPredictionCol):
    """
    Params for :py:class:`FPGrowth` and :py:class:`FPGrowthModel`.

    .. versionadded:: 3.0.0
    """

    itemsCol = Param(Params._dummy(), "itemsCol",
                     "items column name", typeConverter=TypeConverters.toString)
    minSupport = Param(
        Params._dummy(),
        "minSupport",
        "Minimal support level of the frequent pattern. [0.0, 1.0]. " +
        "Any pattern that appears more than (minSupport * size-of-the-dataset) " +
        "times will be output in the frequent itemsets.",
        typeConverter=TypeConverters.toFloat)
    numPartitions = Param(
        Params._dummy(),
        "numPartitions",
        "Number of partitions (at least 1) used by parallel FP-growth. " +
        "By default the param is not set, " +
        "and partition number of the input dataset is used.",
        typeConverter=TypeConverters.toInt)
    minConfidence = Param(
        Params._dummy(),
        "minConfidence",
        "Minimal confidence for generating Association Rule. [0.0, 1.0]. " +
        "minConfidence will not affect the mining for frequent itemsets, " +
        "but will affect the association rules generation.",
        typeConverter=TypeConverters.toFloat)
    maxLength = Param( # maxLength XXX
        Params._dummy(),
        "maxLength",
        "Maximal length for generating Association Rule.",
        typeConverter=TypeConverters.toInt)
    minLift = Param(
        Params._dummy(),
        "minLift",
        "Minimal lift for generating Association Rule.",
        typeConverter=TypeConverters.toFloat)
    minChiSquare = Param(
        Params._dummy(),
        "minChiSquare",
        "Minimal chi-square for generating Association Rule.",
        typeConverter=TypeConverters.toFloat)
    minKulc = Param(
        Params._dummy(),
        "minKulc",
        "Minimal kulc for generating Association Rule.",
        typeConverter=TypeConverters.toFloat)
    minImbalanceRatio = Param(
        Params._dummy(),
        "minImbalanceRatio",
        "Minimal imbalance ratio for generating Association Rule.",
        typeConverter=TypeConverters.toFloat)

    def getItemsCol(self):
        """
        Gets the value of itemsCol or its default value.
        """
        return self.getOrDefault(self.itemsCol)

    def getMinSupport(self):
        """
        Gets the value of minSupport or its default value.
        """
        return self.getOrDefault(self.minSupport)

    def getNumPartitions(self):
        """
        Gets the value of :py:attr:`numPartitions` or its default value.
        """
        return self.getOrDefault(self.numPartitions)

    def getMinConfidence(self):
        """
        Gets the value of minConfidence or its default value.
        """
        return self.getOrDefault(self.minConfidence)

    def getMaxLength(self):
        return self.getOrDefault(self.maxLength)

    def getMinLift(self):
        return self.getOrDefault(self.minLift)

    def getMinChiSquare(self):
        return self.getOrDefault(self.minChiSquare)

    def getMinKulc(self):
        return self.getOrDefault(self.minKulc)

    def getMinImbalanceRatio(self):
        return self.getOrDefault(self.minImbalanceRatio)

class FPGrowthModel(JavaModel, _FPGrowthParams, JavaMLWritable, JavaMLReadable):
    """
    Model fitted by FPGrowth.

    .. versionadded:: 2.2.0
    """

    @since("3.0.0")
    def setItemsCol(self, value):
        """
        Sets the value of :py:attr:`itemsCol`.
        """
        return self._set(itemsCol=value)

    @since("3.0.0")
    def setMinConfidence(self, value):
        """
        Sets the value of :py:attr:`minConfidence`.
        """
        return self._set(minConfidence=value)

    @since("3.0.0")
    def setPredictionCol(self, value):
        """
        Sets the value of :py:attr:`predictionCol`.
        """
        return self._set(predictionCol=value)

    @property
    @since("2.2.0")
    def freqItemsets(self):
        """
        DataFrame with two columns:
        * `items` - Itemset of the same type as the input column.
        * `freq`  - Frequency of the itemset (`LongType`).
        """
        return self._call_java("freqItemsets")

    @property
    @since("2.2.0")
    def associationRules(self):
        """
        DataFrame with four columns:
        * `antecedent`  - Array of the same type as the input column.
        * `consequent`  - Array of the same type as the input column.
        * `confidence`  - Confidence for the rule (`DoubleType`).
        * `lift`        - Lift for the rule (`DoubleType`).
        """
        return self._call_java("associationRules")

class MyFPGrowthModel(JavaModel, _MyFPGrowthParams, JavaMLWritable, JavaMLReadable):
    """
    Model fitted by FPGrowth.

    .. versionadded:: 2.2.0
    """

    @since("3.0.0")
    def setItemsCol(self, value):
        """
        Sets the value of :py:attr:`itemsCol`.
        """
        return self._set(itemsCol=value)

    @since("3.0.0")
    def setMinConfidence(self, value):
        """
        Sets the value of :py:attr:`minConfidence`.
        """
        return self._set(minConfidence=value)

    def setMaxLength(self, value): # maxLength XXX
        return self._set(maxLength=value)

    def setMinLift(self, value):
        return self._set(minLift=value)

    def setMinChiSquare(self, value):
        return self._set(minChiSquare=value)

    def setMinKulc(self, value):
        return self._set(minKulc=value)

    def setMinImbalanceRatio(self, value):
        return self._set(minImbalanceRatio=value)

    @since("3.0.0")
    def setPredictionCol(self, value):
        """
        Sets the value of :py:attr:`predictionCol`.
        """
        return self._set(predictionCol=value)

    @property
    @since("2.2.0")
    def freqItemsets(self):
        """
        DataFrame with two columns:
        * `items` - Itemset of the same type as the input column.
        * `freq`  - Frequency of the itemset (`LongType`).
        """
        return self._call_java("freqItemsets")

    @property
    @since("2.2.0")
    def associationRules(self):
        """
        DataFrame with four columns:
        * `antecedent`  - Array of the same type as the input column.
        * `consequent`  - Array of the same type as the input column.
        * `confidence`  - Confidence for the rule (`DoubleType`).
        * `lift`        - Lift for the rule (`DoubleType`).
        """
        return self._call_java("associationRules")

#@ignore_unicode_prefix
class FPGrowth(JavaEstimator, _FPGrowthParams, JavaMLWritable, JavaMLReadable):
    r"""
    A parallel FP-growth algorithm to mine frequent itemsets. The algorithm is described in
    Li et al., PFP: Parallel FP-Growth for Query Recommendation [LI2008]_.
    PFP distributes computation in such a way that each worker executes an
    independent group of mining tasks. The FP-Growth algorithm is described in
    Han et al., Mining frequent patterns without candidate generation [HAN2000]_

    .. [LI2008] https://doi.org/10.1145/1454008.1454027
    .. [HAN2000] https://doi.org/10.1145/335191.335372

    .. note:: null values in the feature column are ignored during fit().
    .. note:: Internally `transform` `collects` and `broadcasts` association rules.

    >>> from pyspark.sql.functions import split
    >>> data = (spark.read
    ...     .text("data/mllib/sample_fpgrowth.txt")
    ...     .select(split("value", "\s+").alias("items")))
    >>> data.show(truncate=False)
    +------------------------+
    |items                   |
    +------------------------+
    |[r, z, h, k, p]         |
    |[z, y, x, w, v, u, t, s]|
    |[s, x, o, n, r]         |
    |[x, z, y, m, t, s, q, e]|
    |[z]                     |
    |[x, z, y, r, q, t, p]   |
    +------------------------+
    ...
    >>> fp = FPGrowth(minSupport=0.2, minConfidence=0.7)
    >>> fpm = fp.fit(data)
    >>> fpm.setPredictionCol("newPrediction")
    FPGrowthModel...
    >>> fpm.freqItemsets.show(5)
    +---------+----+
    |    items|freq|
    +---------+----+
    |      [s]|   3|
    |   [s, x]|   3|
    |[s, x, z]|   2|
    |   [s, z]|   2|
    |      [r]|   3|
    +---------+----+
    only showing top 5 rows
    ...
    >>> fpm.associationRules.show(5)
    +----------+----------+----------+----+
    |antecedent|consequent|confidence|lift|
    +----------+----------+----------+----+
    |    [t, s]|       [y]|       1.0| 2.0|
    |    [t, s]|       [x]|       1.0| 1.5|
    |    [t, s]|       [z]|       1.0| 1.2|
    |       [p]|       [r]|       1.0| 2.0|
    |       [p]|       [z]|       1.0| 1.2|
    +----------+----------+----------+----+
    only showing top 5 rows
    ...
    >>> new_data = spark.createDataFrame([(["t", "s"], )], ["items"])
    >>> sorted(fpm.transform(new_data).first().newPrediction)
    [u'x', u'y', u'z']

    .. versionadded:: 2.2.0
    """
    @keyword_only
    def __init__(self, minSupport=0.3, minConfidence=0.8, itemsCol="items",
                 predictionCol="prediction", numPartitions=None):
        """
        __init__(self, minSupport=0.3, minConfidence=0.8, itemsCol="items", \
                 predictionCol="prediction", numPartitions=None)
        """
        super(FPGrowth, self).__init__()
        self._java_obj = self._new_java_obj("org.apache.spark.ml.fpm.FPGrowth", self.uid)
        self._setDefault(minSupport=0.3, minConfidence=0.8,
                         itemsCol="items", predictionCol="prediction")
        kwargs = self._input_kwargs
        self.setParams(**kwargs)

    @keyword_only
    @since("2.2.0")
    def setParams(self, minSupport=0.3, minConfidence=0.8, itemsCol="items",
                  predictionCol="prediction", numPartitions=None):
        """
        setParams(self, minSupport=0.3, minConfidence=0.8, itemsCol="items", \
                  predictionCol="prediction", numPartitions=None)
        """
        kwargs = self._input_kwargs
        return self._set(**kwargs)

    def setItemsCol(self, value):
        """
        Sets the value of :py:attr:`itemsCol`.
        """
        return self._set(itemsCol=value)

    def setMinSupport(self, value):
        """
        Sets the value of :py:attr:`minSupport`.
        """
        return self._set(minSupport=value)

    def setNumPartitions(self, value):
        """
        Sets the value of :py:attr:`numPartitions`.
        """
        return self._set(numPartitions=value)

    def setMinConfidence(self, value):
        """
        Sets the value of :py:attr:`minConfidence`.
        """
        return self._set(minConfidence=value)

    def setPredictionCol(self, value):
        """
        Sets the value of :py:attr:`predictionCol`.
        """
        return self._set(predictionCol=value)

    def _create_model(self, java_model):
        return FPGrowthModel(java_model)


#@ignore_unicode_prefix
class MyFPGrowth(JavaEstimator, _MyFPGrowthParams, JavaMLWritable, JavaMLReadable):
    r"""
    A parallel FP-growth algorithm to mine frequent itemsets. The algorithm is described in
    Li et al., PFP: Parallel FP-Growth for Query Recommendation [LI2008]_.
    PFP distributes computation in such a way that each worker executes an
    independent group of mining tasks. The FP-Growth algorithm is described in
    Han et al., Mining frequent patterns without candidate generation [HAN2000]_

    .. [LI2008] https://doi.org/10.1145/1454008.1454027
    .. [HAN2000] https://doi.org/10.1145/335191.335372

    .. note:: null values in the feature column are ignored during fit().
    .. note:: Internally `transform` `collects` and `broadcasts` association rules.

    >>> from pyspark.sql.functions import split
    >>> data = (spark.read
    ...     .text("data/mllib/sample_fpgrowth.txt")
    ...     .select(split("value", "\s+").alias("items")))
    >>> data.show(truncate=False)
    +------------------------+
    |items                   |
    +------------------------+
    |[r, z, h, k, p]         |
    |[z, y, x, w, v, u, t, s]|
    |[s, x, o, n, r]         |
    |[x, z, y, m, t, s, q, e]|
    |[z]                     |
    |[x, z, y, r, q, t, p]   |
    +------------------------+
    ...
    >>> fp = FPGrowth(minSupport=0.2, minConfidence=0.7)
    >>> fpm = fp.fit(data)
    >>> fpm.setPredictionCol("newPrediction")
    FPGrowthModel...
    >>> fpm.freqItemsets.show(5)
    +---------+----+
    |    items|freq|
    +---------+----+
    |      [s]|   3|
    |   [s, x]|   3|
    |[s, x, z]|   2|
    |   [s, z]|   2|
    |      [r]|   3|
    +---------+----+
    only showing top 5 rows
    ...
    >>> fpm.associationRules.show(5)
    +----------+----------+----------+----+
    |antecedent|consequent|confidence|lift|
    +----------+----------+----------+----+
    |    [t, s]|       [y]|       1.0| 2.0|
    |    [t, s]|       [x]|       1.0| 1.5|
    |    [t, s]|       [z]|       1.0| 1.2|
    |       [p]|       [r]|       1.0| 2.0|
    |       [p]|       [z]|       1.0| 1.2|
    +----------+----------+----------+----+
    only showing top 5 rows
    ...
    >>> new_data = spark.createDataFrame([(["t", "s"], )], ["items"])
    >>> sorted(fpm.transform(new_data).first().newPrediction)
    [u'x', u'y', u'z']

    .. versionadded:: 2.2.0
    """
    @keyword_only
    def __init__(self, minSupport=0.3, minConfidence=0.8, maxLength=-1, minLift=-1.0, minChiSquare=-1.0, minKulc=-1.0, minImbalanceRatio=-1.0, itemsCol="items",
                 predictionCol="prediction", numPartitions=None):
        """
        __init__(self, minSupport=0.3, minConfidence=0.8, maxLength=-1, itemsCol="items", \
                 predictionCol="prediction", numPartitions=None)
        """
        super(MyFPGrowth, self).__init__()
        self._java_obj = self._new_java_obj("org.apache.spark.ml.fpm.MyFPGrowth", self.uid)
        self._setDefault(minSupport=0.3, minConfidence=0.8, maxLength=-1, minLift=-1.0, minChiSquare=-1.0, minKulc=-1.0, minImbalanceRatio=-1.0,
                         itemsCol="items", predictionCol="prediction")
        kwargs = self._input_kwargs
        self.setParams(**kwargs)

    @keyword_only
    @since("2.2.0")
    def setParams(self, minSupport=0.3, minConfidence=0.8, maxLength=-1, minLift=-1.0, minChiSquare=-1.0, minKulc=-1.0, minImbalanceRatio=-1.0, itemsCol="items",
                  predictionCol="prediction", numPartitions=None):
        """
        setParams(self, minSupport=0.3, minConfidence=0.8, maxLength=-1, itemsCol="items", \
                  predictionCol="prediction", numPartitions=None)
        """
        kwargs = self._input_kwargs
        return self._set(**kwargs)

    def setItemsCol(self, value):
        """
        Sets the value of :py:attr:`itemsCol`.
        """
        return self._set(itemsCol=value)

    def setMinSupport(self, value):
        """
        Sets the value of :py:attr:`minSupport`.
        """
        return self._set(minSupport=value)

    def setNumPartitions(self, value):
        """
        Sets the value of :py:attr:`numPartitions`.
        """
        return self._set(numPartitions=value)

    def setMinConfidence(self, value):
        """
        Sets the value of :py:attr:`minConfidence`.
        """
        return self._set(minConfidence=value)

    def setMaxLength(self, value): # maxLength XXX
        return self._set(maxLength=value)

    def setMinLift(self, value):
        return self._set(minLift=value)

    def setMinChiSquare(self, value):
        return self._set(minChiSquare=value)

    def setMinKulc(self, value):
        return self._set(minKulc=value)

    def setMinImbalanceRatio(self, value):
        return self._set(minImbalanceRatio=value)

    def setPredictionCol(self, value):
        """
        Sets the value of :py:attr:`predictionCol`.
        """
        return self._set(predictionCol=value)

    def _create_model(self, java_model):
        return MyFPGrowthModel(java_model)

class CSpade(JavaParams):
    """
    A parallel CSPADE algorithm to mine frequent sequential patterns.
    This class is not yet an Estimator/Transformer, use :py:func:`findFrequentSequentialPatterns`
    method to run the CSPADE algorithm.

    @see <a href="https://en.wikipedia.org/wiki/Sequential_Pattern_Mining">Sequential Pattern Mining
    (Wikipedia)</a>

    >>> from pyspark.ml.fpm import CSpade 
    >>> from pyspark.sql import Row
    >>> df = sc.parallelize([Row(sequence=[[1, 2], [3]]),
    ...                      Row(sequence=[[1], [3, 2], [1, 2]]),
    ...                      Row(sequence=[[1, 2], [5]]),
    ...                      Row(sequence=[[6]])]).toDF()
    >>> CSpade = CSpade()
    >>> CSpade.getSequenceCol()
    'sequence'
    >>> CSpade.setMinSupport(0.5)
    CSpade...
    >>> CSpade.setMaxPatternLength(5)
    CSpade...
    >>> CSpade.findFrequentSequentialPatterns(df).sort("sequence").show(truncate=False)
    +----------+----+
    |sequence  |freq|
    +----------+----+
    |[[1]]     |3   |
    |[[1], [3]]|2   |
    |[[2]]     |3   |
    |[[2, 1]]  |3   |
    |[[3]]     |2   |
    +----------+----+
    ...

    .. versionadded:: 2.4.0
    """

    minSupport = Param(Params._dummy(), "minSupport", "The minimal support level of the " +
                       "sequential pattern. Sequential pattern that appears more than " +
                       "(minSupport * size-of-the-dataset) times will be output. Must be >= 0.",
                       typeConverter=TypeConverters.toFloat)

    maxPatternLength = Param(Params._dummy(), "maxPatternLength",
                             "The maximal length of the sequential pattern. Must be > 0.",
                             typeConverter=TypeConverters.toInt)
    
    maxWidth = Param(Params._dummy(), "maxWidth",
                             "The maximal width of the sequential pattern. Must be > 0.",
                             typeConverter=TypeConverters.toInt)

    maxGap = Param(Params._dummy(), "maxGap",
                             "The maximal gap of the sequential pattern. Must be > 0.",
                             typeConverter=TypeConverters.toInt)

    minGap = Param(Params._dummy(), "minGap",
                             "The minimal gap of the sequential pattern. Must be > 0.",
                             typeConverter=TypeConverters.toInt)
    
    window = Param(Params._dummy(), "window",
                             "The maximal window of the sequential pattern. Must be > 0.",
                             typeConverter=TypeConverters.toInt)

    minConfidence = Param(Params._dummy(), "minConfidence",
                             "The minimal confidence of the sequential pattern. Must be > 0.",
                             typeConverter=TypeConverters.toFloat)

    minLift = Param(Params._dummy(), "minLift",
                             "The minimal lift of the sequential pattern. Must be > 0.",
                             typeConverter=TypeConverters.toFloat)

    removeRepetitivePattern  = Param(Params._dummy(), "removeRepetitivePattern",
                             "",
                             typeConverter=TypeConverters.toBoolean)

    sequenceCol = Param(Params._dummy(), "sequenceCol", "The name of the sequence column in " +
                        "dataset, rows with nulls in this column are ignored.",
                        typeConverter=TypeConverters.toString)

    @keyword_only
    def __init__(self, minSupport=0.1, maxPatternLength=10, maxWidth=10, maxGap=-1, minGap=0, window=100, minConfidence=0.7, minLift=0.0, removeRepetitivePattern=False,
                 sequenceCol="sequence"):
        """
        __init__(self, minSupport=0.1, maxPatternLength=10, \
                 sequenceCol="sequence")
        """
        super(CSpade, self).__init__()
        self._java_obj = self._new_java_obj("org.apache.spark.ml.fpm.CSpade", self.uid)
        self._setDefault(minSupport=0.1, maxPatternLength=10,
                         sequenceCol="sequence")
        kwargs = self._input_kwargs
        self.setParams(**kwargs)

    @keyword_only
    @since("2.4.0")
    def setParams(self, minSupport=0.1, maxPatternLength=10, maxWidth=10, maxGap=-1, minGap=0, window=100, minConfidence=0.7, minLift=0.0, removeRepetitivePattern=False,
                  sequenceCol="sequence"):
        """
        setParams(self, minSupport=0.1, maxPatternLength=10, \
                  sequenceCol="sequence")
        """
        kwargs = self._input_kwargs
        return self._set(**kwargs)

    @since("3.0.0")
    def setMinSupport(self, value):
        """
        Sets the value of :py:attr:`minSupport`.
        """
        return self._set(minSupport=value)

    @since("3.0.0")
    def getMinSupport(self):
        """
        Gets the value of minSupport or its default value.
        """
        return self.getOrDefault(self.minSupport)

    @since("3.0.0")
    def setMaxPatternLength(self, value):
        """
        Sets the value of :py:attr:`maxPatternLength`.
        """
        return self._set(maxPatternLength=value)

    @since("3.0.0")
    def getMaxPatternLength(self):
        """
        Gets the value of maxPatternLength or its default value.
        """
        return self.getOrDefault(self.maxPatternLength)

    @since("3.0.0")
    def setMaxWidth(self, value):
        """
        Sets the value of :py:attr:`maxWidth`.
        """
        return self._set(maxWidth=value)

    @since("3.0.0")
    def getMaxWidth(self):
        """
        Gets the value of maxWidth or its default value.
        """
        return self.getOrDefault(self.maxWidth)

    @since("3.0.0")
    def setMinGap(self, value):
        """
        Sets the value of :py:attr:`minGap`.
        """
        return self._set(minGap=value)

    @since("3.0.0")
    def getMinGap(self):
        """
        Gets the value of minGap or its default value.
        """
        return self.getOrDefault(self.minGap)

    @since("3.0.0")
    def setMaxGap(self, value):
        """
        Sets the value of :py:attr:`maxGap`.
        """
        return self._set(maxGap=value)

    @since("3.0.0")
    def getMaxGap(self):
        """
        Gets the value of maxGap or its default value.
        """
        return self.getOrDefault(self.maxGap)

    @since("3.0.0")
    def setWindow(self, value):
        """
        Sets the value of :py:attr:`window`.
        """
        return self._set(window=value)

    @since("3.0.0")
    def getWindow(self):
        """
        Gets the value of window or its default value.
        """
        return self.getOrDefault(self.window)

    @since("3.0.0")
    def setMinConfidence(self, value):
        """
        Sets the value of :py:attr:`window`.
        """
        return self._set(minConfidence=value)

    @since("3.0.0")
    def getMinConfidence(self):
        """
        Gets the value of window or its default value.
        """
        return self.getOrDefault(self.minConfidence)

    @since("3.0.0")
    def setMinLift(self, value):
        """
        Sets the value of :py:attr:`window`.
        """
        return self._set(minLift=value)

    @since("3.0.0")
    def getMinLift(self):
        """
        Gets the value of window or its default value.
        """
        return self.getOrDefault(self.minLift)

    @since("3.0.0")
    def setRemoveRepetitivePattern(self, value):
        """
        Sets the value of :py:attr:`window`.
        """
        return self._set(removeRepetitivePattern=value)

    @since("3.0.0")
    def getRemoveRepetitivePattern(self):
        """
        Gets the value of window or its default value.
        """
        return self.getOrDefault(self.removeRepetitivePattern)

    @since("3.0.0")
    def setSequenceCol(self, value):
        """
        Sets the value of :py:attr:`sequenceCol`.
        """
        return self._set(sequenceCol=value)

    @since("3.0.0")
    def getSequenceCol(self):
        """
        Gets the value of sequenceCol or its default value.
        """
        return self.getOrDefault(self.sequenceCol)

    @since("2.4.0")
    def fit(self, dataset):
        """
        Finds the complete set of frequent sequential patterns in the input sequences of itemsets.

        :param dataset: A dataframe containing a sequence column which is
                        `ArrayType(ArrayType(T))` type, T is the item type for the input dataset.
        :return: A `DataFrame` that contains columns of sequence and corresponding frequency.
                 The schema of it will be:
                 - `sequence: ArrayType(ArrayType(T))` (T is the item type)
                 - `freq: Long`

        .. versionadded:: 2.4.0
        """

        self._transfer_params_to_java()
        jdf = self._java_obj.findFrequentSequentialPatterns(dataset._jdf)
        self.freqSequences = DataFrame(jdf, dataset.sql_ctx)
        return self

    def save(self, path):
        from pyspark.sql.types import StringType
        self.freqSequences.withColumn("sids", self.freqSequences["sids"].cast(StringType())).withColumn("sequence", self.freqSequences["sequence"].cast(StringType())).write.csv(path, header = 'true')
    def load(self, path):
        from pyspark.context import SparkContext
        from pyspark.sql.session import SparkSession
        sc = SparkContext.getOrCreate()
        spark = SparkSession(sc)
        self.freqSequences = spark.read.csv(path, header = 'true') 
        return self

class PrefixSpan(JavaParams):
    """
    A parallel PrefixSpan algorithm to mine frequent sequential patterns.
    The PrefixSpan algorithm is described in J. Pei, et al., PrefixSpan: Mining Sequential Patterns
    Efficiently by Prefix-Projected Pattern Growth
    (see <a href="https://doi.org/10.1109/ICDE.2001.914830">here</a>).
    This class is not yet an Estimator/Transformer, use :py:func:`findFrequentSequentialPatterns`
    method to run the PrefixSpan algorithm.

    @see <a href="https://en.wikipedia.org/wiki/Sequential_Pattern_Mining">Sequential Pattern Mining
    (Wikipedia)</a>

    >>> from pyspark.ml.fpm import PrefixSpan
    >>> from pyspark.sql import Row
    >>> df = sc.parallelize([Row(sequence=[[1, 2], [3]]),
    ...                      Row(sequence=[[1], [3, 2], [1, 2]]),
    ...                      Row(sequence=[[1, 2], [5]]),
    ...                      Row(sequence=[[6]])]).toDF()
    >>> prefixSpan = PrefixSpan()
    >>> prefixSpan.getMaxLocalProjDBSize()
    32000000
    >>> prefixSpan.getSequenceCol()
    'sequence'
    >>> prefixSpan.setMinSupport(0.5)
    PrefixSpan...
    >>> prefixSpan.setMaxPatternLength(5)
    PrefixSpan...
    >>> prefixSpan.findFrequentSequentialPatterns(df).sort("sequence").show(truncate=False)
    +----------+----+
    |sequence  |freq|
    +----------+----+
    |[[1]]     |3   |
    |[[1], [3]]|2   |
    |[[2]]     |3   |
    |[[2, 1]]  |3   |
    |[[3]]     |2   |
    +----------+----+
    ...

    .. versionadded:: 2.4.0
    """

    minSupport = Param(Params._dummy(), "minSupport", "The minimal support level of the " +
                       "sequential pattern. Sequential pattern that appears more than " +
                       "(minSupport * size-of-the-dataset) times will be output. Must be >= 0.",
                       typeConverter=TypeConverters.toFloat)

    maxPatternLength = Param(Params._dummy(), "maxPatternLength",
                             "The maximal length of the sequential pattern. Must be > 0.",
                             typeConverter=TypeConverters.toInt)

    maxLocalProjDBSize = Param(Params._dummy(), "maxLocalProjDBSize",
                               "The maximum number of items (including delimiters used in the " +
                               "internal storage format) allowed in a projected database before " +
                               "local processing. If a projected database exceeds this size, " +
                               "another iteration of distributed prefix growth is run. " +
                               "Must be > 0.",
                               typeConverter=TypeConverters.toInt)

    sequenceCol = Param(Params._dummy(), "sequenceCol", "The name of the sequence column in " +
                        "dataset, rows with nulls in this column are ignored.",
                        typeConverter=TypeConverters.toString)

    @keyword_only
    def __init__(self, minSupport=0.1, maxPatternLength=10, maxLocalProjDBSize=32000000,
                 sequenceCol="sequence"):
        """
        __init__(self, minSupport=0.1, maxPatternLength=10, maxLocalProjDBSize=32000000, \
                 sequenceCol="sequence")
        """
        super(PrefixSpan, self).__init__()
        self._java_obj = self._new_java_obj("org.apache.spark.ml.fpm.PrefixSpan", self.uid)
        self._setDefault(minSupport=0.1, maxPatternLength=10, maxLocalProjDBSize=32000000,
                         sequenceCol="sequence")
        kwargs = self._input_kwargs
        self.setParams(**kwargs)

    @keyword_only
    @since("2.4.0")
    def setParams(self, minSupport=0.1, maxPatternLength=10, maxLocalProjDBSize=32000000,
                  sequenceCol="sequence"):
        """
        setParams(self, minSupport=0.1, maxPatternLength=10, maxLocalProjDBSize=32000000, \
                  sequenceCol="sequence")
        """
        kwargs = self._input_kwargs
        return self._set(**kwargs)

    @since("3.0.0")
    def setMinSupport(self, value):
        """
        Sets the value of :py:attr:`minSupport`.
        """
        return self._set(minSupport=value)

    @since("3.0.0")
    def getMinSupport(self):
        """
        Gets the value of minSupport or its default value.
        """
        return self.getOrDefault(self.minSupport)

    @since("3.0.0")
    def setMaxPatternLength(self, value):
        """
        Sets the value of :py:attr:`maxPatternLength`.
        """
        return self._set(maxPatternLength=value)

    @since("3.0.0")
    def getMaxPatternLength(self):
        """
        Gets the value of maxPatternLength or its default value.
        """
        return self.getOrDefault(self.maxPatternLength)

    @since("3.0.0")
    def setMaxLocalProjDBSize(self, value):
        """
        Sets the value of :py:attr:`maxLocalProjDBSize`.
        """
        return self._set(maxLocalProjDBSize=value)

    @since("3.0.0")
    def getMaxLocalProjDBSize(self):
        """
        Gets the value of maxLocalProjDBSize or its default value.
        """
        return self.getOrDefault(self.maxLocalProjDBSize)

    @since("3.0.0")
    def setSequenceCol(self, value):
        """
        Sets the value of :py:attr:`sequenceCol`.
        """
        return self._set(sequenceCol=value)

    @since("3.0.0")
    def getSequenceCol(self):
        """
        Gets the value of sequenceCol or its default value.
        """
        return self.getOrDefault(self.sequenceCol)

    @since("2.4.0")
    def findFrequentSequentialPatterns(self, dataset):
        """
        Finds the complete set of frequent sequential patterns in the input sequences of itemsets.

        :param dataset: A dataframe containing a sequence column which is
                        `ArrayType(ArrayType(T))` type, T is the item type for the input dataset.
        :return: A `DataFrame` that contains columns of sequence and corresponding frequency.
                 The schema of it will be:
                 - `sequence: ArrayType(ArrayType(T))` (T is the item type)
                 - `freq: Long`

        .. versionadded:: 2.4.0
        """

        self._transfer_params_to_java()
        jdf = self._java_obj.findFrequentSequentialPatterns(dataset._jdf)
        return DataFrame(jdf, dataset.sql_ctx)


if __name__ == "__main__":
    import doctest
    import pyspark.ml.fpm
    from pyspark.sql import SparkSession
    globs = pyspark.ml.fpm.__dict__.copy()
    # The small batch size here ensures that we see multiple batches,
    # even in these small test examples:
    spark = SparkSession.builder\
        .master("local[2]")\
        .appName("ml.fpm tests")\
        .getOrCreate()
    sc = spark.sparkContext
    globs['sc'] = sc
    globs['spark'] = spark
    import tempfile
    temp_path = tempfile.mkdtemp()
    globs['temp_path'] = temp_path
    try:
        (failure_count, test_count) = doctest.testmod(globs=globs, optionflags=doctest.ELLIPSIS)
        spark.stop()
    finally:
        from shutil import rmtree
        try:
            rmtree(temp_path)
        except OSError:
            pass
    if failure_count:
        sys.exit(-1)
