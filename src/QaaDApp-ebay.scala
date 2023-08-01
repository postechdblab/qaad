import QaaD._
import java.io._
import scala.io.Source

def readDataset(inputName: String): RDD[Any] = {

	def arrayToTuple[A <: Object](array:Array[A]): Any = {
		val c = Class.forName("scala.Tuple" + array.size)
		c.getConstructors.apply(0).newInstance(array:_*).asInstanceOf[Any]
	}

	sc.textFile(inputName).map { line =>
		val sLine = line.replace("\"", "").replace("(", "").replace(")", "").split(",", -1)
		val tuple = arrayToTuple(sLine.slice(1, sLine.size))
		tuple
	}

}

val rdd = readDataset(s"/root/QaaD/datasets/synthetic-ebay/num-rows-${numRows}/ebay.csv")
rdd.collect()


try {
  var count = 1
  for (line <- Source.fromFile(s"/root/QaaD/datasets/synthetic-ebay/num-rows-0/ebay.csv").getLines()) {
    count += 1
    if (count > 1 && count <= numQueries + 1) {
			val sLine = line.replace("\"", "").split(",", -1)
			val sellerId = sLine(8)
			val startTime = sLine(2).toFloat
			val endTime = startTime + 30.0f
      val reviewScore = sLine(4).toFloat
			val paramMap = Map[String, Any]("sellerId" -> sellerId,
				"startTime" -> startTime, 
				"endTime" -> endTime,
        "reviewScore" -> reviewScore,
        "numRows" -> numRows)
			new DashboardApp1(paramMap).run()
			new DashboardApp2(paramMap).run()
			new DashboardApp3(paramMap).run()
			new DashboardApp4(paramMap).run()
			new DashboardApp5(paramMap).run()
			new DashboardApp6(paramMap).run()
			new DashboardApp9(paramMap).run()
			new DashboardApp10(paramMap).run()
			new DashboardApp11(paramMap).run()
			new DashboardApp12(paramMap).run()
			new DashboardApp13(paramMap).run()
			new DashboardApp14(paramMap).run()
			new DashboardApp15(paramMap).run()
			new DashboardApp16(paramMap).run()
			new DashboardApp17(paramMap).run()
			new DashboardApp18(paramMap).run()
			new DashboardApp19(paramMap).run()
			new DashboardApp20(paramMap).run()
			new DashboardApp24(paramMap).run()
			new DashboardApp26(paramMap).run()
			new DashboardApp27(paramMap).run()
			new DashboardApp28(paramMap).run()
			new DashboardApp29(paramMap).run()
			new DashboardApp30(paramMap).run()
			new DashboardApp31(paramMap).run()
			new DashboardApp32(paramMap).run()
			new DashboardApp33(paramMap).run()
			if (count == numQueries + 1) exec()
    }
  }
} catch {
  case ex: Exception => println(ex)
}

def exec(): Unit = { 
  val startTime = System.currentTimeMillis
  scheduler.run()
  val resultMicroRddIdList = QaaD.microRddDag.keys.filter { microRddId =>
    QaaD.microRddDag(microRddId).childList.size == 0
  }.toList
  val resultRddIdSet = resultMicroRddIdList.map(microRddId => QaaD.rddIdByMicroRddId(microRddId)).filter(rddId => rddId >= 0).toSet
  val result = sc.union(resultRddIdSet.map(rddId =>
    QaaD.rddArr.result()(rddId).filter { case (microRddId, record) =>
      resultMicroRddIdList.contains(microRddId)
    }).toSeq).collect().toList
  println("Result for the given query set: " + result)
  println("Elapsed time (sec.): " + ((System.currentTimeMillis - startTime) / 1000.0f))
  System.exit(0)
}
  
System.exit(0)
