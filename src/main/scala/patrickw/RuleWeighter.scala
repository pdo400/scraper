package patrickw

import scalaz._
import Scalaz._

object RuleWeighter {
  final val KnownStartWeight: Weight = 1
  final val WildStartWeight: Weight = 0.1
  final val MaxWeight: Weight = 100
  final val KnownSuccess: Weight = Math.pow(MaxWeight / KnownStartWeight, 1d / 10)
  final val WildSuccess: Weight = Math.pow(MaxWeight / WildStartWeight, 1d / 100)
  final val WildFailure: Weight = 1 / WildSuccess
  final val StaleWeight: Weight = WildStartWeight * Math.pow(WildFailure, 10)

  final val empty: WeightedMap = Map.empty

  def weighKnown(known: UnweightedSet*): WeightedMap =
    known . foldLeft (empty) {
      (outer, next) => next . foldLeft (outer) {
        (inner, kr) => inner + (kr -> ((inner get kr) cata (w => Math.min(KnownSuccess * w, MaxWeight), KnownStartWeight)))
      }
    }

  def weighReport(er: ExtractionReport): WeightedMap = {
    er.report . foldLeft (empty) {
      (acc, krToRrw) =>
        val (kr, (rr, w)) = krToRrw
        rr match {
          case Right(s) if er.extraction.get(kr.key) == Some(s) =>
            acc + (kr -> Math.min(WildSuccess * w, MaxWeight))
          case _ =>
            val nw = WildFailure * w
            if (nw > StaleWeight) acc + (kr -> nw) else acc
        }
    }
  }

  def merge(l: WeightedMap, r: WeightedMap): WeightedMap = {
    l . foldLeft (r) {
      (acc, krToW) =>
        acc get krToW._1 match {
          case Some(w) if w >= krToW._2 => acc
          case _ => acc + krToW
        }
    }
  }

  def merge(wm: WeightedMap, us: UnweightedSet, w: Weight): WeightedMap =
    us . foldLeft (wm) { (acc, kr) => if (acc contains kr) acc else acc + (kr -> w) }
}
