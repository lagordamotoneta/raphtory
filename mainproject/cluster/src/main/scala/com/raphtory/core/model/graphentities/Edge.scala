package com.raphtory.core.model.graphentities

import com.raphtory.core.storage.{EdgeHistoryPoint, EdgePropertyPoint, EntityStorage}
import com.raphtory.core.utils.{EntityRemovedAtTimeException, PushedOutOfGraphException, StillWithinLiveGraphException, Utils}

import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.collection.parallel.mutable.ParTrieMap
/**
  * Companion Edge object (extended creator for storage loads)
  */
object Edge {
  def apply(routerID:Int,workerID:Int,creationTime : Long, edgeId : Long,
            previousState : mutable.TreeMap[Long, Boolean],
            properties : ParTrieMap[String, Property]) = {

    val srcId = Utils.getIndexHI(edgeId)
    val dstId = Utils.getIndexLO(edgeId)

    val e = new Edge(routerID,workerID,creationTime, srcId, dstId, initialValue = true)
    e.previousState   = previousState
    e.properties      = properties
    e
  }

  def apply(saved:EdgeHistoryPoint, time:Long) = {
    val src = saved.src
    val dst = saved.dst
    val history = saved.history
    var closestTime:Long = 0
    var value = false
    for((k,v) <- history){
      if(k<=time)
        if((time-k)<(time-closestTime)) {
          closestTime = k
          value = v
        }
    }
    if(!value){
      throw EntityRemovedAtTimeException(Utils.getEdgeIndex(src,dst))
    }
    new Edge(-1,-1,closestTime,src,dst,value)
  }
}

/**
  * Created by Mirate on 01/03/2017.
  */
class Edge(routerID:Int, workerID:Int, msgTime: Long, srcId: Int, dstId: Int, initialValue: Boolean) extends Entity(routerID,msgTime, initialValue) {


  def killList(vKills: mutable.TreeMap[Long, Boolean]): Unit = {
    try{
      removeList ++= vKills
      previousState ++= vKills
    }
    catch {
      case e:java.lang.NullPointerException => {
        e.printStackTrace()
        killList(vKills)
      }
    }
  }

  override def getId: Long = Utils.getEdgeIndex(srcId, dstId)
  def getSrcId : Int = srcId
  def getDstId : Int = dstId
  def getWorkerID:Int = workerID

  def aliveAt(time:Long):Boolean = {
    var closestTime:Long = 0
    var value = false
    for((k,v) <- previousState){
      if(k<=time)
        if((time-k)<(time-closestTime)) {
          closestTime = k
          value = v
        }
    }

    value
  }

  def aliveAtWithWindow(time:Long,windowSize:Long):Boolean = {
    var closestTime:Long = 0
    var value = false
    for((k,v) <- previousState){
      if(k<=time)
        if((time-k)<(time-closestTime)) {
          closestTime = k
          value = v
        }
    }
    if(time-closestTime<=windowSize)
      value
    else
      false
  }



  def viewAt(time:Long):Edge = {

    if(time > EntityStorage.lastCompressedAt){
      throw StillWithinLiveGraphException(time)
    }
    //if(time < EntityStorage.oldestTime){
    //  throw PushedOutOfGraphException(time)
    //}
    var closestTime:Long = 0
    var value = false
    for((k,v) <- previousState){
      if(k<=time)
        if((time-k)<(time-closestTime)) {
          closestTime = k
          value = v
        }
    }
    if(value==false)
      throw EntityRemovedAtTimeException(getId)
    val edge = new Edge(-1,workerID = -1,closestTime,srcId,dstId,value)
    for((k,p) <- properties) {
      val value = p.valueAt(time)
      if (!(value equals("default")))
        edge  + (time,k,value)
    }
    edge
  }



  override def equals(obj: scala.Any): Boolean = {
    if(obj.isInstanceOf[Edge]){
      val v2 = obj.asInstanceOf[Edge] //add associated edges
      if((getSrcId == v2.getSrcId) && (getDstId == v2.getDstId) && (previousState.equals(v2.previousState)) && (oldestPoint.get == v2.oldestPoint.get) && (newestPoint.get == newestPoint.get) && (properties.equals(v2.properties))){
        return true
      }
    }
    false
  }

  def addSavedProperty(property:EdgePropertyPoint, time:Long): Unit ={
    val history = property.history
    var closestTime:Long = 0
    var value = "default"
    for((k,v) <- history){
      if(k<=time)
        if((time-k)<(time-closestTime)) {
          closestTime = k
          value = v
        }
    }
    if(!(value equals ("default")))
      this + (time,property.name,value)
  }

  override def toString: String = {
    s"Edge srcID $srcId dstID $dstId \n History $previousState \n Properties:\n $properties"
  }

}
