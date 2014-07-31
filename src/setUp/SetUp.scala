package setUp

import java.util.Properties
import java.net._

import jtools.io.RichFile._
import com.codahale.jerkson.Json._

case class Config(user: String, pass: String, proxy: String, port: Int)

object SetUp {

  def loadProxyAcount {

    val sysProps: Properties = System.getProperties

    val conf = parse[Config]("config.json".readStr)

    sysProps put ("http.proxyHost", conf.proxy)

    sysProps put ("http.proxyPort", conf.port.toString)

    Authenticator.setDefault(new Authenticator {
      override def getPasswordAuthentication() =
        new PasswordAuthentication(conf.user, conf.pass.toCharArray)
    })

  }

}