import QaaD._
import java.io._
import scala.io.Source

try {
  var count = 0
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
  
