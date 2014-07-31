package setUp

import java.net._
import java.util.Properties

import com.google.gson.Gson
import jtools.io.RichFile._

case class Config(user: String, pass: String, proxy: String, port: Int)

object SetUp {

  def loadProxyAcount {

    val sysProps: Properties = System.getProperties

    val gson = new Gson()
    val conf = gson.fromJson("config.json".readStr, classOf[Config])

    //    val conf = parse[Config]("config.json".readStr)

    sysProps put("http.proxyHost", conf.proxy)

    sysProps put("http.proxyPort", conf.port.toString)

    Authenticator.setDefault(new Authenticator {
      override def getPasswordAuthentication() =
        new PasswordAuthentication(conf.user, conf.pass.toCharArray)
    })

  }

}
