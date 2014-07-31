package main

import jtools._
import jtools.io.RichFile._

object MainObj extends App {

  if (args.length > 0) {

    val m = new ManagerFacade

    args(0) match {

      case "doAll" =>
        //        m.init
        m.processAll()
        m.checkIndex()
        m.getAllUrls2download()
        m.downloadNewGems()

      case "downloadNewGems" =>
        m.downloadNewGems()

      case "init" =>
        m.init()

      case "process" =>
        m.process(args(1).toInt)
      //
      //      case "processAll" =>
      //        m.processAll()

      case "check-index" =>
        m.checkIndex()

      case "get-urls2download" =>
        m.getUrls2download(args(1).toInt)

      case "mvGems2Fldrs" =>
        m.mvGems2Fldrs()

      case "getDiff" =>
        val diff = m.getDiff(args(1), args(2))

        "diferencia.txt".write(diff.mkString("\n"))

      case _ =>
        m.showHelp()

    }

  }

  //   val allGemsNames = parse[List[GemEntry]]("allNames.txt".readStr)
  //
  //  "current.json".write(generate(RunConfig(-1, allGemsNames.length)))
  //
  //"current.json".write(generate(RunConfig(conf.actual_gem + delta)))

  //  updateAllGemsDependencies("rails")

}

