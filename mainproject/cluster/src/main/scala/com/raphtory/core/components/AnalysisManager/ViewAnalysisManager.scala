package com.raphtory.core.components.AnalysisManager

import java.util.Date

import akka.cluster.pubsub.DistributedPubSubMediator
import com.raphtory.core.model.communication.AnalyserPresentCheck
import com.raphtory.core.utils.Utils

abstract class ViewAnalysisManager(jobID:String,time:Long) extends LiveAnalysisManager(jobID:String) {
  override def timestamp():Long = time

  override def restart() = {
    println(s"View Analaysis manager for $jobID at ${new Date(time)} finished")
    System.exit(0)
  }
}
//1471459626000L

