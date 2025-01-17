package com.raphtory.examples.gab.analysis

import akka.actor.ActorContext
import com.raphtory.core.analysis.{Analyser, WorkerID}
import monix.execution.atomic.AtomicDouble

import scala.collection.mutable.ArrayBuffer

class GabMostUsedTopics(networkSize : Int, dumplingFactor : Float) extends Analyser {

  override def setup()(implicit workerID:WorkerID) = {}

  override def analyse()(implicit workerID: WorkerID) : ArrayBuffer[(String, Int, String)] = {
    //println("Analyzing")
    var results = ArrayBuffer[(String, Int, String)]()
    proxy.getVerticesSet().foreach(v => {
      val vertex = proxy.getVertex(v)
      if(vertex.getPropertyCurrentValue("type").getOrElse("no type").equals("topic")){
        val ingoingNeighbors  = vertex.getIngoingNeighbors.size
        results.synchronized {
          vertex.getPropertyCurrentValue("id") match {
            case None =>
            case Some(id) =>
              results +:= (id.toString, ingoingNeighbors, vertex.getPropertyCurrentValue("title").getOrElse("no title").toString)
          }
          if (results.size > 10) {
            results = results.sortBy(_._2)(Ordering[Int].reverse).take(10)
          }
        }
    }})
    //println("Sending step end")
    results
  }
}
