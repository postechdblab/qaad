class Operation(val operator: Int, val f: Any => Any = null, val f2: (Any, Any) => Any = null, val f3: Any => Array[Any], val numPartitions: Int = -1) extends Serializable {
}
