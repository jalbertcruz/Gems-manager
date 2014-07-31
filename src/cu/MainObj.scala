
package cu

import com.codahale.jerkson.Json._

import jtools.io.RichFile._

import setUp.SetUp

object MainObj extends App {

  if (args.length > 0) {

    val m = new ManagerFacade

    args(0) match {
      
      case "init" =>
      	m.init
      	
      case "proc" =>
        m.proc(args(1).toInt)

      case "check-index" =>

        m.checkIndex

      case "get-urls2download" =>

        m.getUrls2download(args(1).toInt)
        
      case "mvGems2Fldrs" =>
        
        m.mvGems2Fldrs
        
      case "getDiff" =>
        
        val diff = m.getDiff(args(1), args(2))
        
        "diferencia.txt".write(diff.mkString("\n"))
        
      case _ =>
        
        m.showHelp

    }

  }

  //   val allGemsNames = parse[List[GemEntry]]("allNames.txt".readStr)
  //   
  //  "current.json".write(generate(RunConfig(-1, allGemsNames.length)))
  //  
  //"current.json".write(generate(RunConfig(conf.actual_gem + delta)))  

  //  updateAllGemsDependencies("rails")

}
