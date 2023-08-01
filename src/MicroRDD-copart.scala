import scala.collection.mutable
import org.apache.spark.rdd.RDD
import org.apache.spark.Partitioner
import org.apache.spark.sql._
import org.apache.spark.storage.StorageLevel
import scala.util.Random
import scala.io.Source

class MicroRDD(operator: Int, f: Any => Any = null, f2: (Any, Any) => Any = null, f3: Any => Array[Any] = null, numPartitions: Int = -1) extends Serializable {
  import QaaD._

  val id: MicroRDDId = QaaD.microRddDag.size // identifier
  var parentList = Array[MicroRDDId]() // list of identifiers of parent MicroRDDs
  var childList = Array[MicroRDDId]() // list of identifiers of child MicroRDDs
  var operation = new Operation(operator, f, f2, f3, numPartitions)
  var counter: Int = 0

  def status: Status = { // status of this MicroRDD (DONE/READY/PENDING)
    if (this.counter == this.parentList.size) READY
    else if (this.counter < this.parentList.size) PENDING
    else DONE
  }

  def addChild(childMicroRDDId: MicroRDDId): Unit = {
    this.childList = this.childList :+ childMicroRDDId
  }

  def addParent(parentMicroRDDId: MicroRDDId): Unit = {
    this.parentList = this.parentList :+ parentMicroRDDId
  }

  def holdTriggerCondition(microRddDag: MicroRDDDAG): Boolean = {
    false
  }

  def qMap(f: Any => Any): MicroRDD = {
    val childMicroRdd = new MicroRDD(operator = MAP, f = f)
    childMicroRdd.addParent(this.id)
    this.addChild(childMicroRdd.id)
    QaaD.microRddDag.put(childMicroRdd.id, childMicroRdd)
    if (holdTriggerCondition(QaaD.microRddDag)) {
      QaaD.scheduler.run()
    }
    childMicroRdd
  }

  def qFlatMap(f: Any => Array[Any]): MicroRDD = {
    val childMicroRdd = new MicroRDD(operator = FLATMAP, f3 = f)
    childMicroRdd.addParent(this.id)
    this.addChild(childMicroRdd.id)
    QaaD.microRddDag.put(childMicroRdd.id, childMicroRdd)
    if (holdTriggerCondition(QaaD.microRddDag)) {
      QaaD.scheduler.run()
    }
    childMicroRdd
  }

  def qFilter(f: Any => Any): MicroRDD = {
    val childMicroRdd = new MicroRDD(operator = FILTER, f = f)
    childMicroRdd.addParent(this.id)
    this.addChild(childMicroRdd.id)
    QaaD.microRddDag.put(childMicroRdd.id, childMicroRdd)
    if (holdTriggerCondition(QaaD.microRddDag)) {
      QaaD.scheduler.run()
    }
    childMicroRdd
  }

  def qSample(frac: Float): MicroRDD = {
    def f(data: Any): Boolean = {
      Random.nextInt(100).toFloat / 100.0f < frac
    }
    this.qFilter(x => f(x))
  }

  def qJoin(other: MicroRDD): MicroRDD = {
    val childMicroRdd = new MicroRDD(operator = JOIN, f = f)
    childMicroRdd.addParent(this.id)
    childMicroRdd.addParent(other.id)
    this.addChild(childMicroRdd.id)
    other.addChild(childMicroRdd.id)
    QaaD.microRddDag.put(childMicroRdd.id, childMicroRdd)
    if (holdTriggerCondition(QaaD.microRddDag)) {
      QaaD.scheduler.run()
    }
    childMicroRdd
  }

  def qReduceByKey(f: (Any, Any) => Any, numPartitions: Int = -1): MicroRDD = {
    val childMicroRdd = new MicroRDD(operator = REDUCEBYKEY, f2 = f, numPartitions = numPartitions)
    childMicroRdd.addParent(this.id)
    this.addChild(childMicroRdd.id)
    QaaD.microRddDag.put(childMicroRdd.id, childMicroRdd)
    if (holdTriggerCondition(QaaD.microRddDag)) {
      QaaD.scheduler.run()
    }
    childMicroRdd
  }

  def qCollect(): Unit = {
    // Ignore actions of microRDD
  }

  def qCount(): MicroRDD = {
    def mapFunc1(data: Any): (Int, Long) = {
      (0, 1L)
    }
    def reduceFunc(d1: Any, d2: Any): (Int, Long) = {
      val row1 = d1.asInstanceOf[(Int, Long)]
      val row2 = d2.asInstanceOf[(Int, Long)]
      (row1._1, row1._2 + row2._2)
    }
    def mapFunc2(data: Any): Long = {
      val row = data.asInstanceOf[(Long, Long)]
      row._2
    }
    this.qMap(x => mapFunc1(x))
      .qReduceByKey((x, y) => reduceFunc(x, y))
      .qMap(x => mapFunc2(x))
  }
}

object QaaD extends Serializable {
  type MicroRDDId = Int
  type RDDId = Int
  type Operator = Int
  type OperatorType = Int
  type Status = Int
  type MicroRDDDAG = mutable.Map[MicroRDDId, MicroRDD]

  val DONE = 0
  val READY = 1
  val PENDING = 2
  
  val NARROW_DEPENDENCY = -1
  val MAP = 0
  val FLATMAP = 1
  val FILTER = 2
  val JOIN = 3
  val REDUCEBYKEY = 4

  val MEMORY_THRESHOLD = 999999999

  var microRddDag = mutable.Map[MicroRDDId, MicroRDD]()
  var microRddIdByInputName = mutable.Map[String, MicroRDDId]()
  val scheduler = new Scheduler()
  var rddArr = mutable.ArrayBuilder.make[RDD[(Int, Any)]]
  var partitionerByInputName = mutable.Map[String, Partitioner]()
  var keyPosByInputName = mutable.Map[String, Int]()
  for (line <- Source.fromFile(inputPath).getLines()) {
    partitionerByInputName.put(line, new IdPartitioner(globalNumPartitions))
    keyPosByInputName.put(line, 0)
  }
  var rddIdByMicroRddId = mutable.Map[MicroRDDId, RDDId]()
  var datasets = mutable.Map[String, RDD[((Int, String), Any)]]()
}

def qTextFile(inputName: String): MicroRDD = {
  if (!QaaD.microRddIdByInputName.contains(inputName)) {
    val microRdd = new MicroRDD(-1, null)
    val microRddId = QaaD.microRddDag.size
    QaaD.microRddDag.put(microRddId, microRdd)
    QaaD.microRddIdByInputName.put(inputName, microRddId)
    microRdd
  } else {
    QaaD.microRddDag(QaaD.microRddIdByInputName(inputName))
  }
}

class Scheduler() extends Serializable {
  import QaaD._

  def readDataset(inputName: String): RDD[(String, Any)] = {

    def arrayToTuple[A <: Object](array:Array[A]): Any = {
      val c = Class.forName("scala.Tuple" + array.size)
      c.getConstructors.apply(0).newInstance(array:_*).asInstanceOf[Any]
    }

    sc.textFile(inputName).map { line =>
      val sLine = line.replace("\"", "").replace("(", "").replace(")", "").split(",", -1)
      val tuple = arrayToTuple(sLine.slice(1, sLine.size))
      (sLine(0), tuple)
    }

  }

  def readDatasetToSeq(inputName: String): Seq[(String, Any)] = {
    def arrayToTuple[A <: Object](array:Array[A]): Any = {
      val c = Class.forName("scala.Tuple" + array.size)
      c.getConstructors.apply(0).newInstance(array:_*).asInstanceOf[Any]
    }
    import scala.io.Source
    var dataset = mutable.ArrayBuilder.make[(String, Any)]
    try {
      val keyPos = QaaD.keyPosByInputName(inputName)
      for (line <- Source.fromFile(inputName).getLines()) {
        val sLine = line.replace("\"", "").split(",", -1)
        if (!sLine.map(elem => elem.size).contains(0)) {
          val tuple = arrayToTuple(sLine)
          val row = (sLine(keyPos), tuple)
          dataset += row
        }
      }
    } catch {
      case ex: Exception => println(ex)
    }
    dataset.result().toSeq
  }

  def readDatasetToSeq2(inputName: String, microRddId: Int): Seq[((Int, String), Any)] = {
    def arrayToTuple[A <: Object](array:Array[A]): Any = {
      val c = Class.forName("scala.Tuple" + array.size)
      c.getConstructors.apply(0).newInstance(array:_*).asInstanceOf[Any]
    }
    import scala.io.Source
    var dataset = mutable.ArrayBuilder.make[((Int, String), Any)]
    try {
      val keyPos = QaaD.keyPosByInputName(inputName)
      for (line <- Source.fromFile(inputName).getLines()) {
        val sLine = line.replace("\"", "").split(",", -1)
        if (!sLine.map(elem => elem.size).contains(0)) {
          val tuple = arrayToTuple(sLine)
          val row = ((microRddId, sLine(keyPos)), tuple)
          dataset += row
        }
      }
    } catch {
      case ex: Exception => println(ex)
    }
    dataset.result().toSeq
  }

  def updateMicroRdd(microRddId: MicroRDDId, currentRddId: RDDId): Unit = {
		microRddDag(microRddId).counter += 1
		microRddDag(microRddId).childList.foreach { c =>
			microRddDag(c).counter += 1
		}
    rddIdByMicroRddId.put(microRddId, currentRddId)
  }

  def update(microRddDag: MicroRDDDAG, plan: Array[Array[MicroRDDId]], currentRddId: RDDId): Unit = {
    plan.foreach { mergedResult =>
      mergedResult.foreach { microRddId =>
        microRddDag(microRddId).counter += 1
        microRddDag(microRddId).childList.foreach { c =>
          microRddDag(c).counter += 1
        }
      }
    }
  }

  def writeDataset(microRddIdByInputName: mutable.Map[String, MicroRDDId]): Unit = {
    microRddIdByInputName.foreach { case (inputName, microRddId) =>
      println("write: " + inputName)
      //println("readDatasetToSeq(inputName): " + readDatasetToSeq(inputName).toList)
      //println("result rdd collect(): " + sc.parallelize(readDatasetToSeq(inputName)).partitionBy(QaaD.partitionerByInputName(inputName)).collect().toList)
      sc.parallelize(readDatasetToSeq(inputName))
        .partitionBy(QaaD.partitionerByInputName(inputName))
        .saveAsTextFile(inputName)
    }
  }

  def getDataset(inputName: String, microRddId: Int): RDD[((Int, String), Any)] = {
    if (!QaaD.datasets.contains(inputName)) {
      QaaD.datasets(inputName) = sc.parallelize(readDatasetToSeq2(inputName, microRddId))
        .partitionBy(QaaD.partitionerByInputName(inputName))
      //println(inputName + ": " + QaaD.datasets(inputName).take(5).toList)
    } 
    QaaD.datasets(inputName)
  }

  def createInitialRDD(microRddIdByInputName: mutable.Map[String, MicroRDDId]): Unit = {
    //writeDataset(microRddIdByInputName)
    rddArr += sc.union(microRddIdByInputName.map { case (inputName, microRddId) =>
        //getDataset(inputName, microRddId)
        readDataset(inputName)
          .map(x => (microRddId, x._2))
      }.toSeq)
      .persist(StorageLevel.MEMORY_AND_DISK_SER)
    microRddIdByInputName.values.foreach { microRddId =>
      updateMicroRdd(microRddId, 0)
    }
    rddArr.result()(0).collect()
  }

  def getType(operator: Operator): OperatorType = {
    if (operator == MAP || operator == FLATMAP || operator == FILTER) NARROW_DEPENDENCY
    else operator
  }

  def getReadyMicroRddIds(microRddDag: MicroRDDDAG): List[MicroRDDId] = {
    microRddDag.filter { case (microRddId, microRdd) =>
      microRdd.status == READY
    }.keys.toList
  }

  def getReadyMicroRddIdsByGroup(readyMicroRddIds: List[MicroRDDId]): Map[(RDDId, OperatorType), List[MicroRDDId]] = {
    readyMicroRddIds.groupBy { x =>
      val parent = QaaD.microRddDag(x).parentList(0)
      val rddId = QaaD.rddIdByMicroRddId(parent)
      val operatorType = getType(QaaD.microRddDag(x).operation.operator)
      (rddId, operatorType)
    }
  }

  def isDone(microRddDag: MicroRDDDAG): Boolean = {
    microRddDag.filter { case (microRddId, microRdd) =>
      microRdd.status == DONE
    }.size == microRddDag.size
  }

  def merge(readyMicroRddIds: List[MicroRDDId]): Array[Array[MicroRDDId]] = {
		// naive method
		// group ready MicroRDDs by 1) RDD id and 2) operator type
		val readyMicroRddIdsByGroup = getReadyMicroRddIdsByGroup(readyMicroRddIds)
		var groups = mutable.ArrayBuilder.make[Array[MicroRDDId]]
    //val t = 2
		readyMicroRddIdsByGroup.values.foreach { readyMicroRddIdList =>
			// consider sorting readyMicroRddIdList by input size
      var visited = mutable.ArrayBuffer.fill(readyMicroRddIdList.size)(false)
			for (i <- 0 until readyMicroRddIdList.size) {
				if (!visited(i)) {
					var group = mutable.ArrayBuilder.make[MicroRDDId]
					group += readyMicroRddIdList(i)
					visited(i) = true
					for (j <- i + 1 until readyMicroRddIdList.size) {
						if (!visited(j)/* && i % t == j % t*/) {
							group += readyMicroRddIdList(j)
							visited(j) = true
						}
					}
					groups += group.result()
				}
			}
		}
		groups.result()
  }

  // get a RDD to transform
  def getRdd(group: Array[MicroRDDId]): RDD[(MicroRDDId, Any)] = {
    assert(group.size > 0 && microRddDag(group(0)).parentList.size > 0)
    //QaaD.rddArr.result()(QaaD.rddIdByMicroRddId(QaaD.microRddDag(group(0)).parentList(0)))
    val parentMicroRddIds = group
      .flatMap(microRddId => QaaD.microRddDag(microRddId).parentList)
      .distinct
    val parentRddIds = parentMicroRddIds
      .map(microRddId => QaaD.rddIdByMicroRddId(microRddId))
      .distinct
    sc.union(parentRddIds.map(rddId => QaaD.rddArr.result()(rddId)).toSeq)
  }

  // transform a record (childMicroRddId, value) by applying the given function
  def transformValue(childMicroRddId: MicroRDDId, value: Any): Array[(MicroRDDId, Any)] = {
		def f = QaaD.microRddDag(childMicroRddId).operation.f
		QaaD.microRddDag(childMicroRddId).operation.operator match {
			case MAP => Array((childMicroRddId, f(value)))
			case FLATMAP => QaaD.microRddDag(childMicroRddId).operation.f3(value).asInstanceOf[Array[Any]].map(x => (childMicroRddId, x))
			case FILTER => {
				if (f(value).asInstanceOf[Boolean]) {
					Array((childMicroRddId, value).asInstanceOf[(MicroRDDId, Any)])
				} else {
					Array[(MicroRDDId, Any)]()
				}
      }
		}
  }

  def recursiveProceed(resultRecord: (MicroRDDId, Any)): Array[(MicroRDDId, Any)] = {
		if (QaaD.microRddDag(resultRecord._1).childList.size == 1) {
			val childMicroRddId = QaaD.microRddDag(resultRecord._1).childList(0)
			val operatorType = getType(QaaD.microRddDag(childMicroRddId).operation.operator)
			if (QaaD.microRddDag(childMicroRddId).parentList.size == 1 &&
					operatorType == NARROW_DEPENDENCY) {
				transformValue(childMicroRddId, resultRecord._2).flatMap(record => recursiveProceed(record))
			} else {
				Array[(MicroRDDId, Any)](resultRecord)
			}
		} else {
			Array[(MicroRDDId, Any)](resultRecord)
		}
  }

  // proceed remained transformations in the current stage
  def proceed(partition: Iterator[(MicroRDDId, Any)]): Iterator[(MicroRDDId, Any)] = {
		partition.flatMap { case record : (MicroRDDId, Any) =>
      var results = Array[(MicroRDDId, Any)](record)
			results.flatMap { resultRecord =>
				recursiveProceed(resultRecord)
			}
		}
  }

  // trasnform inputRDD corresponding to the MicroRDDs in mergedResult into outputRDD
  def transform(inputRDD: RDD[(MicroRDDId, Any)], mergedResult: Array[MicroRDDId]): RDD[(MicroRDDId, Any)] = {
		// get parentList 
		val parentMicroRddIds = mergedResult.flatMap(microRddId => QaaD.microRddDag(microRddId).parentList).distinct.map(x => (x, true)).toMap
		val operatorType = getType(QaaD.microRddDag(mergedResult(0)).operation.operator)
		operatorType match {
			case NARROW_DEPENDENCY => {
				inputRDD.filter(r => parentMicroRddIds.contains(r._1))
					.mapPartitions { partition: Iterator[(MicroRDDId, Any)] =>
						proceed(partition.flatMap { case record: (MicroRDDId, Any) =>
							val microRddId = record._1
							val value = record._2
							QaaD.microRddDag(microRddId).childList.flatMap { childMicroRddId: MicroRDDId =>
                transformValue(childMicroRddId, value)
							}
						})
					}
			}
			case JOIN => {
				val firstParentMicroRddIds = mergedResult.map(microRddId => QaaD.microRddDag(microRddId).parentList(0)).distinct
				val secondParentMicroRddIds = mergedResult.map(microRddId => QaaD.microRddDag(microRddId).parentList(1)).distinct
				val childListMap1 = firstParentMicroRddIds.map { pid =>
					(pid, QaaD.microRddDag(pid).childList.filter(c => QaaD.microRddDag(c).operation.operator == JOIN))
				}.toMap
				val childListMap2 = secondParentMicroRddIds.map { pid =>
					(pid, QaaD.microRddDag(pid).childList.filter(c => QaaD.microRddDag(c).operation.operator == JOIN))
				}.toMap
        val leftInputRdd = sc.union(firstParentMicroRddIds.map { pid =>
          QaaD.rddIdByMicroRddId(pid)
        }.distinct.map(rddId => rddArr.result()(rddId)).toSeq)
        val rightInputRdd = sc.union(secondParentMicroRddIds.map { pid =>
          QaaD.rddIdByMicroRddId(pid)
        }.distinct.map(rddId => rddArr.result()(rddId)).toSeq)
        val partitioner = new org.apache.spark.HashPartitioner(leftInputRdd.getNumPartitions)
				val leftRdd: RDD[((MicroRDDId, Any), Any)] = leftInputRdd
          .filter(r => childListMap1.contains(r._1))
					.flatMap(x => childListMap1(x._1)
          .map(c => ((c, x.asInstanceOf[(MicroRDDId, (Any, Any))]._2._1),
                         x.asInstanceOf[(MicroRDDId, (Any, Any))]._2._2)))
          .partitionBy(partitioner)
				val rightRdd: RDD[((MicroRDDId, Any), Any)] = rightInputRdd
          .filter(r => childListMap2.contains(r._1))
					.flatMap(x => childListMap2(x._1)
          .map(c => ((c, x.asInstanceOf[(MicroRDDId, (Any, Any))]._2._1),
                         x.asInstanceOf[(MicroRDDId, (Any, Any))]._2._2)))
          .partitionBy(partitioner)
				leftRdd.join(rightRdd, partitioner)
          .map(x => (x._1._1, (x._1._2, x._2)))
          .asInstanceOf[RDD[(MicroRDDId, Any)]]
          .mapPartitions(partition => proceed(partition))
			}
			case REDUCEBYKEY => {
				val childListMap = parentMicroRddIds.keys.map { pid =>
				  (pid, QaaD.microRddDag(pid).childList.filter(c => QaaD.microRddDag(c).operation.operator == REDUCEBYKEY))
			  }.toMap
				val funcMap = mergedResult.map { cid =>
					(cid, QaaD.microRddDag(cid).operation.f2)
				}.toMap
			  assert(mergedResult.size > 0)
        val operation = QaaD.microRddDag(mergedResult(0)).operation
        val numPartitions = operation.numPartitions
        if (numPartitions != -1) {
          inputRDD.filter(r => parentMicroRddIds.contains(r._1))
						.flatMap(x => childListMap(x._1)
                            .map(c => ((c, x.asInstanceOf[(MicroRDDId, (Any, Any))]._2._1),
                                       (c, x.asInstanceOf[(MicroRDDId, (Any, Any))]._2._2))))
            .asInstanceOf[RDD[((MicroRDDId, Any), Any)]]
						.reduceByKey((x, y) => QaaD.microRddDag(x.asInstanceOf[(Int, Any)]._1)
                                     .operation
                                     .f2(x.asInstanceOf[Any], y.asInstanceOf[Any]), numPartitions)
						.map(x => (x._1._1, (x._1._2, x._2.asInstanceOf[(MicroRDDId, Any)]._2)))
            .asInstanceOf[RDD[(MicroRDDId, Any)]]
            .mapPartitions(partition => proceed(partition))
        } else {
          val partitioner = new org.apache.spark.HashPartitioner(inputRDD.getNumPartitions)
          inputRDD.filter(r => parentMicroRddIds.contains(r._1))
						.flatMap(x => childListMap(x._1)
                            .map(c => ((c, x.asInstanceOf[(MicroRDDId, (Any, Any))]._2._1),
                                       (c, x.asInstanceOf[(MicroRDDId, (Any, Any))]._2._2))))
            .asInstanceOf[RDD[((MicroRDDId, Any), Any)]]
            .partitionBy(partitioner)
						.reduceByKey(partitioner, (x, y) => QaaD.microRddDag(x.asInstanceOf[(Int, Any)]._1)
                                     .operation.f2(x.asInstanceOf[Any], y.asInstanceOf[Any]))
						.map(x => (x._1._1, (x._1._2, x._2.asInstanceOf[(MicroRDDId, Any)]._2)))
            .asInstanceOf[RDD[(MicroRDDId, Any)]]
            .mapPartitions(partition => proceed(partition))
        }
			}
		}
  }

  // Update the status of decendants (MicroRDDs)
  def updateDecendants(microRddId: MicroRDDId): Unit = {
    var curMicroRddId = microRddId
    var stop = false
    while (!stop) {
      if (QaaD.microRddDag(curMicroRddId).childList.size == 1) {
        val childMicroRddId = QaaD.microRddDag(curMicroRddId).childList(0)
	val operatorType = getType(QaaD.microRddDag(childMicroRddId).operation.operator)
	if (QaaD.microRddDag(childMicroRddId).parentList.size == 1 &&
	  operatorType == NARROW_DEPENDENCY) {
          updateMicroRdd(childMicroRddId, QaaD.rddArr.result().size - 1)
          curMicroRddId = childMicroRddId
	} else {
	  stop = true
	}
      } else {
        stop = true
      }
    }
  }

  // Process the current MicroRDD DAG
  def run(): Unit = {
    createInitialRDD(QaaD.microRddIdByInputName)
    globalStartTime = System.currentTimeMillis
    do {
      val readyMicroRddIds = getReadyMicroRddIds(QaaD.microRddDag)
      //println(s"readyMicroRddIds: ${readyMicroRddIds}")
      val plan = merge(readyMicroRddIds)
      //println(s"plan: ${plan.map(_.toList).toList}")
      plan.foreach { subset: Array[MicroRDDId] =>
        val inputRDD: RDD[(MicroRDDId, Any)] = getRdd(subset)
        val outputRDD: RDD[(MicroRDDId, Any)] = transform(inputRDD, subset)
        rddArr += outputRDD.persist(StorageLevel.MEMORY_AND_DISK_SER)
        subset.foreach { microRddId =>
          updateMicroRdd(microRddId, QaaD.rddArr.result().size - 1)
          updateDecendants(microRddId)
        }
      }
    } while (!isDone(QaaD.microRddDag))
  }
}

