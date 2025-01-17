package com.raphtory.examples.gab.actors

import com.raphtory.core.components.AnalysisManager.LiveAnalysisManager
import com.raphtory.core.analysis.Analyser
import com.raphtory.examples.gab.analysis.GabMostUsedTopics

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class GabLiveAnalyserManagerMostUsedTopics(jobID:String) extends LiveAnalysisManager(jobID) {
  /*private val B       : Int   = 100 // TODO set
  private val epsilon : Float = 0.85F
  private val delta1  : Float = 1F*/

  private var epsilon        = 1
  private val dumplingFactor = 0.85F
  private var firstStep      = true

  override protected def processResults(): Unit = {
    println()
    println("Current top topics")
    results.asInstanceOf[ArrayBuffer[ArrayBuffer[(String, Int, String)]]].flatten.sortBy(f => f._2)(Ordering[Int].reverse).foreach(
      topic => println(s"Topic: ${topic._3} with ID ${topic._1} and total uses of ${topic._2}")
    )
    }
    println()
   //   .flatMap(e => e).sortBy(f => f._2)(Ordering[Double])
   //   .reverse
  //)*/

  override protected def defineMaxSteps(): Int = {
    //steps =  (B * Math.log(getNetworkSize/epsilon)).round
    steps = 1 //Int.MaxValue
    1
  }

  override protected def generateAnalyzer : Analyser = new GabMostUsedTopics(0, dumplingFactor)
  override protected def processOtherMessages(value: Any) : Unit = {println ("Not handled message" + value.toString)}

  override protected def checkProcessEnd() : Boolean = {
    try {
      val _newResults = results.asInstanceOf[ArrayBuffer[ArrayBuffer[(Long, Double)]]].flatten
      val _oldResults = oldResults.asInstanceOf[ArrayBuffer[ArrayBuffer[(Long, Double)]]].flatten
      println(s"newResults: ${_newResults.size} => ${_oldResults.size}")

      if (firstStep) {
        firstStep = false
        return false
      }

      val newSum = _newResults.sum(resultNumeric)._2
      val oldSum = _oldResults.sum(resultNumeric)._2

      println(s"newSum = $newSum - oldSum = $oldSum - diff = ${newSum - oldSum}")
      //results = _newResults
      Math.abs(newSum - oldSum) / _newResults.size < epsilon
    } catch {
      case _ : Exception => false
    }
  }

  implicit object resultNumeric extends Numeric[(Long, Double)] {
    override def plus(x: (Long, Double), y: (Long, Double)) = (x._1 + y._1, x._2 + y._2)
    override def minus(x: (Long, Double), y: (Long, Double)) = (x._1 - y._1, x._2 - y._2)
    override def times(x: (Long, Double), y: (Long, Double)) = (x._1 * y._1, x._2 * y._2)
    override def negate(x: (Long, Double)) = (-x._1, -x._2)
    override def fromInt(x: Int) = (x, x)
    override def toInt(x: (Long, Double)) = x._1.toInt
    override def toLong(x: (Long, Double)) = x._1
    override def toFloat(x: (Long, Double)) = x._2.toFloat
    override def toDouble(x: (Long, Double)) = x._2
    override def compare(x: (Long, Double), y: (Long, Double)) = x._2.compare(y._2)
  }
}