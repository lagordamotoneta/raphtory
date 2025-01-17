package com.raphtory.examples.bitcoin.actors;

import com.raphtory.core.components.AnalysisManager.LiveAnalysisManager
import com.raphtory.core.analysis.Analyser
import com.raphtory.examples.bitcoin.analysis.BitcoinAnalyser
import com.raphtory.examples.bitcoin.communications.CoinsAquiredPayload

import scala.collection.mutable.ArrayBuffer

class BitcoinLiveAnalysisManager(jobID:String) extends LiveAnalysisManager(jobID) {
    override protected def defineMaxSteps(): Int = 1

    override protected def generateAnalyzer: Analyser = new BitcoinAnalyser()

    override protected def processResults(): Unit = {
        var finalResults = ArrayBuffer[(String, Double)]()
        var highestBlock = 0
        var blockHash = ""
        for(indiResult <- results.asInstanceOf[(ArrayBuffer[CoinsAquiredPayload])]){
            for (pair <- indiResult.wallets){
               finalResults :+= pair
            }
            if(indiResult.highestBlock>highestBlock){
                highestBlock = indiResult.highestBlock
                blockHash = indiResult.blockhash
            }
        }
        println(s"Current top three wallets at block $highestBlock ($blockHash)")
        finalResults.sortBy(f => f._2)(Ordering[Double].reverse).take(3).foreach(pair =>{
            println(s"${pair._1} has acquired a total of ${pair._2} bitcoins ")
        })

    }

    override protected def processOtherMessages(value: Any): Unit = Unit

}
