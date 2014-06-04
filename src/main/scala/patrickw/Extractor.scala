package patrickw

import scales.xml._
import scales.xml.Functions._
import scales.xml.ScalesXml._

import patrickw.TextCleaner._
import patrickw.RuleError._

object Extractor {
  private type KS = (Key, String)

  private val kswOrdering = Ordering.by { ksw: (KS, Weight) => ksw._2 }

  def evaluate(wm: WeightedMap, root: XmlPath): ExtractionReport = {
    val report = wm map { case (kr, w) => kr -> (evaluateRule(kr.rule, root), w) }
    (extract(report), report)
  }

  def evaluateRule(rule: Rule, root: XmlPath): RuleResult =
    (rule.xpath evaluate root) . headOption match {
      case Some(ae) =>
        val raw = ae fold (a => cleanAttribute(a.value), t => cleanText(text(t)))
        rule.regex match {
          case Some((r, g)) => r findFirstMatchIn raw match {
            case Some(m) => Right(m group g)
            case None => Left(NonMatchingText)
          }
          case None => Right(raw)
        }
      case None => Left(EmptyPath)
    }

  def extract(am: AppliedMap): WeightedExtraction = {
    val m = scala.collection.mutable.Map[KS, Weight]()

    for {
      (kr, (rr, w)) <- am
      s <- rr.right.toOption
      k = kr.key
      ks = (k, s)
    } {
      m put (ks, (m get ks) . foldLeft (w) { _ + _ })
    }

    ((m groupBy { _._1._1 }) . view map {
      case (k, ksws) =>
        val best = ksws max kswOrdering
        k -> (best._1._2, best._2)
    }) . toMap
  }
}
