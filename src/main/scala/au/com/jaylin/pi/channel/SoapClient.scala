package au.com.jaylin.pi.channel

import scala.xml.{Elem, XML}
import org.apache.commons.codec.binary.Base64


class SoapClient {
    def wrapInSOAPEnvelope(xml: Elem): String = {
        val buf = new StringBuilder
        buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n")
        buf.append("<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">\n")
        buf.append("<SOAP-ENV:Body>\n")
        buf.append(xml.toString)
        buf.append("\n</SOAP-ENV:Body>\n")
        buf.append("</SOAP-ENV:Envelope>\n")
        buf.toString
    }
    
    def sendMessage(host: String, req: Elem, user: String, pass: String): Option[Elem] = {
        val url = new java.net.URL(host)
        val outs = wrapInSOAPEnvelope(req).getBytes()
        val conn = url.openConnection.asInstanceOf[java.net.HttpURLConnection]
        
        try {
            conn.setRequestMethod("POST")
            conn.setDoOutput(true)
            conn.setRequestProperty("Content-Length", outs.length.toString)
            conn.setRequestProperty("Content-Type", "text/xml")
            if (user != "" && pass != "") {
                val userpass = user + ":" + pass
                val basicAuth = "Basic " + new String(new Base64().encode(userpass.getBytes()))
                conn.setRequestProperty("Authorization", basicAuth)
            }
            conn.getOutputStream().write(outs)
            conn.getOutputStream().close()
            Some(XML.load(conn.getInputStream()))
        }
        catch {
          case e: Exception => error("post: " + e)
              error("post:" + scala.io.Source.fromInputStream(conn.getErrorStream).mkString)
              None
        }
    }
}