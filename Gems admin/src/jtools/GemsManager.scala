package jtools

import java.io._
import java.util

import com.google.gson.Gson
import jtools.io.RichFile._

import scala.collection.JavaConverters._
import scala.io.Source
import scala.sys.process.{Process, ProcessLogger}

class GemsManager {

  def execAndWait(cmd: String) = {
    var pb: Process = null

    pb = Process(cmd) run ProcessLogger(
      n => {
        println(n)
      },
      e => {
        println("Error>> " + e)
        //        pb.destroy
      })

    val res = pb.exitValue()
    println("exitValue: " + res)
    res
  }

  def downloadNewGems(){
    execAndWait("downloadNewGems.sh")
  }

  var _c = 0
  val gemFolderName = "./gems/"
  val backUpFolderName = "./backup/"

  /**
   * Dada una letra determina la cantidad de páginas de gems que están agrupados en dicha letra (sus nombres
   * comienzan con ella).
   */
  def pagesCount(letter: Char): Int = {

    val url = "http://rubygems.org/gems?letter=" + letter

    _c = 0

    println("\nBajando: " + url)

    lazy val src = Source.fromURL(url)("UTF-8").getLines.mkString("\n")

    //    ("backup/" + letter + "_0.html").write(src)

    lazy val p1 = src.indexOf( """<a class="next_page" rel="next"""")

    lazy val s2 = src.substring(0, p1)

    lazy val p2 = s2.lastIndexOf("href=")

    lazy val re = ("""(/gems\?letter=""" + letter + """&amp;page=)(\d+)""").r

    lazy val res = re findFirstIn s2.substring(p2) match {

      case Some(re(_, n)) => n.toInt

      case _ => 0
    }

    res

  }

  private[this] def logPage(letter: Char, url: String, src: String): Unit = {

    println("Bajado: " + url)

    _c += 1

    ("backup/" + letter + "_" + _c + ".html").write(src)
  }

  /**
   * Dada una letra y una posicion relativa a dicha letra, devuelve una lista de los nombres de todos los gems que están
   * en la misma.
   */
  def gemsName(letter: Char, pos: Int): List[GemEntry] = {

    val url = "http://rubygems.org/gems?letter=" + letter + "&page=" + pos

    //    val src = "d:/workspace/gemlists/backup/A_1.html".readStr

    lazy val src = Source.fromURL(url)("UTF-8").getLines.mkString("\n")

    logPage(letter, url, src)

    val m1 = """<div class="gems border">"""

    val m2 = """<div class="pagination">"""

    val p1 = src.indexOf(m1) + m1.length

    val p2 = src.indexOf(m2)

    val s1 = src.substring(p1, p2)

    val units = s1.split( """<div class="downloads"><strong>""")

    val extract = (s: String) => {

      lazy val rname = """href="/gems/(.+)"""".r

      val name = rname findFirstIn s match {
        case Some(rname(name)) => name
        case _ => ""
      }

      lazy val rversion = """\((.+)\)""".r

      val version = rversion findFirstIn s match {
        case Some(rversion(ver)) => ver
        case _ => ""
      }

      GemEntry(name, version)
    }

    (units.slice(1, units.length).map(extract)).toList
  }

  /**
   * Dado un gem determina los nombres de todas sus versiones.
   */
  private[this] def allGemsURLs(gem: String) = {
    //  val gemName = "nokogiri"

    lazy val src = Source.fromURL("http://rubygems.org/gems/" + gem +
      "/versions")("UTF-8").getLines.mkString("\n")

    //  val src = "builder.html".readStr

    (gemFolderName + gem + "/" + gem + ".html").write(src)

    lazy val rversions = (""""http://rubygems.org/gems/""" + gem + """/versions/([\p{Alnum}\.\-]+)""").r

    val versions = for (rversions(ver) <- rversions findAllIn src) yield ver

    // http://rubygems.org/downloads/
    versions.map(v => gem + "-" + v + ".gem")

  }

  /**
   * Destinado a inicializar la BD de gems.
   *
   * Obtiene los nombres de todos los gems que existen en RubyGems, yendo por cada letra del directorio.
   */
  def genAllGems = {

    val letters = 'A' to 'Z'

    def genAllGems4Letter(letter: Char) = {

      val cantidad = pagesCount(letter)

      println("\nHay " + cantidad + " paginas en la letra " + letter + "\n")

      (for (ind <- 1 to cantidad)
      yield gemsName(letter, ind)).flatten

    }

    letters.map(genAllGems4Letter).flatten
  }

  /**
   * Dado un gem, determina todos sus links (versiones).
   *
   * No tiene en cuenta lo bajado con anterioridad.
   */
  private[this] def mkOneGemEntry(nname: String): List[String] = {

    val path = gemFolderName + nname

    (new File(path)).mkdirs

    (path + "/" + nname + ".txt").write(allGemsURLs(nname).mkString("\n"))

    (path + "/" + nname + ".json").write(
      Source.fromURL("http://rubygems.org/api/v1/gems/" + nname + ".json")("UTF-8").getLines.mkString("\n"))

    (path + "/" + nname + ".json").readLines
  }

  /**
   * Dada una lista de gems, determina todos los links (versiones) de ellos.
   *
   * No tiene en cuenta lo bajado con anterioridad.
   */
  def links_to_download(allGemsNames: List[GemEntry]) {

    if (!gemFolderName.fileExists)
      gemFolderName.mkdirs

    val gson = new Gson()
    val conf = gson.fromJson("current.json".readStr, classOf[RunConfig])

    try {
      for (i <- 0 to allGemsNames.length - 1) {

        val nname = allGemsNames(i).name

        println("Procesando la entrada: " + nname + " (indice " + i.toString + ")")

        mkOneGemEntry(nname)

        conf.actual_gem += 1

      }
    } finally {
      "current.json".write(gson.toJson(conf))
    }

  }

  /**
   * Determina todas todas las dependencias de la versión actual (en internet) de un gem
   *
   * @param gem El gem a analizar
   * @example {{{
   *          }}}
   */
  def dependencies(gem: String, acc: List[GemDep]): List[GemDep] = {

    //    val g = parse[GemInfo](Source.fromURL("http://rubygems.org/api/v1/gems/" + gem + ".json")("UTF-8").getLines.mkString)
    val gson = new Gson()
    val g = gson.fromJson(Source.fromURL("http://rubygems.org/api/v1/gems/" + gem + ".json")("UTF-8").getLines.mkString, classOf[GemInfo])

    val dps = g.dependencies("runtime") // las presentes en la pagina web (las directas)

    val ndps = for (a <- dps if (!acc.contains(a))) yield a // elimino aquellas que ya estan en ´acc´

    var res = ndps

    for (a <- ndps)
      res ++= dependencies(a.name, res)

    res

  }

  def dependenciesFromCache(gem: String) = {
    //TODO: Queda pendiente chequear las dependencias a partir de lo ya bajado...
    val gson = new Gson()
    val g = gson.fromJson(gemFolderName + gem
      + "/" + gem
      + ".json".readStr, classOf[GemInfo])

    //    val g = parse[GemInfo](("./gems/" + gem
    //      + "/" + gem
    //      + ".json".readStr))

    g.dependencies("runtime")

  }

  /**
   * Chequea la correspondencias entre lo que ya se ha bajado y lo que existe en internet,
   * para determinar si hay novedades.
   */
  private[this] def isUpdated(gem: String) = {
    val gson = new Gson()
    val g = gson.fromJson(Source.fromURL("http://rubygems.org/api/v1/gems/" + gem + ".json")("UTF-8").getLines.mkString, classOf[GemInfo])

    //    val g = parse[GemInfo](Source.fromURL("http://rubygems.org/api/v1/gems/" + gem + ".json")("UTF-8").getLines.mkString)

    val fpath = (gemFolderName + gem
      + "/" + gem
      + ".json")

    if (fpath.fileExists) {

      val gson = new Gson()
      val c = gson.fromJson(fpath.readStr, classOf[GemInfo])

      //      val c = parse[GemInfo](fpath.readStr)

      g.version == c.version // resultado de la funcion

    } else false

  }

  /**
   * Dado un gem busca las actualizaciones necesarias para el y todas sus dependencias.
   */
  def updatedsNeed(gem: String) = {

    val ds = for (d <- dependencies(gem, List()) if (!isUpdated(d.name))) yield d.name

    if (isUpdated(gem))
      ds
    else
      gem :: ds
  }

  /**
   * Busca las versiones que han salido nuevas luego de la ultima actualizacion
   */
  def updateThisGem(gem: String): List[String] = {

    if (!(gemFolderName + gem + "/" + gem + ".json").fileExists) {
      mkOneGemEntry(gem)
    } else {

      val current = allGemsURLs(gem) // - busco en su pagina todas las versiones

      val downloads = (gemFolderName + gem + "/" + gem + ".txt").readLines // - chequeo con las que ya han sido bajadas

      (current.toSet diff downloads.toSet).toList // - devuelvo la diferencia
    }
  }

  /**
   * Dado el nombre de un gem actualiza las entradas de ´<<gem>>.txt´ y crea un nuevo ´<<gem_news>>.txt´ con las nuevas.
   */
  def updateAllGemsDependencies(gem: String) {

    val fpath = (gemFolderName + gem
      + "/" + gem
      + ".json")

    var ne = List(gem)

    if (!fpath.fileExists) {

      mkOneGemEntry(gem)
      ne = List()
    }

    val gems = updatedsNeed(gem)

    for (g <- ne ::: gems) {

      val upd = updateThisGem(g)

      (gemFolderName + gem + "/" + gem + ".txt").append(upd.mkString("\n"))

      // ("./gems/" + gem + "/" + gem + "_news.txt").write(upd.mkString("\n")) --> Ya no es necesario dado que esto se realizará para obtener las URLs de los gems a bajar.

    }

  }

  /**
   * Actualización de todo el repo.
   *
   * Baja los nombres de todos los gems de RubyGems y los actualiza.
   */
  def updateRepo =
    genAllGems.map(e => e.name).foreach(updateAllGemsDependencies)

  /**
   * Efectúa la creación de la lista de versiones de los gems desde el índice from hasta (from + count)
   */
  def mkIndex(from: Int, count: Int) {

    val gson = new Gson()
    val allGemsNames1 = gson.fromJson("allNames.txt".readStr, classOf[util.ArrayList[com.google.gson.internal.LinkedTreeMap[String, String]]])
    val all2 = new util.ArrayList[GemEntry]()
    allGemsNames1.asScala.foreach((e: com.google.gson.internal.LinkedTreeMap[String, String]) => {
      all2.add(GemEntry(e.get("name"), e.get("last_version")))
    })
    val allGemsNames = all2.asScala.toList

    //    val allGemsNames1 = gson.fromJson("allNames.txt".readStr, classOf[util.ArrayList[GemEntry]])
    //    val allGemsNames = allGemsNames1.asScala.toList
    //    val allGemsNames = parse[List[GemEntry]]("allNames.txt".readStr)

    if (from + count < allGemsNames.length)
      links_to_download(allGemsNames.slice(from + 1, from + count + 1))

    else
      links_to_download(allGemsNames.slice(from + 1, allGemsNames.length))

  }

  /**
   * Efectúa la creación de la lista de versiones de los gems desde el índice from hasta (from + count)
   */
  def mkIndex() {

    val gson = new Gson()
    val allGemsNames1 = gson.fromJson("allNames.txt".readStr, classOf[util.ArrayList[com.google.gson.internal.LinkedTreeMap[String, String]]])
    val all2 = new util.ArrayList[GemEntry]()
    allGemsNames1.asScala.foreach((e: com.google.gson.internal.LinkedTreeMap[String, String]) => {
      all2.add(GemEntry(e.get("name"), e.get("last_version")))
    })
    val allGemsNames = all2.asScala.toList

    links_to_download(allGemsNames)

  }


  /**
   * Puts all the URLs of the don't downloaded gems in a file name ´urls´.
   */
  def checkIndex(urls: String) {

    urls.deleteAll

    var nl = ""

    for (f <- gemFolderName.listFiles if f.isDirectory) {

      val versionsList = (new File(f, f.getName + ".txt"))

      if (versionsList.exists) {

        println("Procesando " + f.getName)

        val allGemsAlreadyDownloaded = (for (f1 <- f.listFiles if f1.isFile && f1.getName.endsWith(".gem")) yield f1.getName).toSet

        val allVersionsGems = versionsList.readLines.toSet

        val diff = (allVersionsGems diff allGemsAlreadyDownloaded).toList

        //         println("cantidad: " + diff.length)

        (gemFolderName + f.getName + "/" + f.getName + "_news.txt").write(diff.mkString("\n"))

        val gson = new Gson()

        urls.append(nl + diff.map(version => gson.toJson(GemLocData(f.getName, version))).mkString("\n"))
        //        urls.append(nl + diff.map(version => generate(GemLocData(f.getName, version))).mkString("\n"))

        nl = "\n"

      }
    }

  }

}