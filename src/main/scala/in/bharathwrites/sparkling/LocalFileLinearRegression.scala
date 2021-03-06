package in.bharathwrites.sparkling

import org.apache.spark.util.Vector
import org.apache.spark.SparkContext
import org.apache.spark.mllib.regression.{LinearRegressionWithSGD, LabeledPoint, LinearRegressionModel}
import org.apache.spark.rdd.RDD
import java.lang.Math
import org.apache.spark.mllib.util.MLUtils

object LocalFileLinearRegression {
  val ITERATIONS = 1500
  val ALPHA = 0.01

  //parses a csv file where each line has a set of features at the beginning and the label at the end
  def parseFileContent(inputData: RDD[String]): RDD[LabeledPoint] = {
    val labelledRDD = inputData.map(line => {
      val parts = line.split(",")
      LabeledPoint(parts.last.toDouble, parts.init.map(x => x.toDouble).toArray)
    })
    labelledRDD
  }

  //Predicts the possibility of the data set provided as input and handles feature normalisations also.
  def doPrediction(model:LinearRegressionModel, features:Array[Double],labelMeanStd:(Double,Double)=null,featureMean:Array[Double]=null, featureStdDev : Array[Double]=null):Double={
    if (labelMeanStd !=null || featureMean != null || featureStdDev != null) {
      val normalizedFeatures = new Array[Double](features.length)
      for (a <- 0 to features.length - 1) {
        normalizedFeatures(a) = (features(a) - featureMean(a)) / featureStdDev(a)
      }
      val finalPredictedValue = model.predict(normalizedFeatures) * labelMeanStd._2 + labelMeanStd._1
      finalPredictedValue
    } else {
      val finalPredictedValue = model.predict(features)
      finalPredictedValue
    }

  }

  //Calculates the rate of error the model is predicting
  def findErrorRate(labelledRDD: RDD[LabeledPoint], model: LinearRegressionModel): Double = {
    val total = labelledRDD.map (
      labeledPoint =>{
        val prediction = model.predict(labeledPoint.features)
        Math.pow(labeledPoint.label - prediction, 2.0)
    }).reduce(_+_)
    val trainError = total / (2 * labelledRDD.count())
    trainError
  }


  //Runs ex1_linear_regression Regression on the RDD with the optimizations passed as parameter and returns a LR model which can be used for predictions
  def runLinearRegression(labelledRDD: RDD[LabeledPoint], numberOfIterations: Int, initialWeights:Array[Double], stepSize:Double,miniBatchFraction:Double): LinearRegressionModel = {
    val regression = new LinearRegressionWithSGD()
    regression.optimizer.setNumIterations(numberOfIterations).setStepSize(stepSize).setMiniBatchFraction(miniBatchFraction)
    println("starting the linear regression run...")
    val linearRegressionModel = regression.run(labelledRDD, initialWeights)
    println("finished the linear regression run")
    linearRegressionModel
  }

  //Normalises any number of features in a RDD of LabeledPoints and returns the normalised RDD, mean and std dev of label and features.
  def normaliseFeatures(labelledRDD: RDD[LabeledPoint]): (RDD[LabeledPoint], Array[Double], Array[Double], Double, Double) = {
    val context = labelledRDD.context
    val numOfFeatures = labelledRDD.first().features.length
    val nexamples = labelledRDD.count()
    val results = MLUtils.computeStats(labelledRDD,numOfFeatures,nexamples)
    val labelMean = results._1
    val featureMean = results._2.toArray
    val featureStdDev = results._3.toArray

    var broadcastLabelMean = context.broadcast(labelMean)

    val intermediate = labelledRDD.map(eachLabelledPoint=>{
      Math.pow(eachLabelledPoint.label-broadcastLabelMean.value,2.0)
    }).reduce((a,b)=>{a+b})

    val labelStdDev = Math.sqrt(intermediate/(nexamples-1))
    val broadcastMeanAndStdDev = context.broadcast(labelMean,labelStdDev,featureMean,featureStdDev)

    val normalizedRDD = labelledRDD.map(point => {
      val normalizedFeatureArray = new Array[Double](numOfFeatures)
      val features = point.features
      for (a <- 0 to numOfFeatures - 1) {
        if (broadcastMeanAndStdDev.value._3(a) == 0 && broadcastMeanAndStdDev.value._4(a) == 0) {
          normalizedFeatureArray(a) = 0.0
        } else {
          normalizedFeatureArray(a) = (features(a) - broadcastMeanAndStdDev.value._3(a)) / broadcastMeanAndStdDev.value._4(a)
        }

      }
      LabeledPoint((point.label - broadcastMeanAndStdDev.value._1)/broadcastMeanAndStdDev.value._2, normalizedFeatureArray)
    })
    (normalizedRDD,featureMean,featureStdDev,labelMean,labelStdDev)
  }


  def main(args: Array[String]) {
    // val context = new SparkContext("local", "ml-exercise")
    val master: String = "spark://master:7077"
    val sparkHome: String = "/opt/spark-0.9.0"

    val oldJarWay = SparkContext.jarOfObject(this)
    val newJarWay = Seq(System.getenv("JARS"))

    val context: SparkContext = new SparkContext(master, "Test", sparkHome, newJarWay)

    val fileContents = context.textFile("hdfs://master:9000/bharath/ex1data1.txt").cache()
    println("Reading File")
    var labelledRDD = parseFileContent(fileContents).cache()
    println("Running Regression")
    val featureScaledData = normaliseFeatures(labelledRDD)
    println("normalizing features done")
    labelledRDD = featureScaledData._1.cache()
    val featureMean = featureScaledData._2
    val featureStd = featureScaledData._3
    val labelMean = featureScaledData._4
    val labelStd = featureScaledData._5
    val model = runLinearRegression(labelledRDD, 30, Array(0.80),0.6,1.0)
    println("Finding Error Rate")
    val errorRate = findErrorRate(labelledRDD, model)
    println("Error Rate is:" + errorRate)
    println("Theta values are:")
    println(model.intercept)
    model.weights.foreach(println)
    print("For population = 35,000, we predict a profit of: ")
    println(doPrediction(model,Array(3.5),(labelMean,labelStd),featureMean,featureStd)*10000)
    println("Octave predicted value is: 2912.764904")
    print("For population = 75,000, we predict a profit of: ")
    println(doPrediction(model,Array(7.0),(labelMean,labelStd),featureMean,featureStd)*10000)
    println("Octave predicted value is: 44606.906716")
  }
}
