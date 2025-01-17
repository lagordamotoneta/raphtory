package com.raphtory.examples.random.actors

import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import com.raphtory.core.components.Router.TraditionalRouter.Helpers.RouterSlave
import com.raphtory.core.components.Router.TraditionalRouter.RaphtoryRouter
import com.raphtory.core.model.communication._
import com.raphtory.core.utils.Utils.getManager
import spray.json._

/**
  * The Graph Manager is the top level actor in this system (under the stream)
  * which tracks all the graph partitions - passing commands processed by the 'command processor' actors
  * to the correct partition
  */

/**
  * The Command Processor takes string message from Kafka and translates them into
  * the correct case Class which can then be passed to the graph manager
  * which will then pass it to the graph partition dealing with the associated vertex
  */

class RandomRouter(routerId:Int,override val initialManagerCount:Int) extends RouterSlave {

  //************* MESSAGE HANDLING BLOCK

  def parseRecord(record:Any):Unit={
    val command = record.asInstanceOf[String]
    val parsedOBJ = command.parseJson.asJsObject //get the json object
    val commandKey = parsedOBJ.fields //get the command type
    if(commandKey.contains("VertexAdd")) vertexAdd(parsedOBJ.getFields("VertexAdd").head.asJsObject)
    else if(commandKey.contains("VertexUpdateProperties")) vertexUpdateProperties(parsedOBJ.getFields("VertexUpdateProperties").head.asJsObject)
    else if(commandKey.contains("VertexRemoval")) vertexRemoval(parsedOBJ.getFields("VertexRemoval").head.asJsObject)
    else if(commandKey.contains("EdgeAdd")) edgeAdd(parsedOBJ.getFields("EdgeAdd").head.asJsObject) //if addVertex, parse to handling function
    else if(commandKey.contains("EdgeUpdateProperties")) edgeUpdateProperties(parsedOBJ.getFields("EdgeUpdateProperties").head.asJsObject)
    else if(commandKey.contains("EdgeRemoval")) edgeRemoval(parsedOBJ.getFields("EdgeRemoval").head.asJsObject)
    else println(command)
  }

  def vertexAdd(command:JsObject):Unit = {
    val msgTime = command.fields("messageID").toString().toLong
    val srcId = command.fields("srcID").toString().toInt                 //extract the srcID
    if(command.fields.contains("properties")) {                          //if there are properties within the command
      var properties = Map[String,String]()                              //create a vertex map
      command.fields("properties").asJsObject.fields.foreach( pair => {  //add all of the pairs to the map
        properties = properties updated (pair._1, pair._2.toString())
      })
      //send the srcID and properties to the graph manager
      toPartitionManager(VertexAddWithProperties(routerId,msgTime,srcId,properties))
    }
    else {
      toPartitionManager(VertexAdd(routerId,msgTime,srcId))
    } // if there are not any properties, just send the srcID
  }

  def vertexUpdateProperties(command:JsObject):Unit={
    val msgTime = command.fields("messageID").toString().toLong
    val srcId = command.fields("srcID").toString().toInt //extract the srcID
    var properties = Map[String,String]() //create a vertex map
    command.fields("properties").asJsObject.fields.foreach( pair => {properties = properties updated (pair._1,pair._2.toString())})
    toPartitionManager(VertexUpdateProperties(routerId,msgTime,srcId,properties)) //send the srcID and properties to the graph parition
  }

  def vertexRemoval(command:JsObject):Unit={
    val msgTime = command.fields("messageID").toString().toLong
    val srcId = command.fields("srcID").toString().toInt //extract the srcID
    toPartitionManager(VertexRemoval(routerId,msgTime,srcId))
  }

  def edgeAdd(command:JsObject):Unit = {
    val msgTime = command.fields("messageID").toString().toLong
    val srcId = command.fields("srcID").toString().toInt //extract the srcID
    val dstId = command.fields("dstID").toString().toInt //extract the dstID
    if(command.fields.contains("properties")){ //if there are properties within the command
    var properties = Map[String,String]() //create a vertex map
      command.fields("properties").asJsObject.fields.foreach( pair => { //add all of the pairs to the map
        properties = properties updated (pair._1,pair._2.toString())
      })
      toPartitionManager(EdgeAddWithProperties(routerId,msgTime,srcId,dstId,properties))
    }
    else toPartitionManager(EdgeAdd(routerId,msgTime,srcId,dstId))
  }

  def edgeUpdateProperties(command:JsObject):Unit={
    val msgTime = command.fields("messageID").toString().toLong
    val srcId = command.fields("srcID").toString().toInt //extract the srcID
    val dstId = command.fields("dstID").toString().toInt //extract the dstID
    var properties = Map[String,String]() //create a vertex map
    command.fields("properties").asJsObject.fields.foreach( pair => {properties = properties updated (pair._1,pair._2.toString())})
    toPartitionManager(EdgeUpdateProperties(routerId,msgTime,srcId,dstId,properties))//send the srcID, dstID and properties to the graph manager
  }

  def edgeRemoval(command:JsObject):Unit={
    val msgTime = command.fields("messageID").toString().toLong
    val srcId = command.fields("srcID").toString().toInt //extract the srcID
    val dstId = command.fields("dstID").toString().toInt //extract the dstID
    toPartitionManager(EdgeRemoval(routerId,msgTime,srcId,dstId))
  }
}
