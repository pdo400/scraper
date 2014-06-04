package patrickw

import scala.annotation.tailrec
import scala.collection.SeqView

import java.util.regex.Pattern

import scales.utils._
import scales.xml._
import scales.xml.Functions._
import scales.xml.ScalesXml._

import patrickw.TextCleaner._
import patrickw.XPathBuilder.xpath

object RuleGenerator {
  final val Space = ' '

  def generateRules(doc: Doc, ex: Extraction): UnweightedSet = {
    val s = scala.collection.mutable.Set[KeyRule]()
    val sb = new StringBuilder

    def addRules(key: Key, value: String, xpath: String, text: String) {
      if (text == value)
        s += ((key, Rule(xpath, None)))
      else
        s ++= (generateRegexes(text, value, sb) map { r => (key, Rule(xpath, Some(r))) })
    }

    for (((key, value), path, text) <- matchingAttributes(doc, ex)) {
      addRules(key, value, xpath(path), text)
    }

    for (((key, value), path, text) <- matchingTexts(doc, ex)) {
      addRules(key, value, xpath(path), text)
    }

    s . toSet
  }

  def matchingAttributes(doc: Doc, extractions: Extraction) =
    for {
      p <- top(doc) . \\@
      t = cleanAttribute(p.value)
      ex <- extractions
      if t contains ex._2
    } yield
      (ex, p, t)

  def matchingTexts(doc: Doc, extractions: Extraction) =
    for {
      p <- top(doc) . \\ . textOnly
      t = cleanText(text(p))
      ex <- extractions
      if t contains ex._2
    } yield
      (ex, p, t)

  @tailrec
  def generateRegexes(from: String, to: String, sb: StringBuilder, startIndex: Int = 0, acc: List[(String, Int)] = Nil): List[(String, Int)] =
    from indexOf(to, startIndex) match {
      case -1 => acc
      case i =>
        generateRegexes(from, to, sb, i + 1, {
          sb.clear()
          if (
            appendPrefix(sb, from view (0, i))
              && appendCapture(sb)
              && appendSuffix(sb, from view (i + to.length, from.length)))
            (sb.toString(), 1) :: acc
          else
            acc
        })
    }

  def appendCapture(sb: StringBuilder): Boolean = {
    sb append "(.*)"
    true
  }

  def appendPrefix(sb: StringBuilder, sv: SeqView[Char, String]): Boolean = {
    sb append '^'
    sv.isEmpty || {
      sv.last match {
        case x if x.isLetterOrDigit => false
        case c =>
          val spaces = sv count { _ == Space }

          if (spaces > 0)
            sb append "(?:[^ ]* ){" append spaces append "}+"

          if (c != Space) {
            val q = Pattern quote c.toString
            val n = { if (spaces == 0) sv else sv.reverse takeWhile { _ != Space } } count { _ == c }
            sb append "(?:[^ " append q append "]*" append q append "){" append n append "}+"
          }

          true
      }
    }
  }

  def appendSuffix(sb: StringBuilder, sv: SeqView[Char, String]): Boolean =
    { sv.isEmpty ||
      { sv.head match {
        case x if x.isLetterOrDigit => false
        case c =>
          val spaces = sv count { _ == Space }

          if (c != Space) {
            val q = Pattern quote c.toString
            val n = { if (spaces == 0) sv else sv takeWhile { _ != Space } } count { _ == c }
            sb append "(?:" append q append "[^ " append q append "]*){" append n append "}+"
          }

          if (spaces > 0)
            sb append "(?: [^ ]*){" append spaces append "}+"

          true
      } }
    } && {
      sb append '$'
      true
    }

}
