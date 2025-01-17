

package com.raphtory.examples.gabMining.analysis

import com.raphtory.core.analysis.{Analyser, WorkerID}
import com.raphtory.core.model.communication.VertexMessage

import scala.collection.mutable
import scala.collection.parallel.mutable.ParTrieMap

class GabMiningCCAnalyser extends Analyser {

  case class ClusterLabel(value: Int) extends VertexMessage

  //Definition of a Pregel-like superstep=0 . In Raphtory is called setup. All the vertices are initiallized to its own id.

  override def setup()(implicit workerID: WorkerID) = {
    proxy.getVerticesSet().foreach(v => {
      val vertex = proxy.getVertex(v)
      var min = v
      //math.min(v, vertex.getOutgoingNeighbors.union(vertex.getIngoingNeighbors).min)
      val toSend = vertex.getOrSetCompValue("cclabel", min).asInstanceOf[Int]
      vertex.messageAllNeighbours(ClusterLabel(toSend))
    })
  }

  //The vertices are parsed to get the id they have and if minimum to the one they have, they will assign the minimum to themselves
  // and message this value to their neighbours
  //In this function we can observe that in order to maintain the state of the value of the vertex, the getOrSetCompValue is used.
  // the result is passed to the Live Analyser as in a key value pair structure
  override def analyse()(implicit workerID: WorkerID): Any= {
    var results = ParTrieMap[Int, Int]()
    var verts = Set[Int]()
    //println(s"Here !!! $workerID ${proxy.getVerticesSet().size}")
    for(v <- proxy.getVerticesSet()){
      val vertex = proxy.getVertex(v)
      val queue = vertex.messageQueue.map(_.asInstanceOf[ClusterLabel].value)
      var label = v
      if(queue.nonEmpty)
        label = queue.min
      vertex.messageQueue.clear
      var currentLabel = vertex.getOrSetCompValue("cclabel",v).asInstanceOf[Int]
      if (label < currentLabel) {
        vertex.setCompValue("cclabel", label)
        vertex messageAllNeighbours  (ClusterLabel(label))
        currentLabel = label
      }
      else{
        vertex messageAllNeighbours (ClusterLabel(currentLabel))
        //vertex.voteToHalt()
      }
      results.put(currentLabel, 1+results.getOrElse(currentLabel,0))
      verts+=v
    }
    //if(verts.size!=proxy.getVerticesSet().size)
    //  println(println(s"$workerID ${proxy.getVerticesSet().size} ${verts.size} $verts"))
    results
  }


}
