import QaaD._
import java.io._
import scala.io.Source

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

try {
  var count = 0
  for (line <- Source.fromFile(s"/root/QaaD/querysets/brazilian-ecommerce/param/param-num-rows-${numRows}.csv").getLines()) {
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
      new SetupPartitions(paramMap).run()
      if (count == numQueries + 1) exec()
    }
  }
} catch {
  case ex: Exception => println(ex)
}

def exec(): Unit = { 
  scheduler.run()
  System.exit(0)
}
System.exit(0)
  

