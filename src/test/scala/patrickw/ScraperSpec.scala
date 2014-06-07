package patrickw

import scala.util.{Try, Success, Failure}

import java.net.URL
import java.util.regex.Pattern
import java.util.Scanner

import org.scalatest._

import scales.utils._
import scales.utils.ScalesUtils._
import scales.xml._
import scales.xml.jaxen._
import scales.xml.ScalesXml._
import scales.xml.Functions._

import Key._

//class ScraperSpecWithNu extends ScraperSpec(Parser.Nu)
//class ScraperSpecWithSax extends ScraperSpec(Parser.Sax)
class ScraperSpecWithTagSoup extends ScraperSpec(Parser.TagSoup)

object ScraperSpec {
  val sampleResource = "camplejohn.html"
  val sampleUrl = getClass.getResource(sampleResource)
  val sampleHtml = new Scanner(getClass.getResourceAsStream(sampleResource), "UTF-8").useDelimiter("""\A""").next()
  val sampleExtraction: Extraction = Map(Name -> "Doug Camplejohn")
  val remoteUrl = new URL("https://twitter.com/camplejohn")

  val useAsKnownCount = 5
  val (knownExtractions, testExtractions) = KnownInputs.twitterProfiles splitAt useAsKnownCount
}

class ScraperSpec(parserPool: Parser.Pool) extends FlatSpec with Matchers {
  import ScraperSpec._

  lazy val sampleDoc = Try { Parser.parse(sampleUrl, parserPool) }
  lazy val sampleRoot = top(sampleDoc.get)
  lazy val unweightedRules = RuleGenerator.generateRules(sampleDoc.get, sampleExtraction)
  lazy val weightedRules = RuleWeighter.weighKnown(unweightedRules)

  "Parser" should "should parse the sample document" in {
    sampleDoc shouldBe a [Success[_]]
  }

  if (sampleDoc.isSuccess) {
    it should "find an html root node" in {
      qname(sampleRoot) should be ("html")
    }

    "RuleGenerator" should "generate a rule for each occurrence of the sample data in the sample document" in {
      val expectedSize =
        sampleExtraction . foldLeft (0) { (acc, next) => acc + (Pattern.quote(next._2).r findAllMatchIn sampleHtml).size }

      unweightedRules.size should be (expectedSize)
    }

    "RuleWeighter" should "transform the generated rules into an equivalent weight map" in {
      weightedRules.size should be (unweightedRules.size)

      for {
        (kr, w) <- weightedRules
      } {
        unweightedRules should contain (kr)
        w should be (RuleWeighter.KnownStartWeight)
      }
    }

    "Extractor" should "extract the sample datum using the sample text rule" in {
      val rules: WeightedMap = Map(
        (Name, Rule(
          "/html[1]/body[1]/div[1]/div[2]/div[1]/div[2]/div[1]/div[1]/div[1]/div[1]/div[1]/div[1]/div[1]/h1[1]/a[1]/child::text()[1]",
          None)) ->
          1
      )

      Extractor.evaluate(rules, sampleRoot).extraction.unweighted.get(Name) should be(Some("Doug Camplejohn"))
    }

    it should "extract the sample datum using the sample attribute rule" in {
      val rules: WeightedMap = Map(
        (Name, Rule(
           "/html[1]/body[1]/div[1]/div[2]/div[1]/div[1]/div[1]/div[1]/div[1]/div[1]/div[1]/a[1]/@title",
          None)) ->
          1
      )

      Extractor.evaluate(rules, sampleRoot).extraction.unweighted.get(Name) should be(Some("Doug Camplejohn"))
    }

    it should "extract the sample datum from the sample document for each generated rule" in {
      for {
        (k, r) <- unweightedRules
        expected = Right[RuleError, String](sampleExtraction(k))
      } {
        Extractor.evaluateRule(r, sampleRoot) should be (expected)
      }
    }

    it should "extract the sample data from the sample document using the weighted rules" in {
      Extractor.evaluate(weightedRules, sampleRoot).extraction.unweighted should be (sampleExtraction)
    }

    "RuleManager" should "correctly extract each remote test document" in {
      val rm = RuleManager(knownExtractions, parserPool)

      for ((url, ex) <- testExtractions)
        rm.extractAndEvolve(url).unweighted should be (ex)
    }

    it should "increase in confidence on a successful extraction" in {
      val rm = RuleManager(knownExtractions, parserPool)

      val (url, _) = testExtractions.head
      val first = rm.extractAndEvolve(url)
      val second = rm.extractAndEvolve(url)

      for ((key, (found1, weight1)) <- first) {
        val (found2, weight2) = second(key)
        found1 should be (found2)
        weight1 should be < weight2
      }
    }
  }
}
