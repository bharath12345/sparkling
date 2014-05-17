package in.bharathwrites.sparkling

import org.apache.spark.SparkContext

object Test extends App {
  val master: String = "spark://master:7077"
  val sparkHome: String = "/opt/spark-0.9.0"

  val sc: SparkContext = new SparkContext(master, "Test", sparkHome, SparkContext.jarOfObject(this))
  val textFile = sc.textFile("hdfs://master:9000/bharath/ex1data1.txt")
  println("file count = " + textFile.count())
  sc.stop()
}
