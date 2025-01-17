package com.raphtory.examples.TestPackage

import com.raphtory.core.components.Router.TraditionalRouter.Helpers.RouterSlave
import com.raphtory.core.model.communication._
import java.text.SimpleDateFormat


class TestRouter (routerId:Int,override val initialManagerCount:Int) extends RouterSlave {


  def parseRecord(record: Any): Unit = {

    val fileLine = record.asInstanceOf[String].split(",").map(_.trim)
    //extract the values from the data source in the form of:
    // 0-sourceNode,1-targetNode,2-sourceCitedTargetOn,3-targetCreationDate,4-targetLastCitedOn
    val sourceNode=fileLine(0).toInt
    val targetNode=fileLine(1).toInt
    val sourceCitedTargetOn = dateToUnixTime(timestamp=fileLine(2))
    val targetCreationDate = dateToUnixTime(timestamp=fileLine(3))
    val targetLastCitedOn =  dateToUnixTime(timestamp=fileLine(4))

    //create sourceNode
    toPartitionManager(VertexAdd(routerId, sourceCitedTargetOn, sourceNode))
    //create destinationNode
    toPartitionManager(VertexAdd(routerId, targetCreationDate, targetNode))
    //create edge
    toPartitionManager(EdgeAdd(routerId, sourceCitedTargetOn, sourceNode, targetNode))

    if (sourceCitedTargetOn == targetLastCitedOn) {
      toPartitionManager(EdgeRemoval(routerId, targetLastCitedOn, sourceNode, targetNode))
    }


  }

  def dateToUnixTime(timestamp: => String): Long = {
    //if(timestamp == null) return null;
    println(timestamp)
    val sdf = new SimpleDateFormat("dd/MM/yyyy")
    println(sdf)
    val dt = sdf.parse(timestamp)
    println(dt)
    val epoch = dt.getTime()
    println(epoch)
    epoch / 1000

  }
}
