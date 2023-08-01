import QaaD._
import java.io._
import scala.io.Source
import org.apache.spark.rdd.RDD
import scala.collection.mutable
import org.apache.spark.Partitioner
import org.apache.spark.storage.StorageLevel

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

val lines = Source.fromFile(s"/root/QaaD/datasets/synthetic-ebay/num-rows-0/ebay.csv").getLines().toList
var sTime = System.currentTimeMillis
var rddArr = scala.collection.mutable.ArrayBuilder.make[RDD[Any]]
var count = 1
try {
  for (i <- startLine until endLine) {
    val line = lines(i)
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
		  rddArr += (new DashboardApp1(paramMap)).seqRun(rdd)
		  rddArr += (new DashboardApp2(paramMap)).seqRun(rdd)
		  rddArr += (new DashboardApp3(paramMap)).seqRun(rdd)
		  rddArr += (new DashboardApp4(paramMap)).seqRun(rdd)
		  rddArr += (new DashboardApp5(paramMap)).seqRun(rdd)
		  rddArr += (new DashboardApp6(paramMap)).seqRun(rdd)
		  rddArr += (new DashboardApp9(paramMap)).seqRun(rdd)
		  rddArr += (new DashboardApp10(paramMap)).seqRun(rdd)
		  rddArr += (new DashboardApp11(paramMap)).seqRun(rdd)
		  rddArr += (new DashboardApp12(paramMap)).seqRun(rdd)
		  rddArr += (new DashboardApp13(paramMap)).seqRun(rdd)
		  rddArr += (new DashboardApp14(paramMap)).seqRun(rdd)
		  rddArr += (new DashboardApp15(paramMap)).seqRun(rdd)
		  rddArr += (new DashboardApp16(paramMap)).seqRun(rdd)
		  rddArr += (new DashboardApp17(paramMap)).seqRun(rdd)
		  rddArr += (new DashboardApp18(paramMap)).seqRun(rdd)
		  rddArr += (new DashboardApp19(paramMap)).seqRun(rdd)
		  rddArr += (new DashboardApp20(paramMap)).seqRun(rdd)
		  rddArr += (new DashboardApp24(paramMap)).seqRun(rdd)
		  rddArr += (new DashboardApp26(paramMap)).seqRun(rdd)
		  rddArr += (new DashboardApp27(paramMap)).seqRun(rdd)
		  rddArr += (new DashboardApp28(paramMap)).seqRun(rdd)
		  rddArr += (new DashboardApp29(paramMap)).seqRun(rdd)
		  rddArr += (new DashboardApp30(paramMap)).seqRun(rdd)
		  rddArr += (new DashboardApp31(paramMap)).seqRun(rdd)
		  rddArr += (new DashboardApp32(paramMap)).seqRun(rdd)
		  rddArr += (new DashboardApp33(paramMap)).seqRun(rdd)
			if (count == numQueries + 1) {
				val result = sc.union(rddArr.result().toSeq).collect().toList
        println("Results for the given query set: " + result)
        println("Elapsed time (sec.): " + ((System.currentTimeMillis - sTime) / 1000.0f))
			  System.exit(0)
			}
    }
  }
} catch {
  case ex: Exception => println(ex)
}
System.exit(0)
