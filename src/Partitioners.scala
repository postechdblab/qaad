import org.apache.spark.Partitioner

class IdPartitioner(_numPartitions: Int) extends Partitioner {
  override def numPartitions = _numPartitions

  override def getPartition(key: Any): Int = {
    (key.asInstanceOf[String].hashCode().toInt.abs) % numPartitions
  }

  override def equals(other: Any): Boolean = other match {
    case partitioner: IdPartitioner => partitioner.numPartitions == numPartitions
    case _ => false
  }
}
