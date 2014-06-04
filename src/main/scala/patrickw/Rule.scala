package patrickw

import scales.xml.jaxen.ScalesXPath

case class Rule(path: String, transformation: Option[(String, Int)]) {
  val xpath = ScalesXPath(path) withNameConversion ScalesXPath.localOnly
  val regex = transformation map { case (s, g) => (s.r, g) }
}
