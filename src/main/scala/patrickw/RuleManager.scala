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

  final val MinSnapshotPeriod: Long = 10 * (1000 * 60)
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
  private var snapshots = List.empty[(WeightedMap, Long, Long)]
  private var lastSnapshotTime = Platform.currentTime
  private var nextSnapshotId = 0L
  private var wildRules = future { Set.empty : UnweightedSet }

  private val keys = (knownExtractions . view flatMap { case (url, exs) => exs . keys }) . toSet

  def rules = currentRules

  def history = snapshots

  def extractAndEvolve(url: URL)(implicit ord: Ordering[OrderableExtractionReport]): WeightedExtraction = {
    val doc = Parser.parse(url, parserPool)
    val root = top(doc)

    blocking {
      ruleLock.synchronized {
        val er = Extractor.evaluate(currentRules, root)

        if (acceptAndEvolveCurrent(er, doc))
          er.extraction
        else
          bestFromSnapshots(root)(ord) match {
            case Some((oer, id)) if isAcceptable(oer.value) =>
              touchSnapshot(id)
              oer.value.extraction
            case bs : Option[(OrderableExtractionReport, Long)] =>
              if (Platform.currentTime > lastSnapshotTime + MinSnapshotPeriod) {
                updateSnapshots()
                mergeKnown()

                val er2 = Extractor.evaluate(currentRules, root)

                if (acceptAndEvolveCurrent(er2, doc))
                  er2.extraction
                else {
                  val best = bs.foldLeft(List(er.toOrderable, er2.toOrderable)) { (acc, n) => n._1 :: acc} max ord
                  best.value.extraction
                }
              } else {
                val best = bs.foldLeft(List(er.toOrderable)) { (acc, n) => n._1 :: acc} max ord
                best.value.extraction
              }
          }
      }
    }
  }

  private def buildKnown() =
    RuleWeighter.weighKnown(
      (knownExtractions . view map { case (url, exs) =>
        RuleGenerator.generateRules(Parser.parse(url, parserPool), exs) }).toSeq : _*)

  private def isAcceptable(er: ExtractionReport, w: Weight = MinAcceptanceWeight) =
    keys forall { k => er.extraction get k exists { _._2 >= w } }

  private def acceptAndEvolveCurrent(er: ExtractionReport, doc: Doc) =
    isAcceptable(er) && {
      currentRules = RuleWeighter.weighReport(er)
      addWildRules(doc, er)
      true
    }

  private def bestFromSnapshots(root: XmlPath)(implicit ord: Ordering[OrderableExtractionReport]) =
    if (snapshots.isEmpty)
      None
    else
      Some ( snapshots . view .
        map { case (wm, id, _) => (Extractor.evaluate(wm, root).toOrderable, id) } .
        reduceLeft { (acc, next) => if (ord.compare(acc._1, next._1) >= 0) acc else next } )


  def touchSnapshot(id: Long) =
    snapshots = snapshots . view . map {
      case (snap, `id`, _) => (snap, id, Platform.currentTime)
      case other => other
    } . toList

  private def updateSnapshots(): Unit = {
    val now = Platform.currentTime
    val stale = now - SnapshotStaleTime
    snapshots = snapshots filter { case (_, _, ts) => ts > stale }
    snapshots = (currentRules, nextSnapshotId, now) :: snapshots
    nextSnapshotId = nextSnapshotId + 1
    lastSnapshotTime = now
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
