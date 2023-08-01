import scala.collection.mutable
import org.apache.spark.rdd.RDD
import org.apache.spark.Partitioner
import org.apache.spark.sql._
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

  def readDatasetToSeq(inputName: String): Seq[(String, Any)] = {
    def arrayToTuple[A <: Object](array:Array[A]): Any = {
      val c = Class.forName("scala.Tuple" + array.size)
      c.getConstructors.apply(0).newInstance(array:_*).asInstanceOf[Any]
    }
    var dataset = mutable.ArrayBuilder.make[(String, Any)]
    try {
      val keyPos = QaaD.keyPosByInputName(inputName)
      for (line <- Source.fromFile(inputName).getLines()) {
        val sLine = line.split(",", -1)
        val tuple = arrayToTuple(sLine)
        val row = (sLine(keyPos), tuple)
        if (!sLine.contains("")) {
          dataset += row
        }
      }
    } catch {
      case ex: Exception => println(ex)
    }
    dataset.result().toSeq
  }

  def writeDataset(microRddIdByInputName: mutable.Map[String, MicroRDDId]): Unit = {
    microRddIdByInputName.foreach { case (inputName, microRddId) =>
      sc.parallelize(readDatasetToSeq(inputName))
        .partitionBy(QaaD.partitionerByInputName(inputName))
        .saveAsTextFile(inputName)
    }
  }

  def run(): Unit = {
    writeDataset(QaaD.microRddIdByInputName)
  }
}

