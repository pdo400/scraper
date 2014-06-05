package patrickw

import scala.compat.Platform
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

import java.net.URL

import scales.utils._
import scales.xml._
import scales.xml.ScalesXml._

import patrickw.Key._

object RuleManager extends scalaz.Options {
  final val MinAcceptanceWeight: Weight = RuleWeighter.KnownStartWeight * Math.pow(RuleWeighter.KnownSuccess, 5)
  final val MinWildMergeWeight: Weight = RuleWeighter.KnownStartWeight

  final val SnapshotPeriod: Long = 10 * (1000 * 60)
  final val SnapshotStaleTime: Long = 24 * (1000 * 60 * 60)

  def apply(knownExtractions: KnownExtractions, parserPool: Parser.Pool) =
    new RuleManager(knownExtractions, parserPool)
}

class RuleManager(knownExtractions: KnownExtractions, parserPool: Parser.Pool) {
  import RuleManager._

  //
  // This lock serializes rule application and mutation, but extraction time is likely minuscule compared to IO and parsing
  //
  private val ruleLock = new AnyRef()
  private var currentRules = buildKnown()
  private var snapshots = List.empty[(WeightedMap, Long)]
  private var nextSnapshot = Platform.currentTime + SnapshotPeriod
  private var wildRules = future { Set.empty : UnweightedSet }

  private val keys = (knownExtractions . view flatMap { case (url, exs) => exs . keys }) . toSet

  def rules = currentRules

  def history = snapshots

  def extractAndEvolve(url: URL): WeightedExtraction = {
    val doc = Parser.parse(url, parserPool)
    val root = top(doc)

    blocking {
      ruleLock.synchronized {
        if (Platform.currentTime > nextSnapshot) {
          updateSnapshots()
          mergeKnown()
        }

        val er = Extractor.evaluate(currentRules, root)

        if (acceptAndEvolveCurrent(er, doc))
          er.extraction
        else
          bestFromSnapshots(root, er)
      }
    }
  }

  private def buildKnown() =
    RuleWeighter.weighKnown(
      (knownExtractions . view map { case (url, exs) =>
        RuleGenerator.generateRules(Parser.parse(url, parserPool), exs) }).toSeq : _*)

  private def isAcceptable(er: ExtractionReport) =
    keys forall { k => er.extraction get k exists { _._2 >= MinAcceptanceWeight } }

  private def acceptAndEvolveCurrent(er: ExtractionReport, doc: Doc) =
    isAcceptable(er) && {
      currentRules = RuleWeighter.weighReport(er)
      addWildRules(doc, er)
      true
    }

  private def bestFromSnapshots(root: XmlPath, current: ExtractionReport)(implicit ord: Ordering[OrderableExtractionReport]) = {
    val best = snapshots . view . zipWithIndex . foldLeft ((current.toOrderable, Option.empty[Int])) {
      (acc, next) =>
        val ((wm, _), i) = next
        val oex = Extractor.evaluate(wm, root).toOrderable
        if (ord.compare(acc._1, oex) >= 0) acc else (oex, Some(i))
    }

    for (usedIndex <- best . _2)
      snapshots = snapshots . view . zipWithIndex . map {
        case (snap, i) => if (i == usedIndex) (snap._1, Platform.currentTime) else snap
      } . toList

    best . _1 . value . extraction
  }

  private def updateSnapshots(): Unit = {
    val now = Platform.currentTime
    val stale = now - SnapshotStaleTime
    snapshots = snapshots filter { case (wm, ts) => ts > stale }
    snapshots = (currentRules, now) :: snapshots
    nextSnapshot = now + SnapshotPeriod
  }

  private def mergeKnown(): Unit = {
    currentRules = RuleWeighter.merge(currentRules, buildKnown())
  }

  private def addWildRules(doc: Doc, er: ExtractionReport): Unit =
    if (wildRules.isCompleted) {
      wildRules = future { RuleGenerator.generateRules(doc, heavyWild(er)) }
      wildRules onComplete {
        case Success(us) => mergeWild(us)
        case _ =>
      }
    }

  private def heavyWild(er: ExtractionReport) =
    er.extraction . foldLeft (Map.empty: Extraction) {
      (acc, kToSw) =>
        val (k, (s, w)) = kToSw
        if (w >= MinWildMergeWeight) acc + (k -> s) else acc
    }

  private def mergeWild(us: UnweightedSet): Unit =
    blocking {
      ruleLock.synchronized {
        currentRules = RuleWeighter merge(currentRules, us, RuleWeighter.WildStartWeight)
      }
    }

}
