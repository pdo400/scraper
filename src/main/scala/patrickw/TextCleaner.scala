package patrickw

import scala.util.matching.Regex

object TextCleaner {
  def cleanAttribute(s: String) = process(s, attributeReplacements)

  def cleanText(s: String) = process(s, textReplacements)

  private[this] def process(s: String, rs: Replacements) =
    rs.foldLeft (s) { (acc, next) => next._1.replaceAllIn(acc, next._2) } . trim

  private[this] type Replacements = Seq[(Regex, String)]

  private[this] val space = " "

  private[this] val ws = """"[\s\p{Z}]"""
  private[this] val wsNotSpace = """[\s\p{Z}&&[^ ]]"""

  private[this] val collapsableWs = s"""${ws}*${wsNotSpace}${ws}*|${space}${space}+""".r

  private[this] val attributeReplacements: Replacements = Seq((collapsableWs, space))
  private[this] val textReplacements: Replacements = attributeReplacements
}
