package org.apache.spark.mllib.fpm

import java.{lang => jl, util => ju}
//import java.util.concurrent.atomic.AtomicInteger

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

import org.json4s.DefaultFormats
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods.{compact, render}

import org.apache.spark.SparkContext
//import org.apache.spark.annotation.Since
import org.apache.spark.api.java.JavaRDD
import org.apache.spark.api.java.JavaSparkContext.fakeClassTag
import org.apache.spark.internal.Logging
import org.apache.spark.mllib.util.{Loader, Saveable}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.sql.catalyst.ScalaReflection
import org.apache.spark.sql.types._
import org.apache.spark.storage.StorageLevel

import org.apache.spark.Partitioner
//import org.apache.spark.broadcast.Broadcast

class CSpade private (
    private var minSupport: Double,
    private var maxPatternLength: Int,
    private var maxWidth: Int,
    private var minGap: Int,
    private var maxGap: Int,
    private var window: Int,
    private var minConfidence: Double,
    private var minLift: Double,
    private var removeRepetitivePattern: Boolean) extends Logging with Serializable {
  import CSpade._

  def this() = this(0.1, -1, -1, -1, -1, -1, 0.0, 0.0, false)

  def getMinSupport: Double = minSupport

  def setMinSupport(minSupport: Double): this.type = {
    this.minSupport = minSupport
    this
  }

  def getMaxPatternLength: Int = maxPatternLength

  def setMaxPatternLength(maxPatternLength: Int): this.type = {
    this.maxPatternLength = maxPatternLength
    this
  }

  def getMaxWidth: Int = maxWidth

  def setMaxWidth(maxWidth: Int): this.type = {
    this.maxWidth = maxWidth
    this
  }

  def getMinGap: Int = minGap

  def setMinGap(minGap: Int): this.type = {
    this.minGap = minGap
    this
  }

  def getMaxGap: Int = maxGap

  def setMaxGap(maxGap: Int): this.type = {
    this.maxGap = maxGap
    this
  }

  def getWindow: Int = window

  def setWindow(window: Int): this.type = {
    this.window = window
    this
  }

  def getMinConfidence: Double = minConfidence

  def setMinConfidence(minConfidence: Double): this.type = {
    this.minConfidence = minConfidence
    this
  }

  def getMinLift: Double = minLift

  def setMinLift(minLift: Double): this.type = {
    this.minLift = minLift
    this
  }

  def getRemoveRepetitivePattern: Boolean = removeRepetitivePattern

  def setRemoveRepetitivePattern(removeRepetitivePattern: Boolean): this.type = {
    this.removeRepetitivePattern = removeRepetitivePattern
    this
  }

  def fit[Item: ClassTag](dataset: DataFrame): CSpadeModel[Int] = {
    logInfo(s"mllib.fpm.CSpade.fit()")

    val totalCount = dataset.select("sid").distinct().count()
    val minCount = math.ceil(minSupport * totalCount).toLong
    logInfo(s"number of sequences: $totalCount")

    val freqItems = findFrequentItems(dataset, minCount)
    logInfo(s"number of frequent items: ${freqItems.length}")

    val itemToInt = freqItems.zipWithIndex.toMap
    val filteredDataset = filterDataset(dataset, itemToInt)
      .persist(StorageLevel.MEMORY_AND_DISK)

    val results: RDD[(Array[Array[Int]], Long, Array[Int])] = if (maxGap < 0) /*genFreqPatterns(dataset, filteredDataset, minCount, maxPatternLength, maxWidth, minGap, maxGap, window, itemToInt.size)*/ processWithoutMaxGapPrefixVer(dataset, filteredDataset, minCount, maxPatternLength, maxWidth, minGap, maxGap, window, minConfidence, minLift, itemToInt.size, freqItems, totalCount, removeRepetitivePattern)
else processMaxGapPrefixVer(dataset, filteredDataset, minCount, maxPatternLength, maxWidth, minGap, maxGap, window, minConfidence, minLift, itemToInt.size, freqItems, totalCount, removeRepetitivePattern)

    val freqSequences = results.map { case (sequence: Array[Array[Int]], count: Long, sids: Array[Int]) =>
      new FreqSequence[Int](sequence.map { itemset =>
        itemset.map(x => freqItems(x))      
      }, count, sids)
    }

    filteredDataset.unpersist()

    new CSpadeModel(freqSequences)
  }

  def fit[Item, Itemset <: jl.Iterable[Item], Sequence <: jl.Iterable[Itemset]](
      dataset: JavaRDD[(Int, Int, Array[Item])]): CSpadeModel[Int] = {
    implicit val tag = fakeClassTag[Item]
    fit(dataset.rdd.map(r => (r._1, r._2, r._3)))
  }
}

object CSpade extends Logging {
  type Sequence = List[Vector[Int]]
  type Table = Array[(Int, Array[(Int, Int)])]
  type Node = (Sequence, Table)

  val Sequence: Short = 0
  val Itemset: Short = 1
  type PairType = (Int, Short, Int, Short, Short)

  private[fpm] def findFrequentItems(
      dataset: DataFrame,
      minCount: Long): Array[Int] = {
    dataset.rdd.flatMap { r =>
      val uniqItems = mutable.Set.empty[(Int, Int)]
      r.getAs[Seq[Int]](2).toArray.foreach(item => uniqItems.add((item, r.getAs[Int](0))))
      uniqItems
    }.distinct()
    .map(x => (x._1, 1L))
    .reduceByKey(_ + _).filter { case (_, count) =>
      count >= minCount
    }
    .map(_._1)
    .collect()
  }

  private[fpm] def filterDataset(
      dataset: DataFrame,
      itemToInt: Map[Int, Int]): RDD[(Int, Int, Array[Int])] = {
    dataset.rdd.map { r =>
      val freqItems = mutable.ArrayBuilder.make[Int]
      r.getAs[Seq[Int]](2).toArray.foreach { item =>
        if (itemToInt.contains(item)) {
          freqItems += itemToInt(item)
        }
      }
      if (freqItems.result().nonEmpty) {
        (r(0).asInstanceOf[Int], r(1).asInstanceOf[Int], freqItems.result())
      } else {
        (r(0).asInstanceOf[Int], r(1).asInstanceOf[Int], Array.empty[Int])
      }
    }.filter(r => r._3.length > 0)
  }

  private[fpm] def processWithoutMaxGapPrefixVer(
    df: DataFrame,
    dataset: RDD[(Int, Int, Array[Int])],
    minCount: Long,
    maxPatternLength: Int,
    maxWidth: Int,
    minGap: Int,
    maxGap: Int,
    window: Int,
    minConfidence: Double,
    minLift: Double,
    numFreqItems: Int,
    freqItems: Array[Int],
    totalCount: Long,
    removeRepetitivePattern: Boolean): RDD[(Array[Array[Int]], Long, Array[Int])] = {
    //import org.apache.spark.util.SizeEstimator
    
    val t0 = System.currentTimeMillis
    /* 1-sequences */
    val partitioner = new CustomPartitioner(2 * numFreqItems)
    val oneSequences = (dataset.flatMap { transaction =>
      for (item <- transaction._3) yield (transaction._1, transaction._2, item)
    }.groupBy(_._3)
    .mapValues(x => x.groupBy(_._1).mapValues(y => y.map(z => (z._2, z._2)).toArray.sorted).toArray.sortBy(_._1))
    .map { case (item: Int, data: Table) =>
      val sequence = List[Vector[Int]](Vector[Int](item))
      (sequence, data)
    }).partitionBy(partitioner)
    .persist(StorageLevel.MEMORY_AND_DISK)

    def getResultRDD(rdd: RDD[(Sequence, Table)]): RDD[(Array[Array[Int]], Long, Array[Int])] = {
      rdd.map { case (sequence: Sequence, data: Table) =>
	(sequence.map(x => x.map(item => freqItems(item)).toArray).toArray, data.size.toLong, data.map(_._1))
      }
    }

    val oneSeqToFreq = oneSequences.map(x => (x._1.last.last, x._2.size.toLong)).collectAsMap
    var result = getResultRDD(oneSequences)
    var fileCount: Long = 0
    import df.sparkSession.implicits._
    result.toDF.withColumnRenamed("_1", "sequence").withColumnRenamed("_2", "freq").withColumnRenamed("_3", "sids").write.parquet("/user/root/assets/" + dataset.sparkContext.applicationId + "-" + fileCount)
    fileCount = fileCount + 1
    println("# of freqItems: " + result.count)

    def _isItemset(sequence: Sequence): Boolean = sequence.last.size != 1

    def _getJoinType(sequence1: Sequence, sequence2: Sequence): Short = {
      if (_isItemset(sequence1) && _isItemset(sequence2) && sequence1.last.last < sequence2.last.last) {
	0: Short
      } else if (_isItemset(sequence1) && !_isItemset(sequence2)) {
	1: Short
      } else if (!_isItemset(sequence1) && !_isItemset(sequence2) && sequence1.last.last != sequence2.last.last) {
	2: Short
      } else if (!_isItemset(sequence1) && !_isItemset(sequence2) && sequence1.last.last == sequence2.last.last) {
	3: Short
      } else {
	-1: Short
      }
    }

    def _join1(table1: Table, table2: Table): Table = {
      var acc = mutable.ArrayBuilder.make[(Int, Array[(Int, Int)])]
      var i: Int = 0
      var j: Int = 0
      val n = table1.length
      val m = table2.length
      while (i < n && j < m) {
	val sid1: Int = table1(i)._1
	val sid2: Int = table2(j)._1
	if (sid1 == sid2) {
	  var k: Int = 0
	  var l: Int = 0
	  val dataOfSid1: Array[(Int, Int)] = table1(i)._2
	  val dataOfSid2: Array[(Int, Int)] = table2(j)._2
	  var newData = mutable.ArrayBuilder.make[(Int, Int)]
	  val l1 = dataOfSid1.length
	  val l2 = dataOfSid2.length
	  while (k < l1 && l < l2) {
	    val eid1 = dataOfSid1(k)._1
	    val eid2 = dataOfSid2(l)._1
	    if (eid1 == eid2) {
	      val tmp = (eid1, math.max(dataOfSid1(k)._2, dataOfSid2(l)._2))
	      newData += tmp
	      k = k + 1
	      l = l + 1
	    } else if (eid1 < eid2) {
	      k = k + 1
	    } else {
	      l = l + 1
	    }
	  }
	  if (newData.result().size > 0) {
	    val tmp = (sid1, newData.result().toArray)
	    acc += tmp
	  }
	  i = i + 1
	  j = j + 1
	} else if (sid1 < sid2) {
	  i = i + 1
	} else {
	  j = j + 1
	}
      }
      acc.result()
    }

    def _join2(table1: Table, table2: Table, minGap: Int, maxGap: Int, window: Int): Table = {
      var acc = mutable.ArrayBuilder.make[(Int, Array[(Int, Int)])]
      var i: Int = 0
      var j: Int = 0
      val n: Int = table1.length
      val m: Int = table2.length
      val minGap2 = math.max(1, minGap)
      while (i < n && j < m) {
	val sid1: Int = table1(i)._1
	val sid2: Int = table2(j)._1
	if (sid1 == sid2) {
	  var k: Int = 0
	  var l: Int = 0
	  val dataOfSid1: Array[(Int, Int)] = table1(i)._2
	  val dataOfSid2: Array[(Int, Int)] = table2(j)._2
	  var newData = mutable.ArrayBuilder.make[(Int, Int)]
	  val l1 = dataOfSid1.length
	  val l2 = dataOfSid2.length
	  while (k < l1 && l < l2) {
	    val eid1 = dataOfSid1(k)._1
	    val eid2 = dataOfSid2(l)._1
	    if (eid1 < eid2 + minGap2) {
	      k = k + 1
	    } else {
	      if ((maxGap < 0 || (maxGap > 0 && eid1 - eid2 <= maxGap)) &&
		(window < 0 || (eid1 - dataOfSid2(l)._2 <= window))) {
		val tmp = (eid1, math.min(dataOfSid2(l)._2, dataOfSid1(k)._2))
		newData += tmp
                k = k + 1
	      } else {
                l = l + 1
              }
	    }
	  }
	  if (newData.result().size > 0) {
	    val tmp = (sid1, newData.result().toArray)
	    acc += tmp
	  }
	  i = i + 1
	  j = j + 1
	} else if (sid1 < sid2) {
	  i = i + 1
	} else {
	  j = j + 1
	}
      }
      acc.result()
    }

    def _getNextSequence1(sequence1: Sequence, sequence2: Sequence): Sequence = sequence1.dropRight(1) ++ List(sequence1.last ++ Vector(sequence2.last.last))

    def _getNextSequence2(sequence1: Sequence, sequence2: Sequence): Sequence = sequence1 ++ List(sequence2.last)

    def getNextNodes(node1: Node, node2: Node): Array[Node] = {
      val sequence1 = node1._1
      val table1 = node1._2
      val sequence2 = node2._1
      val table2 = node2._2
      val joinType = _getJoinType(sequence1, sequence2)
      if (joinType == 0) {
	if (sequence1.last.size < maxWidth || maxWidth < 0) {
	  Array((_getNextSequence1(sequence1, sequence2), _join1(table2, table1)))
	} else {
	  Array()
        }
      } else if (joinType == 1) {
        Array((_getNextSequence2(sequence1, sequence2), _join2(table2, table1, minGap, maxGap, window)))
      } else if (joinType == 2) {
	if (sequence1.last.last < sequence2.last.last) {
	  if (sequence1.last.size < maxWidth || maxWidth < 0) {
	    Array((_getNextSequence1(sequence1, sequence2), _join1(table2, table1)), (_getNextSequence2(sequence1, sequence2), _join2(table2, table1, minGap, maxGap, window)))
	  } else {
	    Array((_getNextSequence2(sequence1, sequence2), _join2(table2, table1, minGap, maxGap, window)))
	  }
        } else {
	  Array((_getNextSequence2(sequence1, sequence2), _join2(table2, table1, minGap, maxGap, window)))
	}
      } else if (joinType == 3) {
	Array((_getNextSequence2(sequence1, sequence2), _join2(table1, table2, minGap, maxGap, window)))
      } else {
	Array()
      }
    }

    var resultCount: Long = oneSequences.count()
    var totalSize: Long = 0
    var numCollect: Long = 0
    var numBroadcast: Long = 0
    var filteredSize: Long = 0
    val numSteps: Int = 1
    def processOneLevel(kSequencesRDD: RDD[(Sequence, Table)], k: Int, prevSeqToFreq: scala.collection.Map[List[Vector[Int]], Long]): Unit = {
      for (i <- 0 until numFreqItems by numSteps) {
        val rightSequenceList = kSequencesRDD.filter(node => i <= node._1.last.last && node._1.last.last < i + numSteps).collect.toList
        var bArr = new Array[org.apache.spark.broadcast.Broadcast[Node]](rightSequenceList.size)
        for (j <- 0 until rightSequenceList.size) {
          numCollect = numCollect + 1
	  bArr(j) = dataset.sparkContext.broadcast(rightSequenceList(j))
        }
        if (k == maxPatternLength - 1) {
	  var resultRDD = kSequencesRDD.flatMap { rightNode: (Sequence, Table) =>
            (for (rightSequences <- bArr.iterator;
                  leftNode = rightSequences.value;
                  if _getJoinType(leftNode._1, rightNode._1) >= 0) yield getNextNodes(leftNode, rightNode)
                    .filter(x => x._2.size >= minCount)).flatten
                    .filter { x =>
                      x._1.size < 2 || {
		        val confidence = x._2.size.toDouble / prevSeqToFreq(x._1.slice(0, x._1.size - 1)).toDouble
		        confidence >= minConfidence
                      }
                    }
                    .filter { x =>
                      x._1.size < 2 || {
		        val lift = x._2.size.toDouble * totalCount.toDouble / prevSeqToFreq(x._1.slice(0, x._1.size - 1)).toDouble / oneSeqToFreq(x._1.last.last).toDouble
		        lift >= minLift
                      }
                    }
                  .filter { x =>
                    if (removeRepetitivePattern) {
                      val arr = x._1.flatMap(y => y).toArray.sorted
                      var check = true
                      for (k <- 1 until arr.size) {
                        if (arr(k) == arr(k - 1)) check = false
                      }
                      check
                    } else {
                      true
                    }
                  }
	  }
          resultCount = resultCount + resultRDD.map(_._1).count()
          getResultRDD(resultRDD).toDF.withColumnRenamed("_1", "sequence").withColumnRenamed("_2", "freq").withColumnRenamed("_3", "sids").write.parquet("/user/root/assets/" + dataset.sparkContext.applicationId + "-" + fileCount)
          fileCount = fileCount + 1
	} else {
	  for (j <- 0 until rightSequenceList.size) {
            val rightSequences = bArr(j)
	    var resultRDD = kSequencesRDD.flatMap { rightNode: (Sequence, Table) =>
	      val leftNode = rightSequences.value
	      if (_getJoinType(leftNode._1, rightNode._1) >= 0) {
		getNextNodes(leftNode, rightNode)
		  .filter(x => x._2.size >= minCount)
		  .filter { x =>
                    x._1.size < 2 || {
		      val confidence = x._2.size.toDouble / prevSeqToFreq(x._1.slice(0, x._1.size - 1)).toDouble
		      confidence >= minConfidence
                    }
		  }
		  .filter { x =>
                    x._1.size < 2 || {
		      val lift = x._2.size.toDouble * totalCount.toDouble / prevSeqToFreq(x._1.slice(0, x._1.size - 1)).toDouble / oneSeqToFreq(x._1.last.last).toDouble
		      lift >= minLift
                    }
		  }
                  .filter { x =>
                    if (removeRepetitivePattern) {
                      val arr = x._1.flatMap(y => y).toArray.sorted
                      var check = true
                      for (k <- 1 until arr.size) {
                        if (arr(k) == arr(k - 1)) check = false
                      }
                      check
                    } else {
                      true
                    }
                  }
	      } else {
		Array[(Sequence, Table)]()
	      }
	    }
	    val itemsetRDD = resultRDD.filter(x => _isItemset(x._1))
	    val sequenceRDD = resultRDD.filter(x => !_isItemset(x._1))
	    resultRDD = itemsetRDD.union(sequenceRDD)
	    resultRDD.persist(StorageLevel.MEMORY_AND_DISK)
	    resultCount = resultCount + resultRDD.map(_._1).count()
	    getResultRDD(resultRDD).toDF.withColumnRenamed("_1", "sequence").withColumnRenamed("_2", "freq").withColumnRenamed("_3", "sids").write.parquet("/user/root/assets/" + dataset.sparkContext.applicationId + "-" + fileCount)
	    fileCount = fileCount + 1
	    processOneLevel(resultRDD, k + 1, (prevSeqToFreq.toList ++ resultRDD.map(x => (x._1, x._2.size.toLong)).collect().toList).toMap)
	    resultRDD.unpersist()
	  }
	}
        for (j <- 0 until bArr.size) {
          bArr(j).destroy()
        }
      }
    }
    /* 2-sequences */
    if (maxPatternLength > 1) {
      var kSequencesRDDs = processOneLevel(oneSequences, 1, oneSequences.map(x => (x._1, x._2.size.toLong)).collectAsMap)
    }

    println("# of patterns: " + resultCount)

    if (resultCount > 0) getResultRDD(oneSequences)
    else getResultRDD(oneSequences)
  }

  private[fpm] def processMaxGapPrefixVer(
    df: DataFrame,
    dataset: RDD[(Int, Int, Array[Int])],
    minCount: Long,
    maxPatternLength: Int,
    maxWidth: Int,
    minGap: Int,
    maxGap: Int,
    window: Int,
    minConfidence: Double,
    minLift: Double,
    numFreqItems: Int,
    freqItems: Array[Int],
    totalCount: Long,
    removeRepetitivePattern: Boolean): RDD[(Array[Array[Int]], Long, Array[Int])] = {
    //import org.apache.spark.util.SizeEstimator
    
    val t0 = System.currentTimeMillis
    /* 1-sequences */
    val partitioner = new CustomPartitioner(2 * numFreqItems)
    val oneSequences = (dataset.flatMap { transaction =>
      for (item <- transaction._3) yield (transaction._1, transaction._2, item)
    }.groupBy(_._3)
    .mapValues(x => x.groupBy(_._1).mapValues(y => y.map(z => (z._2, z._2)).toArray.sorted).toArray.sortBy(_._1))
    .map { case (item: Int, data: Table) =>
      val sequence = List[Vector[Int]](Vector[Int](item))
      (sequence, data)
    }).partitionBy(partitioner)
    .persist(StorageLevel.MEMORY_AND_DISK)

    def getResultRDD(rdd: RDD[(Sequence, Table)]): RDD[(Array[Array[Int]], Long, Array[Int])] = {
      rdd.map { case (sequence: Sequence, data: Table) =>
	(sequence.map(x => x.map(item => freqItems(item)).toArray).toArray, data.size.toLong, data.map(_._1))
      }
    }

    val oneSeqToFreq = oneSequences.map(x => (x._1.last.last, x._2.size.toLong)).collectAsMap
    var result = getResultRDD(oneSequences)
    var fileCount: Long = 0
    import df.sparkSession.implicits._
    result.toDF.withColumnRenamed("_1", "sequence").withColumnRenamed("_2", "freq").withColumnRenamed("_3", "sids").write.parquet("/user/root/assets/" + dataset.sparkContext.applicationId + "-" + fileCount)
    fileCount = fileCount + 1
    println("# of freqItems: " + result.count)

    def _isItemset(sequence: Sequence): Boolean = sequence.last.size != 1

    def _getJoinType(sequence1: Sequence, sequence2: Sequence): Short = {
      if (sequence1.size == 1 && sequence1(0).size == 1) {
        if (sequence1(0)(0) < sequence2(0)(0)) {
          4: Short
        } else {
          5: Short
        }
      } else {
	if (_isItemset(sequence1) && _isItemset(sequence2) && sequence1.last.last < sequence2.last.last) {
	  0: Short
	} else if (_isItemset(sequence1) && !_isItemset(sequence2)) {
	  1: Short
	} else if (!_isItemset(sequence1) && _isItemset(sequence2)) {
	  2: Short
	} else if (!_isItemset(sequence1) && !_isItemset(sequence2)) {
	  3: Short
	} else {
	  -1: Short
	}
      }
    }

    def _join1(table1: Table, table2: Table): Table = {
      var acc = mutable.ArrayBuilder.make[(Int, Array[(Int, Int)])]
      var i: Int = 0
      var j: Int = 0
      val n = table1.length
      val m = table2.length
      while (i < n && j < m) {
	val sid1: Int = table1(i)._1
	val sid2: Int = table2(j)._1
	if (sid1 == sid2) {
	  var k: Int = 0
	  var l: Int = 0
	  val dataOfSid1: Array[(Int, Int)] = table1(i)._2
	  val dataOfSid2: Array[(Int, Int)] = table2(j)._2
	  var newData = mutable.ArrayBuilder.make[(Int, Int)]
	  val l1 = dataOfSid1.length
	  val l2 = dataOfSid2.length
	  while (k < l1 && l < l2) {
	    val eid1 = dataOfSid1(k)._1
	    val eid2 = dataOfSid2(l)._1
	    if (eid1 == eid2) {
	      val tmp = (eid1, math.max(dataOfSid1(k)._2, dataOfSid2(l)._2))
	      newData += tmp
	      k = k + 1
	      l = l + 1
	    } else if (eid1 < eid2) {
	      k = k + 1
	    } else {
	      l = l + 1
	    }
	  }
	  if (newData.result().size > 0) {
	    val tmp = (sid1, newData.result().toArray)
	    acc += tmp
	  }
	  i = i + 1
	  j = j + 1
	} else if (sid1 < sid2) {
	  i = i + 1
	} else {
	  j = j + 1
	}
      }
      acc.result()
    }

    def _join2(table1: Table, table2: Table, minGap: Int, maxGap: Int, window: Int): Table = {
      var acc = mutable.ArrayBuilder.make[(Int, Array[(Int, Int)])]
      var i: Int = 0
      var j: Int = 0
      val n: Int = table1.length
      val m: Int = table2.length
      val minGap2 = math.max(1, minGap)
      while (i < n && j < m) {
	val sid1: Int = table1(i)._1
	val sid2: Int = table2(j)._1
	if (sid1 == sid2) {
	  var k: Int = 0
	  var l: Int = 0
	  val dataOfSid1: Array[(Int, Int)] = table1(i)._2
	  val dataOfSid2: Array[(Int, Int)] = table2(j)._2
	  var newData = mutable.ArrayBuilder.make[(Int, Int)]
	  val l1 = dataOfSid1.length
	  val l2 = dataOfSid2.length
	  while (k < l1 && l < l2) {
	    val eid1 = dataOfSid1(k)._1
	    val eid2 = dataOfSid2(l)._1
	    if (eid1 < eid2 + minGap2) {
	      k = k + 1
	    } else {
	      if ((maxGap < 0 || (maxGap > 0 && eid1 - eid2 <= maxGap)) &&
		(window < 0 || (eid1 - dataOfSid2(l)._2 <= window))) {
		val tmp = (eid1, math.min(dataOfSid2(l)._2, dataOfSid1(k)._2))
		newData += tmp
                k = k + 1
	      } else {
                l = l + 1
              }
	    }
	  }
	  if (newData.result().size > 0) {
	    val tmp = (sid1, newData.result().toArray)
	    acc += tmp
	  }
	  i = i + 1
	  j = j + 1
	} else if (sid1 < sid2) {
	  i = i + 1
	} else {
	  j = j + 1
	}
      }
      acc.result()
    }

    def _getNextSequence1(sequence1: Sequence, sequence2: Sequence): Sequence = sequence1.dropRight(1) ++ List(sequence1.last ++ Vector(sequence2.last.last))

    def _getNextSequence2(sequence1: Sequence, sequence2: Sequence): Sequence = sequence1 ++ List(sequence2.last)

    def getNextNodes(node1: Node, node2: Node): Array[Node] = {
      val sequence1 = node1._1
      val table1 = node1._2
      val sequence2 = node2._1
      val table2 = node2._2
      val joinType = _getJoinType(sequence1, sequence2)
      if (joinType == 0 || joinType == 2) {
	if (sequence1.last.size < maxWidth || maxWidth < 0) {
	  Array((_getNextSequence1(sequence1, sequence2), _join1(table2, table1)))
        } else {
          Array()
        }
      } else if (joinType == 1 || joinType == 3 || joinType == 5) {
        Array((_getNextSequence2(sequence1, sequence2), _join2(table2, table1, minGap, maxGap, window)))
      } else if (joinType == 4) {
	if (sequence1.last.size < maxWidth || maxWidth < 0) {
	  Array((_getNextSequence1(sequence1, sequence2), _join1(table2, table1)), (_getNextSequence2(sequence1, sequence2), _join2(table2, table1, minGap, maxGap, window)))
        } else {
	  Array((_getNextSequence2(sequence1, sequence2), _join2(table2, table1, minGap, maxGap, window)))
        }
      } else {
	Array()
      }
    }

    var resultCount: Long = oneSequences.count()
    var totalSize: Long = 0
    var numCollect: Long = 0
    var numBroadcast: Long = 0
    var filteredSize: Long = 0
    val numSteps: Int = 1
    def getTwoSequences(kSequencesRDD: RDD[(Sequence, Table)], oneSequencesRDD: RDD[(Sequence, Table)], prevSeqToFreq: scala.collection.Map[List[Vector[Int]], Long]): RDD[(Sequence, Table)] = {
      var result = dataset.sparkContext.emptyRDD[(Sequence, Table)]
      for (i <- 0 until numFreqItems by numSteps) {
        val leftSequenceList = kSequencesRDD.filter(node => i <= node._1.last.last && node._1.last.last < i + numSteps).collect.toList // delete toList
        var bArr = new Array[org.apache.spark.broadcast.Broadcast[Node]](leftSequenceList.size)
        for (j <- 0 until leftSequenceList.size) {
          numCollect = numCollect + 1
	  bArr(j) = dataset.sparkContext.broadcast(leftSequenceList(j))
        }
	var resultRDD = oneSequencesRDD.flatMap { rightNode: (Sequence, Table) => // first item
	  (for (leftSequences <- bArr.iterator;
		leftNode = leftSequences.value;
		if _getJoinType(leftNode._1, rightNode._1) >= 0) yield getNextNodes(leftNode, rightNode)
		  .filter(x => x._2.size >= minCount)).flatten
		  .filter { x =>
                    x._1.size < 2 || {
		      val confidence = x._2.size.toDouble / prevSeqToFreq(x._1.slice(0, x._1.size - 1)).toDouble
		      confidence >= minConfidence
                    }
		  }
		  .filter { x =>
                    x._1.size < 2 || {
		      val lift = x._2.size.toDouble * totalCount.toDouble / prevSeqToFreq(x._1.slice(0, x._1.size - 1)).toDouble / oneSeqToFreq(x._1.last.last).toDouble
		      lift >= minLift
                    }
		  }
                  .filter { x =>
                    if (removeRepetitivePattern) {
                      val arr = x._1.flatMap(y => y).toArray.sorted
                      var check = true
                      for (k <- 1 until arr.size) {
                        if (arr(k) == arr(k - 1)) check = false
                      }
                      check
                    } else {
                      true
                    }
                  }
	}
        resultCount = resultCount + resultRDD.map(_._1).count()
        getResultRDD(resultRDD).toDF.withColumnRenamed("_1", "sequence").withColumnRenamed("_2", "freq").withColumnRenamed("_3", "sids").write.parquet("/user/root/assets/" + dataset.sparkContext.applicationId + "-" + fileCount)
        fileCount = fileCount + 1
        result = result ++ resultRDD
      }
      result
    }

    def processOneLevel(kSequencesRDD: RDD[(Sequence, Table)], twoSequencesRDD: RDD[(Sequence, Table)], k: Int, prevSeqToFreq: scala.collection.Map[List[Vector[Int]], Long]): Unit = {
      for (i <- 0 until numFreqItems by numSteps) {
        val leftSequenceList = kSequencesRDD.filter(node => i <= node._1.last.last && node._1.last.last < i + numSteps).collect.toList
        var bArr = new Array[org.apache.spark.broadcast.Broadcast[Node]](leftSequenceList.size)
        for (j <- 0 until leftSequenceList.size) {
          numCollect = numCollect + 1
	  bArr(j) = dataset.sparkContext.broadcast(leftSequenceList(j))
        }
        if (k == maxPatternLength - 1) {
	  var resultRDD = twoSequencesRDD.flatMap { rightNode: (Sequence, Table) =>
            (for (leftSequences <- bArr.iterator;
                  leftNode = leftSequences.value;
                  if leftNode._1.last.last == rightNode._1(0)(0);
                  if _getJoinType(leftNode._1, rightNode._1) >= 0) yield getNextNodes(leftNode, rightNode)
                    .filter(x => x._2.size >= minCount)).flatten
                    .filter { x =>
                      x._1.size < 2 || {
                        val confidence = x._2.size.toDouble / prevSeqToFreq(x._1.slice(0, x._1.size - 1)).toDouble
		        confidence >= minConfidence
                      }
                    }
                    .filter { x =>
                      x._1.size < 2 || {
		        val lift = x._2.size.toDouble * totalCount.toDouble / prevSeqToFreq(x._1.slice(0, x._1.size - 1)).toDouble / oneSeqToFreq(x._1.last.last).toDouble
		        lift >= minLift
                      }
                    }
		    .filter { x =>
		      if (removeRepetitivePattern) {
		        val arr = x._1.flatMap(y => y).toArray.sorted
		        var check = true
		        for (k <- 1 until arr.size) {
			  if (arr(k) == arr(k - 1)) check = false
		        }
		        check
		      } else {
		        true
		      }
		    }
	  }
          resultCount = resultCount + resultRDD.map(_._1).count()
          getResultRDD(resultRDD).toDF.withColumnRenamed("_1", "sequence").withColumnRenamed("_2", "freq").withColumnRenamed("_3", "sids").write.parquet("/user/root/assets/" + dataset.sparkContext.applicationId + "-" + fileCount)
	  fileCount = fileCount + 1
	} else {
	  for (j <- 0 until leftSequenceList.size) {
            val leftSequences = bArr(j)
	    var resultRDD = twoSequencesRDD.flatMap { rightNode: (Sequence, Table) =>
	      val leftNode = leftSequences.value
	      if (leftNode._1.last.last == rightNode._1(0)(0) && _getJoinType(leftNode._1, rightNode._1) >= 0) {
		getNextNodes(leftNode, rightNode)
		  .filter(x => x._2.size >= minCount)
		  .filter { x =>
                    x._1.size < 2 || {
		      val confidence = x._2.size.toDouble / prevSeqToFreq(x._1.slice(0, x._1.size - 1)).toDouble
		      confidence >= minConfidence
                    }
		  }
		  .filter { x =>
                    x._1.size < 2 || {
		      val lift = x._2.size.toDouble * totalCount.toDouble / prevSeqToFreq(x._1.slice(0, x._1.size - 1)).toDouble / oneSeqToFreq(x._1.last.last).toDouble
		      lift >= minLift
                    }
		  }
                  .filter { x =>
                    if (removeRepetitivePattern) {
                      val arr = x._1.flatMap(y => y).toArray.sorted
                      var check = true
                      for (k <- 1 until arr.size) {
                        if (arr(k) == arr(k - 1)) check = false
                      }
                      check
                    } else {
                      true
                    }
                  }
	      } else {
		Array[(Sequence, Table)]()
	      }
	    }
	    resultRDD.persist(StorageLevel.MEMORY_AND_DISK)
	    resultCount = resultCount + resultRDD.map(_._1).count()
	    getResultRDD(resultRDD).toDF.withColumnRenamed("_1", "sequence").withColumnRenamed("_2", "freq").withColumnRenamed("_3", "sids").write.parquet("/user/root/assets/" + dataset.sparkContext.applicationId + "-" + fileCount)
	    fileCount = fileCount + 1
	    processOneLevel(resultRDD, twoSequencesRDD, k + 1, (prevSeqToFreq.toList ++ resultRDD.map(x => (x._1, x._2.size.toLong)).collect().toList).toMap)
	    resultRDD.unpersist()
	  }
	}
        for (j <- 0 until bArr.size) {
          bArr(j).destroy()
        }
      }
    }

    if (maxPatternLength > 1) {
      /* 2-sequences */
      val oneSequencesList = oneSequences.map(x => (x._1, x._2.size.toLong)).collect().toList
      val twoSequences = getTwoSequences(oneSequences, oneSequences, oneSequencesList.toMap)

      if (maxPatternLength > 2) {
        for (item <- 0 until numFreqItems) {
          println("Progress: " + item + " / " + numFreqItems)
          var kSequencesRDDs = processOneLevel(twoSequences.filter(_._1(0)(0) == item), twoSequences, 2, (oneSequencesList ++ twoSequences.map(x => (x._1, x._2.size.toLong)).collect().toList).toMap)
        }
      }
    }

    println("# of patterns: " + resultCount)

    if (resultCount > 0) getResultRDD(oneSequences)
    else getResultRDD(oneSequences)
  }

  def getResultRDD(rdd: RDD[(Sequence, Table)]): RDD[(Array[Array[Int]], Long, Array[Int])] = {
    rdd.map { case (sequence: Sequence, data: Table) =>
      (sequence.map(_.toArray).toArray, data.size.toLong, data.map(_._1))
    }
  }

  private[fpm] def genFreqPatterns(
    df: DataFrame,
    dataset: RDD[(Int, Int, Array[Int])],
    minCount: Long,
    maxPatternLength: Int,
    maxWidth: Int,
    minGap: Int,
    maxGap: Int,
    window: Int,
    numFreqItems: Int): RDD[(Array[Array[Int]], Long, Array[Int])] = {
    println("genFreqPatterns()")
    import df.sparkSession.implicits._
    
    def getSequencePairs(sequences: Array[(Sequence, Table)]): Array[(Sequence, Table, Sequence, Table, Int)] = {
      val numSequences: Int = sequences.length
      val sequenceSet: Array[(Sequence, Table)] = sequences.filter(x => !isItemset(x._1))
      var sequenceSet2 = sequenceSet
      val itemsets: Array[(Sequence, Table)] = sequences.filter(x => isItemset(x._1))
      var itemsets2 = itemsets
      var numSequenceSet: Int = sequenceSet.length
      var numItemsets: Int = itemsets.length
      val acc1 = (for (i <- 0 until numItemsets;
	j <- i + 1 until numItemsets;
	if (itemsets(i)._1(0)(0) != itemsets2(j)._1(0)(0))) yield if (itemsets(i)._1(0)(0) < itemsets2(j)._1(0)(0)) (itemsets(i)._1, itemsets(i)._2, itemsets2(j)._1, itemsets2(j)._2, 0) else (itemsets2(j)._1, itemsets2(j)._2, itemsets(i)._1, itemsets(i)._2, 0)).toArray
      val acc2 = (for (i <- 0 until numSequenceSet;
	j <- 0 until numItemsets) yield (sequenceSet(i)._1, sequenceSet(i)._2, itemsets2(j)._1, itemsets2(j)._2, 1)).toArray
      val acc3 = (for (i <- 0 until numSequenceSet;
	j <- 0 until numSequenceSet) yield if (sequenceSet(i)._1(0)(0) != sequenceSet2(j)._1(0)(0)) (sequenceSet(i)._1, sequenceSet(i)._2, sequenceSet2(j)._1, sequenceSet2(j)._2, 2) else (sequenceSet(i)._1, sequenceSet(i)._2, sequenceSet2(j)._1, sequenceSet2(j)._2, 3)).toArray
      acc1 ++ acc2 ++ acc3
    }

    def groupBySuffix(sequencePairs: Array[(Sequence, Table, Sequence, Table, Int)]): Array[Array[(Sequence, Table, Sequence, Table, Int)]] = {
      sequencePairs.groupBy(x => x._3).mapValues(x => x.map(y => (y._1, y._2, y._3, y._4, y._5)).toArray).values.toArray
    }
    def getSequenceAndSupport(rdd: RDD[Array[(Sequence, Table)]]): RDD[(Array[Array[Int]], Long, Array[Int])] = {
      rdd.flatMap { case (sequences: Array[(Sequence, Table)]) =>
      sequences.map(x => (x._1.map(y => y.toArray).toArray, x._2.size.toLong, x._2.map(_._1)))
      }
    }

    def getSuffix(sequence: Sequence): Sequence = {
      if (sequence(0).size == 1) {
	sequence.drop(1)
      } else {
	List[Vector[Int]](sequence(0).drop(1)) ++ sequence.drop(1)
      }
    }

    def isItemset(sequence: Sequence): Boolean = sequence(0).size != 1

    def getJoinType(sequence1: Sequence, sequence2: Sequence): Short = {
      if (isItemset(sequence1) && isItemset(sequence2) && sequence1(0)(0) < sequence2(0)(0)) {
	0: Short
      } else if (!isItemset(sequence1) && isItemset(sequence2)) {
	1: Short
      } else if (!isItemset(sequence1) && !isItemset(sequence2) && sequence1(0)(0) != sequence2(0)(0)) {
	2: Short
      } else if (!isItemset(sequence1) && !isItemset(sequence2) && sequence1(0)(0) == sequence2(0)(0)) {
	3: Short
      } else {
	-1: Short
      }
    }

    def getNextSequence1(sequence1: Sequence, sequence2: Sequence): Sequence = (sequence1(0)(0) +: sequence2(0)) +: sequence1.drop(1)

    def getNextSequence2(sequence1: Sequence, sequence2: Sequence): Sequence = sequence1(0) +: sequence2 //  List(sequence1(0)) ++ sequence2

    def join1(table1: Table, table2: Table): Table = {
      var acc = mutable.ArrayBuilder.make[(Int, Array[(Int, Int)])]
      var i: Int = 0
      var j: Int = 0
      val n = table1.length
      val m = table2.length
      while (i < n && j < m) {
	val sid1: Int = table1(i)._1
	val sid2: Int = table2(j)._1
	if (sid1 == sid2) {
	  var k: Int = 0
	  var l: Int = 0
	  val dataOfSid1: Array[(Int, Int)] = table1(i)._2
	  val dataOfSid2: Array[(Int, Int)] = table2(j)._2
	  var newData = mutable.ArrayBuilder.make[(Int, Int)]
	  val l1 = dataOfSid1.length
	  val l2 = dataOfSid2.length
	  while (k < l1 && l < l2) {
	    val eid1 = dataOfSid1(k)._1
	    val eid2 = dataOfSid2(l)._1
	    if (eid1 == eid2) {
	      val tmp = (eid1, math.max(dataOfSid1(k)._2, dataOfSid2(l)._2))
	      newData += tmp
	      k = k + 1
	      l = l + 1
	    } else if (eid1 < eid2) {
	      k = k + 1
	    } else {
	      l = l + 1
	    }
	  }
	  if (newData.result().size > 0) {
	    val tmp = (sid1, newData.result().toArray)
	    acc += tmp
	  }
	  i = i + 1
	  j = j + 1
	} else if (sid1 < sid2) {
	  i = i + 1
	} else {
	  j = j + 1
	}
      }
      acc.result()
    }

    def join2(table1: Table, table2: Table, minGap: Int, maxGap: Int, window: Int): Table = {
      var acc = mutable.ArrayBuilder.make[(Int, Array[(Int, Int)])]
      var i: Int = 0
      var j: Int = 0
      val n: Int = table1.length
      val m: Int = table2.length
      while (i < n && j < m) {
	val sid1: Int = table1(i)._1
	val sid2: Int = table2(j)._1
	if (sid1 == sid2) {
	  var k: Int = 0
	  var l: Int = 0
	  val dataOfSid1: Array[(Int, Int)] = table1(i)._2
	  val dataOfSid2: Array[(Int, Int)] = table2(j)._2
	  var newData = mutable.ArrayBuilder.make[(Int, Int)]
	  val l1 = dataOfSid1.length
	  val l2 = dataOfSid2.length
	  if (maxGap < 0) {
	    l = l2 - 1
	  }
	  while (k < l1 && l < l2) {
	    val eid1 = dataOfSid1(k)._1
	    val eid2 = dataOfSid2(l)._1
	    if (eid1 < eid2) {
	      if ((maxGap < 0 || (maxGap > 0 && eid2 - eid1 <= maxGap)) &&
		(minGap < 0 || (eid2 - eid1 >= minGap)) &&
		(window < 0 || (dataOfSid2(l)._2 - dataOfSid1(k)._2 <= window))) {
		val tmp = (eid1, math.max(dataOfSid1(k)._2, dataOfSid2(l)._2))
		newData += tmp
	      }
	      k = k + 1
	    } else {
	      l = l + 1
	    }
	  }
	  if (newData.result().size > 0) {
	    val tmp = (sid1, newData.result().toArray)
	    acc += tmp
	  }
	  i = i + 1
	  j = j + 1
	} else if (sid1 < sid2) {
	  i = i + 1
	} else {
	  j = j + 1
	}
      }
      acc.result()
    }
		def computeNextSequences(sequencePairs: Array[(Sequence, Table, Sequence, Table, Int)]): Array[(Sequence, Table)] = {
			(for ((sequence1: Sequence, data1: Table, sequence2: Sequence, data2: Table, joinType: Int) <- sequencePairs) yield getNextNodes((sequence1, data1), (sequence2, data2)).filter { case (sequence: Sequence, data: Table) =>
				data.size >= minCount
			} ).flatten
		}

		def getNextNodes(node1: Node, node2: Node): Array[Node] = {
			val sequence1 = node1._1
			val table1 = node1._2
			val sequence2 = node2._1
			val table2 = node2._2
			val joinType = getJoinType(sequence1, sequence2)
			if (joinType == 0) {
				if (sequence1(0).size < maxWidth || maxWidth < 0) {
					Array((getNextSequence1(sequence1, sequence2), join1(table1, table2)))
				} else {
					Array()
				}
			} else if (joinType == 1) {
				Array((getNextSequence2(sequence1, sequence2), join2(table1, table2, minGap, maxGap, window)))
			} else if (joinType == 2) {
				if (sequence1(0)(0) < sequence2(0)(0)) {
					if (sequence1(0).size < maxWidth || maxWidth < 0) {
						Array((getNextSequence1(sequence1, sequence2), join1(table1, table2)), (getNextSequence2(sequence1, sequence2), join2(table1, table2, minGap, maxGap, window)))
					} else {
						Array((getNextSequence2(sequence1, sequence2), join2(table1, table2, minGap, maxGap, window)))
					}
				} else {
					Array((getNextSequence2(sequence1, sequence2), join2(table1, table2, minGap, maxGap, window)))
				}
			} else if (joinType == 3) {
				Array((getNextSequence2(sequence1, sequence2), join2(table1, table2, minGap, maxGap, window)))
			} else if (joinType == 4) {
				if (sequence2(0).size < maxWidth || maxWidth < 0) {
					Array((getNextSequence1(sequence2, sequence1), join1(table2, table1)))
				} else {
					Array()
				}
			} else if (joinType == 5) {
				Array((getNextSequence2(sequence2, sequence1), join2(table2, table1, minGap, maxGap, window)))
			} else {
				Array()
			}
		}

    var rdd = (dataset.flatMap { transaction =>
      for (item <- transaction._3) yield (transaction._1, transaction._2, item)
    }.groupBy(_._3)
    .mapValues(x => x.groupBy(_._1).mapValues(y => y.map(z => (z._2, z._2)).toArray.sorted).toArray.sortBy(_._1))
    .map { case (item: Int, data: Table) =>
      val sequence = List[Vector[Int]](Vector[Int](item))
      (sequence, data)
    }
    .groupBy(x => getSuffix(x._1))
    .mapValues(x => x.map(y => (y._1, y._2)).toArray)
    .map(_._2))

    var result: RDD[(Array[Array[Int]], Long, Array[Int])] = getSequenceAndSupport(rdd)
    //getDistOfPartitionSizes(rdd)

    var sequencePairs = rdd.flatMap { case sequences: Array[(Sequence, Table)] =>
      groupBySuffix(getSequencePairs(sequences))
    }.repartition(dataset.getNumPartitions)

    var i: Int = 2
    while (i <= maxPatternLength || maxPatternLength < 0) {
      rdd = sequencePairs.map { case x: Array[(Sequence, Table, Sequence, Table, Int)] =>
        computeNextSequences(x)
      }.filter(_.size > 0)
      result = result.union(getSequenceAndSupport(rdd))
      sequencePairs = rdd.flatMap(x => groupBySuffix(getSequencePairs(x)))
      i = i + 1
    }

    result.toDF.withColumnRenamed("_1", "sequence").withColumnRenamed("_2", "freq").withColumnRenamed("_3", "sids").write.parquet("/user/root/assets/" + dataset.sparkContext.applicationId)

    result
  }


  class FreqSequence[Item](val sequence: Array[Array[Item]], val freq: Long, val sids: Array[Int]) extends Serializable {
    def javaSequence: ju.List[ju.List[Item]] = sequence.map(_.toList.asJava).toList.asJava
  }
}

class CSpadeModel[Item](val freqSequences: RDD[CSpade.FreqSequence[Item]]) extends Saveable with Serializable {
  override def save(sc: SparkContext, path: String): Unit = {
    CSpadeModel.SaveLoadV1_0.save(this, path)
  }
}

object CSpadeModel extends Loader[CSpadeModel[_]] {
  override def load(sc: SparkContext, path: String): CSpadeModel[_] = {
    CSpadeModel.SaveLoadV1_0.load(sc, path)
  }

  private[fpm] object SaveLoadV1_0 {

    private val thisFormatVersion = "1.0"

    private val thisClassName = "org.apache.spark.mllib.fpm.CSpadeModel"

    def save(model: CSpadeModel[_], path: String): Unit = {
      val sc = model.freqSequences.sparkContext
      val spark = SparkSession.builder().sparkContext(sc).getOrCreate()

      val metadata = compact(render(
        ("class" -> thisClassName) ~ ("version" -> thisFormatVersion)))
      sc.parallelize(Seq(metadata), 1).saveAsTextFile(Loader.metadataPath(path))

      // Get the type of item class
      val sample = model.freqSequences.first().sequence(0)(0)
      val className = sample.getClass.getCanonicalName
      val classSymbol = runtimeMirror(getClass.getClassLoader).staticClass(className)
      val tpe = classSymbol.selfType

      val itemType = ScalaReflection.schemaFor(tpe).dataType
      val fields = Array(StructField("sequence", ArrayType(ArrayType(itemType))),
        StructField("freq", LongType))
      val schema = StructType(fields)
      val rowDataRDD = model.freqSequences.map { x =>
        Row(x.sequence, x.freq)
      }
      spark.createDataFrame(rowDataRDD, schema).write.parquet(Loader.dataPath(path))
    }

    def load(sc: SparkContext, path: String): CSpadeModel[_] = {
      implicit val formats = DefaultFormats
      val spark = SparkSession.builder().sparkContext(sc).getOrCreate()

      val (className, formatVersion, metadata) = Loader.loadMetadata(sc, path)
      assert(className == thisClassName)
      assert(formatVersion == thisFormatVersion)

      val freqSequences = spark.read.parquet(Loader.dataPath(path))
      val sample = freqSequences.select("sequence").head().get(0)
      loadImpl(freqSequences, sample)
    }

    def loadImpl[Item: ClassTag](freqSequences: DataFrame, sample: Item): CSpadeModel[Item] = {
      val freqSequencesRDD = freqSequences.select("sequence", "freq").rdd.map { x =>
        val sequence = x.getAs[Seq[Seq[Item]]](0).map(_.toArray).toArray
        val freq = x.getLong(1)
        val sids = x.getAs[Seq[Int]](2).toArray
        new CSpade.FreqSequence(sequence, freq, sids)
      }
      new CSpadeModel(freqSequencesRDD)
    }
  }
}

/*
import scala.reflect.runtime.universe._

class CustomPartitioner(override val numPartitions: Int) extends Partitioner {
  //import CSpade._
  def getPartition[T: TypeTag](key: T): Int = typeOf[T] match {
    case x if x =:= typeOf[Int] => key.asInstanceOf[Int] % numPartitions
    //case x: (Sequence, Sequence) => {
    case x if x =:= typeOf[(List[Vector[Int]], List[Vector[Int]])]  => {
      val k = key.asInstanceOf[(List[Vector[Int]], List[Vector[Int]])]
      val first = if (k._1(0).size == 1) 2 * k._1(0)(0) else 2 * k._1(0)(0) + 1
      val second = if (k._2(0).size == 1) 2 * k._2(0)(0) else 2 * k._2(0)(0) + 1
      math.pow(numPartitions, 0.5).toInt * first + second
    }
    case x if x =:= typeOf[List[Vector[Int]]] => {
      val k = key.asInstanceOf[List[Vector[Int]]]
      if (k(0).size == 1) 2 * k(0)(0) else 2 * k(0)(0) + 1
    }
    case x if x =:= typeOf[Any] => 0
    case _ => 0
    //case x: Sequence => if (x(0).size == 1) 2 * x(0)(0) else 2 * x(0)(0) + 1
  }
}*/
  
class CustomPartitioner(override val numPartitions: Int) extends Partitioner {
  //import CSpade._
  def getPartition(key: Any): Int = key match {
    case x: Int => x % numPartitions
    case x: (List[Vector[_]], List[Vector[_]]) @unchecked => {
      val k = x.asInstanceOf[(List[Vector[Int]], List[Vector[Int]])]
      val first = if (k._1(0).size == 1) 2 * k._1(0)(0) else 2 * k._1(0)(0) + 1
      val second = if (k._2(0).size == 1) 2 * k._2(0)(0) else 2 * k._2(0)(0) + 1
      math.pow(numPartitions, 0.5).toInt * first + second
    }
    case x: List[Vector[_]] @unchecked => {
      val k = x.asInstanceOf[List[Vector[Int]]]
      if (k(0).size == 1) 2 * k(0)(0) else 2 * k(0)(0) + 1
    }
  }
}

