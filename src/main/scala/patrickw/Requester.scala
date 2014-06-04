package patrickw

import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object Requester {
  def get(url: URL, headers: Map[String, String] = Map.empty): InputStream = {
    val conn = url.openConnection().asInstanceOf[HttpURLConnection]
    for ((k, v) <- headers)
      conn.setRequestProperty(k, v)
    conn.getInputStream
  }
}
