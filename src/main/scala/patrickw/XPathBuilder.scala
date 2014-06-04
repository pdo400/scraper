package patrickw

import scales.xml._
import scales.xml.xpath.Ancestors

import scales.utils._
import scales.utils.ScalesUtils._
import scales.xml._
import scales.xml.Functions._
import scales.xml.jaxen._
import scales.xml.ScalesXml._
import scales.xml.xpath._
import scala.annotation.tailrec

object XPathBuilder {
  def xpath(path: XmlPath): String =
    appendPath(path, ancestorPath(path)).toString()

  def xpath(path: AttributePath): String =
    appendPath(path, ancestorPath(path)).toString()

  def appendPath(path: XmlPath, sb: StringBuilder): StringBuilder =
    if (path.isItem)
      sb append "/text()" append '[' append textIndex(path) append ']'
    else {
      val qn = qname(path)
      sb append '/' append qn append '[' append elementIndex(path, qn) append ']'
    }

  def appendPath(path: AttributePath, sb: StringBuilder): StringBuilder =
    sb append "/@" append path.name.local

  def ancestorPath(path: XmlPath): StringBuilder =
    new Ancestors(path).foldRight (new StringBuilder) { appendPath }

  def ancestorPath(path: AttributePath): StringBuilder = {
    val parent = path.parent
    appendPath(parent, ancestorPath(parent))
  }

  @tailrec
  def elementIndex(path: XmlPath, qn: String, acc: Int = 1): Int =
    if (path.hasPreviousSibling) {
      val ps = path.previousSibling
      elementIndex(ps, qn, if (!ps.isItem && qname(ps) == qn) acc + 1 else acc)
    } else
      acc

  @tailrec
  def textIndex(path: XmlPath, acc: Int = 1): Int =
    if (path.hasPreviousSibling) {
      val ps = path.previousSibling
      textIndex(ps, if (ps.isItem) acc + 1 else acc)
    } else
      acc

  @tailrec
  def ancestors(path: XmlPath, acc: List[XmlPath] = Nil): List[XmlPath] =
    if (path.top.isRight) {
      val parentPath = path.top.getRight
      ancestors(parentPath, parentPath :: acc)
    } else
      acc

}
