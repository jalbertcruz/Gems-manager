package jtools

import java.util

import com.google.gson.Gson
import jtools.io.RichFile._
import setUp.SetUp

import scala.collection.JavaConverters._

class ManagerFacade {

  if ("config.json".fileExists)
    SetUp.loadProxyAcount

  val obj = new GemsManager
  val gson = new Gson()

  def downloadNewGems(){
    obj.downloadNewGems()
  }

  /**
   * Initilize the repo: Obtain the names of all the gems
   */
  def init() {

    if (!obj.gemFolderName.fileExists)
      obj.gemFolderName.mkdirs
    else
      obj.gemFolderName.deleteAll

    if (!obj.backUpFolderName.fileExists)
      obj.backUpFolderName.mkdirs
    else
      obj.backUpFolderName.deleteAll

    val total = obj.genAllGems

    "allNames.txt".write(gson.toJson(total.asJavaCollection))
    "current.json".write(gson.toJson(RunConfig(-1, total.length)))
    //    "allNames.txt".write(generate(total))
    //
    //    "current.json".write(generate(RunConfig(-1, total.length)))

  }

  /**
   * Obtain the names of the versions of a number of gems
   */
  def process(count: Int) {

    val conf = gson.fromJson("current.json".readStr, classOf[RunConfig])
    //    val conf = parse[RunConfig]("current.json".readStr)

    obj.mkIndex(conf.actual_gem, count)

  }

  def processAll() {
    obj.mkIndex()
  }

  /**
   * Get all the URLs of the don't downloaded gems ( objects_to_download.txt )
   * Build a text file on each gem's dir whit the name of the don't downloaded gems
   */
  def checkIndex() {
    obj.checkIndex("objects_to_download.txt")
  }

  def getDiff(dir: String, file: String) = {

    val gemsAlreadyDownloaded = (for (f1 <- dir.listFiles if f1.isFile && f1.getName.endsWith(".gem")) yield f1.getName).toSet

    println("Son: " + gemsAlreadyDownloaded.size + " gems en el directorio")

    val all = file.readLines.map(f => {

      val index = f.lastIndexOf("/")

      f.substring(index + 1)

    }).toSet

    println("Son: " + all.size + " gems en el fichero")

    (all diff gemsAlreadyDownloaded).toList.map("http://rubygems.org/downloads/" + _)

  }

  /**
   * Get as input a number (n) a create a .txt file with n gems's URLs for download
   */
  def getAllUrls2download() {

    val current = if
    ("marker.json".fileExists)
      gson.fromJson("marker.json".readStr, classOf[Int])
    else 0
    //    val current = parse[Int]("marker.json".readStr)

    val wget_list = "objects_to_download.txt".readLines.map(gson.fromJson(_, classOf[GemLocData]))
    //    val wget_list = "objects_to_download.txt".readLines(current, count).map(parse[GemLocData](_))

    //        "wgcmd.sh".write("wget -i new_urls.txt")

    "./gems2download/".mkdirs

    "./gems2download/new_urls.txt".write(wget_list.map("http://rubygems.org/downloads/" + _.version).mkString("\n"))

    "./gems2download/restituye.json".write(gson.toJson(wget_list.asJavaCollection))

    "marker.json".write(gson.toJson(wget_list.length))

    //    "./gems2download/restituye.json".write(generate(wget_list))
    //
    //    "marker.json".write(generate(current + count))
  }


  /**
   * Get as input a number (n) a create a .txt file with n gems's URLs for download
   */
  def getUrls2download(count: Int) {

    val current = if
    ("marker.json".fileExists)
      gson.fromJson("marker.json".readStr, classOf[Int])
    else 0
    //    val current = parse[Int]("marker.json".readStr)

    val wget_list = "objects_to_download.txt".readLines(current, count).map(gson.fromJson(_, classOf[GemLocData]))
    //    val wget_list = "objects_to_download.txt".readLines(current, count).map(parse[GemLocData](_))

    //        "wgcmd.sh".write("wget -i new_urls.txt")

    "./gems2download/".mkdirs

    "./gems2download/new_urls.txt".write(wget_list.map("http://rubygems.org/downloads/" + _.version).mkString("\n"))

    "./gems2download/restituye.json".write(gson.toJson(wget_list.asJavaCollection))

    "marker.json".write(gson.toJson(current + count))

    //    "./gems2download/restituye.json".write(generate(wget_list))
    //
    //    "marker.json".write(generate(current + count))
  }

  /**
   * Once executed ´get-urls2download´ command it's used this for move de gems for it's folder
   */
  def mvGems2Fldrs() {
    val wg1 = gson.fromJson("./gems2download/restituye.json".readStr, classOf[util.ArrayList[GemLocData]])
    val wget_list = wg1.asScala.toList
    //    val wget_list = parse[List[GemLocData]]("./gems2download/restituye.json".readStr)

    wget_list.foreach(e => {

      ("./gems2download/" + e.version).moveTo(obj.gemFolderName + e.name + "/" + e.version)

    })

    val cant = (for (f <- "./history/restituye/".listFiles if f.getName.endsWith(".json")) yield f).length

    ("./gems2download/restituye.json").moveTo("./history/restituye/restituye" + (cant + 1) + ".json")

  }

  /**
   * Move all the gems for a ´webgems´
   */
  def mvAllgems2webserverFolder {

    "./webgems/".mkdirs

    for (f <- "./gems/".listFiles if f.isDirectory) {
      for (g <- f.listFiles if g.getName.endsWith(".gem"))
        g.moveTo("./webgems/")
    }

  }

  def showHelp() {

    val commands = List(

      "init --> Initilize the repo: " +
        "Obtain the names of all the gems",

      "proc --> Obtain the names of the versions of a number of gems",

      "check-index --> Get all the URLs of the don't downloaded gems ( objects_to_download.txt ). " +
        "Build a text file on each gem's dir whit the name of the don't downloaded gems",

      "get-urls2download --> Get as input a number (n) a create a .txt file with n gems's URLs for download",

      "mvGems2Fldrs --> Once executed ´get-urls2download´ command it's used this for move de gems for it's folder",

      "mvAllgems2webserverFolder --> Move all the gems for a ´webgems´")

    commands.foreach(println)
  }

}
