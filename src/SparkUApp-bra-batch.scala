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
		val sLine = line.replace("(", "").replace(")", "").split(",")
		val tuple = arrayToTuple(sLine.slice(1, sLine.size))
		tuple
	}

}

def getDist(fromLat: Float, fromLng: Float, toLat: Float, toLng: Float): Float = {
	val AVERAGE_RADIUS_OF_EARTH_KM = 6371f
	val latDistance = Math.toRadians(fromLat - toLat)
	val lngDistance = Math.toRadians(fromLng - toLng)
	val sinLat = Math.sin(latDistance / 2)
	val sinLng = Math.sin(lngDistance / 2)
	val a = sinLat * sinLat +
	(Math.cos(Math.toRadians(fromLat)) *
			Math.cos(Math.toRadians(toLat)) *
			sinLng * sinLng)
	val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
	val dist = AVERAGE_RADIUS_OF_EARTH_KM * c
	1.0f * dist.toFloat
}

val rddOrders = readDataset(s"/root/QaaD/datasets/synthetic-brazilian-ecommerce/num-rows-${numRows}/orders.csv")
val rddOrderItems = readDataset(s"/root/QaaD/datasets/synthetic-brazilian-ecommerce/num-rows-${numRows}/order_items.csv")
val rddProducts = readDataset(s"/root/QaaD/datasets/synthetic-brazilian-ecommerce/num-rows-${numRows}/products.csv")
val rddCustomers = readDataset(s"/root/QaaD/datasets/synthetic-brazilian-ecommerce/num-rows-${numRows}/customers.csv")
val rddSellers = readDataset(s"/root/QaaD/datasets/synthetic-brazilian-ecommerce/num-rows-${numRows}/sellers.csv")
val rddGeolocation = readDataset(s"/root/QaaD/datasets/synthetic-brazilian-ecommerce/num-rows-${numRows}/geolocation.csv")
val rddOrderPayments = readDataset(s"/root/QaaD/datasets/synthetic-brazilian-ecommerce/num-rows-${numRows}/order_payments.csv")
val rddOrderReviews = readDataset(s"/root/QaaD/datasets/synthetic-brazilian-ecommerce/num-rows-${numRows}/order_reviews.csv")

rddOrders.collect()
rddOrderItems.collect()
rddProducts.collect()
rddCustomers.collect()
rddSellers.collect()
rddGeolocation.collect()
rddOrderPayments.collect()
rddOrderReviews.collect()

val lines = Source.fromFile(s"../querysets/brazilian-ecommerce/param/param-num-rows-${numRows}.csv").getLines().toList
var sTime = System.currentTimeMillis
var rddArr = scala.collection.mutable.ArrayBuilder.make[RDD[Any]]
var count = 1
try {
  for (i <- (startLine - 1) until (endLine - 1)) {
    val line = lines(i)
    count += 1
    if (count > 1 && count <= numQueries + 1) {
			val sLine = line.split(",")
			val sellerId = sLine(10)
			val startTime = sLine(3).toLong
			val endTime = startTime + 2592000L
      val distance = getDist(sLine(26).toFloat, sLine(27).toFloat, sLine(39).toFloat, sLine(40).toFloat)
      val volume = sLine(33).toFloat * sLine(34).toFloat * sLine(35).toFloat
      val customerCity = sLine(24)
      val customerState = sLine(25)
      val sellerCity = sLine(37)
      val sellerState = sLine(38)
      val reviewScore = sLine(19).toFloat
      val orderStatus = sLine(2)
      val deliveredTime = sLine(6).toLong
      val customerZipCodePrefix = sLine(23)
      val sellerZipCodePrefix = sLine(36)
      val reviewAnswerTime = sLine(21).toLong
      val weight = sLine(32).toFloat
			val paramMap = Map[String, Any]("sellerId" -> sellerId,
				"startTime" -> startTime, 
				"endTime" -> endTime,
        "distance" -> distance,
        "weight" -> weight,
        "customerCity" -> customerCity,
        "sellerCity" -> sellerCity,
        "customerState" -> customerState,
        "sellerState" -> sellerState,
        "reviewScore" -> reviewScore,
        "orderStatus" -> orderStatus,
        "deliveredTime" -> deliveredTime,
        "customerZipCodePrefix" -> customerZipCodePrefix,
        "sellerZipCodePrefix" -> sellerZipCodePrefix,
				"reviewAnswerTime" -> reviewAnswerTime, 
        "volume" -> volume,
        "numRows" -> numRows)
		  rddArr += (new DashboardApp1(paramMap)).seqRun(rddOrders, rddOrderItems, rddProducts, rddCustomers, rddSellers, rddGeolocation, rddOrderPayments, rddOrderReviews)
		  rddArr += (new DashboardApp2(paramMap)).seqRun(rddOrders, rddOrderItems, rddProducts, rddCustomers, rddSellers, rddGeolocation, rddOrderPayments, rddOrderReviews)
		  rddArr += (new DashboardApp3(paramMap)).seqRun(rddOrders, rddOrderItems, rddProducts, rddCustomers, rddSellers, rddGeolocation, rddOrderPayments, rddOrderReviews)
		  rddArr += (new DashboardApp4(paramMap)).seqRun(rddOrders, rddOrderItems, rddProducts, rddCustomers, rddSellers, rddGeolocation, rddOrderPayments, rddOrderReviews)
		  rddArr += (new DashboardApp5(paramMap)).seqRun(rddOrders, rddOrderItems, rddProducts, rddCustomers, rddSellers, rddGeolocation, rddOrderPayments, rddOrderReviews)
		  rddArr += (new DashboardApp6(paramMap)).seqRun(rddOrders, rddOrderItems, rddProducts, rddCustomers, rddSellers, rddGeolocation, rddOrderPayments, rddOrderReviews)
		  rddArr += (new DashboardApp7(paramMap)).seqRun(rddOrders, rddOrderItems, rddProducts, rddCustomers, rddSellers, rddGeolocation, rddOrderPayments, rddOrderReviews)
		  rddArr += (new DashboardApp8(paramMap)).seqRun(rddOrders, rddOrderItems, rddProducts, rddCustomers, rddSellers, rddGeolocation, rddOrderPayments, rddOrderReviews)
		  rddArr += (new DashboardApp9(paramMap)).seqRun(rddOrders, rddOrderItems, rddProducts, rddCustomers, rddSellers, rddGeolocation, rddOrderPayments, rddOrderReviews)
		  rddArr += (new DashboardApp10(paramMap)).seqRun(rddOrders, rddOrderItems, rddProducts, rddCustomers, rddSellers, rddGeolocation, rddOrderPayments, rddOrderReviews)
		  rddArr += (new DashboardApp11(paramMap)).seqRun(rddOrders, rddOrderItems, rddProducts, rddCustomers, rddSellers, rddGeolocation, rddOrderPayments, rddOrderReviews)
		  rddArr += (new DashboardApp12(paramMap)).seqRun(rddOrders, rddOrderItems, rddProducts, rddCustomers, rddSellers, rddGeolocation, rddOrderPayments, rddOrderReviews)
		  rddArr += (new DashboardApp13(paramMap)).seqRun(rddOrders, rddOrderItems, rddProducts, rddCustomers, rddSellers, rddGeolocation, rddOrderPayments, rddOrderReviews)
		  rddArr += (new DashboardApp14(paramMap)).seqRun(rddOrders, rddOrderItems, rddProducts, rddCustomers, rddSellers, rddGeolocation, rddOrderPayments, rddOrderReviews)
		  rddArr += (new DashboardApp15(paramMap)).seqRun(rddOrders, rddOrderItems, rddProducts, rddCustomers, rddSellers, rddGeolocation, rddOrderPayments, rddOrderReviews)
		  rddArr += (new DashboardApp16(paramMap)).seqRun(rddOrders, rddOrderItems, rddProducts, rddCustomers, rddSellers, rddGeolocation, rddOrderPayments, rddOrderReviews)
		  rddArr += (new DashboardApp17(paramMap)).seqRun(rddOrders, rddOrderItems, rddProducts, rddCustomers, rddSellers, rddGeolocation, rddOrderPayments, rddOrderReviews)
		  rddArr += (new DashboardApp18(paramMap)).seqRun(rddOrders, rddOrderItems, rddProducts, rddCustomers, rddSellers, rddGeolocation, rddOrderPayments, rddOrderReviews)
		  rddArr += (new DashboardApp19(paramMap)).seqRun(rddOrders, rddOrderItems, rddProducts, rddCustomers, rddSellers, rddGeolocation, rddOrderPayments, rddOrderReviews)
		  rddArr += (new DashboardApp20(paramMap)).seqRun(rddOrders, rddOrderItems, rddProducts, rddCustomers, rddSellers, rddGeolocation, rddOrderPayments, rddOrderReviews)
		  rddArr += (new DashboardApp21(paramMap)).seqRun(rddOrders, rddOrderItems, rddProducts, rddCustomers, rddSellers, rddGeolocation, rddOrderPayments, rddOrderReviews)
		  rddArr += (new DashboardApp22(paramMap)).seqRun(rddOrders, rddOrderItems, rddProducts, rddCustomers, rddSellers, rddGeolocation, rddOrderPayments, rddOrderReviews)
		  rddArr += (new DashboardApp23(paramMap)).seqRun(rddOrders, rddOrderItems, rddProducts, rddCustomers, rddSellers, rddGeolocation, rddOrderPayments, rddOrderReviews)
		  rddArr += (new DashboardApp24(paramMap)).seqRun(rddOrders, rddOrderItems, rddProducts, rddCustomers, rddSellers, rddGeolocation, rddOrderPayments, rddOrderReviews)
		  rddArr += (new DashboardApp25(paramMap)).seqRun(rddOrders, rddOrderItems, rddProducts, rddCustomers, rddSellers, rddGeolocation, rddOrderPayments, rddOrderReviews)
		  rddArr += (new DashboardApp26(paramMap)).seqRun(rddOrders, rddOrderItems, rddProducts, rddCustomers, rddSellers, rddGeolocation, rddOrderPayments, rddOrderReviews)
		  rddArr += (new DashboardApp27(paramMap)).seqRun(rddOrders, rddOrderItems, rddProducts, rddCustomers, rddSellers, rddGeolocation, rddOrderPayments, rddOrderReviews)
		  rddArr += (new DashboardApp28(paramMap)).seqRun(rddOrders, rddOrderItems, rddProducts, rddCustomers, rddSellers, rddGeolocation, rddOrderPayments, rddOrderReviews)
		  rddArr += (new DashboardApp29(paramMap)).seqRun(rddOrders, rddOrderItems, rddProducts, rddCustomers, rddSellers, rddGeolocation, rddOrderPayments, rddOrderReviews)
		  rddArr += (new DashboardApp30(paramMap)).seqRun(rddOrders, rddOrderItems, rddProducts, rddCustomers, rddSellers, rddGeolocation, rddOrderPayments, rddOrderReviews)
		  rddArr += (new DashboardApp31(paramMap)).seqRun(rddOrders, rddOrderItems, rddProducts, rddCustomers, rddSellers, rddGeolocation, rddOrderPayments, rddOrderReviews)
		  rddArr += (new DashboardApp32(paramMap)).seqRun(rddOrders, rddOrderItems, rddProducts, rddCustomers, rddSellers, rddGeolocation, rddOrderPayments, rddOrderReviews)
		  rddArr += (new DashboardApp33(paramMap)).seqRun(rddOrders, rddOrderItems, rddProducts, rddCustomers, rddSellers, rddGeolocation, rddOrderPayments, rddOrderReviews)
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
