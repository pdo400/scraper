import java.net.URL

package object patrickw {
  type Weight = Double
  type KeyRule = (Key, Rule)
  type RuleResult = Either[RuleError, String]
  type UnweightedSet = Set[KeyRule]
  type WeightedMap = Map[KeyRule, Weight]
  type AppliedMap = Map[KeyRule, (RuleResult, Weight)]
  type Extraction = Map[Key, String]
  type WeightedExtraction = Map[Key, (String, Weight)]
  type ExtractionReport = (WeightedExtraction, AppliedMap)
  type KnownExtractions = Map[URL, Extraction]

  implicit val werOrdering = Ordering.by { oer: OrderableExtractionReport => oer.key }

  implicit class PimpedKeyRule(val kr: KeyRule) extends AnyVal {
    def key = kr._1
    def rule = kr._2
  }

  implicit class PimpedExtractionReport(val er: ExtractionReport) extends AnyVal {
    def extraction = er._1
    def report = er._2
    def toOrderable = new OrderableExtractionReport(er)
  }

  implicit class PimpedWeightedExtraction(val wex: WeightedExtraction) extends AnyVal {
    def unweighted: Extraction =
      wex . foldLeft (Map.empty : Extraction) { (acc, ksw) => acc + (ksw._1 -> ksw._2._1) }
  }

  implicit class PimpedWeightedMap(val wm: WeightedMap) extends AnyVal {
    def unweighted: UnweightedSet =
      wm.foldLeft(Set.empty: UnweightedSet) { (acc, krw) => acc + krw._1 }
  }

  class OrderableExtractionReport (val value: ExtractionReport) {
    val key: (Int, Weight) = (
      value . extraction . size,
      value . extraction . foldLeft (0: Weight) { (acc, ksw) => Math.min(acc, ksw._2._2) })
  }

  def print(us: UnweightedSet) {
    for ((key, krs) <- us groupBy { _.key }) {
      println()
      println(key)
      println()
      krs foreach { kr => println (kr.rule) }
    }
  }

  def print(wm: WeightedMap) {
    for ((key, krws) <- wm groupBy { case (kr, w) => kr.key }) {
      println()
      println(key)
      println()
      krws foreach { case(kr, w) => println (s"${w} -> ${kr.rule}") }
    }
  }
}
