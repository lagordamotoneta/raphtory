package com.raphtory.examples.bitcoin.analysis

import akka.actor.ActorContext
import com.raphtory.core.analysis.{Analyser, WorkerID}
import com.raphtory.examples.bitcoin.communications.CoinsAquiredPayload

import scala.collection.mutable.ArrayBuffer

class BitcoinAnalyser extends Analyser {

  //(implicit proxy: GraphRepoProxy.type, managerCount: Int,workerID:Int):
  override def analyse()(implicit workerID:WorkerID): Any = {
    var results = ArrayBuffer[(String, Double)]()
    var currentBlock = 0
    var hash = ""
    proxy.getVerticesSet().foreach(v => {
      val vertex = proxy.getVertex(v)
      val vertexType = vertex.getPropertyCurrentValue("type").getOrElse("no-type")
      if(vertexType.equals("address")) {
        val address = vertex.getPropertyCurrentValue("address").getOrElse("no address")
        var total: Double = 0
        for (edge <- vertex.getIngoingNeighbors) {
          val edgeValue = vertex.getIngoingNeighborProp(edge, "value").getOrElse("0")
          total += edgeValue.toDouble
        }
        results :+= (address, total)
      }
      else if(vertexType.equals("transaction")){
        val block = vertex.getPropertyCurrentValue("block").getOrElse("0")
        if(block.toInt>currentBlock){
          currentBlock=block.toInt
          hash= vertex.getPropertyCurrentValue("blockhash").getOrElse("0")
        }
      }
    })
    //println("Sending step end")

    CoinsAquiredPayload(workerID,results.sortBy(f => f._2)(Ordering[Double].reverse).take(10),currentBlock,hash)
  }

  override def setup()(implicit workerID:WorkerID): Any = {

  }
}



