import QaaD._
import java.io._
import scala.io.Source
import org.apache.spark.rdd.RDD
import scala.collection.mutable
import org.apache.spark.Partitioner

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

var sTime = System.currentTimeMillis
var count = 1
try {
  println("Results for the given query set:")
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
			var ts = System.currentTimeMillis()
			println((new DashboardApp1(paramMap)).seqRun(rdd).map(x => (count - 1, x)).collect().toList)
			println((new DashboardApp2(paramMap)).seqRun(rdd).map(x => (count - 1, x)).collect().toList)
			println((new DashboardApp3(paramMap)).seqRun(rdd).map(x => (count - 1, x)).collect().toList)
			println((new DashboardApp4(paramMap)).seqRun(rdd).map(x => (count - 1, x)).collect().toList)
			println((new DashboardApp5(paramMap)).seqRun(rdd).map(x => (count - 1, x)).collect().toList)
			println((new DashboardApp6(paramMap)).seqRun(rdd).map(x => (count - 1, x)).collect().toList)
			println((new DashboardApp9(paramMap)).seqRun(rdd).map(x => (count - 1, x)).collect().toList)
			println((new DashboardApp10(paramMap)).seqRun(rdd).map(x => (count - 1, x)).collect().toList)
			println((new DashboardApp11(paramMap)).seqRun(rdd).map(x => (count - 1, x)).collect().toList)
			println((new DashboardApp12(paramMap)).seqRun(rdd).map(x => (count - 1, x)).collect().toList)
			println((new DashboardApp13(paramMap)).seqRun(rdd).map(x => (count - 1, x)).collect().toList)
			println((new DashboardApp14(paramMap)).seqRun(rdd).map(x => (count - 1, x)).collect().toList)
			println((new DashboardApp15(paramMap)).seqRun(rdd).map(x => (count - 1, x)).collect().toList)
			println((new DashboardApp16(paramMap)).seqRun(rdd).map(x => (count - 1, x)).collect().toList)
			println((new DashboardApp17(paramMap)).seqRun(rdd).map(x => (count - 1, x)).collect().toList)
			println((new DashboardApp18(paramMap)).seqRun(rdd).map(x => (count - 1, x)).collect().toList)
			println((new DashboardApp19(paramMap)).seqRun(rdd).map(x => (count - 1, x)).collect().toList)
			println((new DashboardApp20(paramMap)).seqRun(rdd).map(x => (count - 1, x)).collect().toList)
			println((new DashboardApp24(paramMap)).seqRun(rdd).map(x => (count - 1, x)).collect().toList)
			println((new DashboardApp26(paramMap)).seqRun(rdd).map(x => (count - 1, x)).collect().toList)
			println((new DashboardApp27(paramMap)).seqRun(rdd).map(x => (count - 1, x)).collect().toList)
			println((new DashboardApp28(paramMap)).seqRun(rdd).map(x => (count - 1, x)).collect().toList)
			println((new DashboardApp29(paramMap)).seqRun(rdd).map(x => (count - 1, x)).collect().toList)
			println((new DashboardApp30(paramMap)).seqRun(rdd).map(x => (count - 1, x)).collect().toList)
			println((new DashboardApp31(paramMap)).seqRun(rdd).map(x => (count - 1, x)).collect().toList)
			println((new DashboardApp32(paramMap)).seqRun(rdd).map(x => (count - 1, x)).collect().toList)
			println((new DashboardApp33(paramMap)).seqRun(rdd).map(x => (count - 1, x)).collect().toList)
			if (count == numQueries + 1) {
        println("Elapsed time (sec.): " + ((System.currentTimeMillis - sTime) / 1000.0f))
			  System.exit(0)
			}
    }
  }
} catch {
  case ex: Exception => println(ex)
}
System.exit(0)
