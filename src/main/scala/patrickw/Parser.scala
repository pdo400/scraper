package patrickw

import org.xml.sax.{XMLReader, InputSource}
import org.xml.sax.helpers.XMLReaderFactory

import scales.utils._
import scales.utils.ScalesUtils._
import scales.xml._
import scales.xml.Functions._
import scales.xml.ScalesXml._

object Parser {
  trait ProcessSource {
    def processSource(source: InputSource): Unit = ()
  }

  type Pool = SimpleUnboundedPool[XMLReader] with DefaultSaxSupport with ProcessSource

  def parse(source: InputSource, pool: Pool = TagSoup) = {
    pool.processSource(source)
    loadXmlReader(source, parsers = pool)
  }

  val Nu: Pool = new SimpleUnboundedPool[XMLReader] with DefaultSaxSupport with ProcessSource {
    override def create = {
      import nu.validator.htmlparser.{sax,common}
      import sax.HtmlParser
      import common.XmlViolationPolicy

      val reader = new HtmlParser
      reader.setXmlPolicy(XmlViolationPolicy.ALLOW)
      reader.setXmlnsPolicy(XmlViolationPolicy.ALLOW)
      reader
    }
  }

  val TagSoup: Pool = new SimpleUnboundedPool[XMLReader] with DefaultSaxSupport with ProcessSource {

    override def processSource(source: InputSource) =
      if (source.getEncoding == null)
        source.setEncoding("UTF-8")

    override def getXmlVersion(reader : XMLReader): AnyRef = null

    override def create = {
      val reader = new org.ccil.cowan.tagsoup.Parser
      reader.setFeature(org.ccil.cowan.tagsoup.Parser.namespacesFeature, false)
      reader
    }
  }

  val Sax: Pool = new SimpleUnboundedPool[XMLReader] with DefaultSaxSupport with ProcessSource {
    override def create = XMLReaderFactory.createXMLReader()
  }
}
