import sbt._
import Keys._
import org.scalatra.sbt._
import org.scalatra.sbt.PluginKeys._
import com.mojolly.scalate.ScalatePlugin._
import ScalateKeys._

object ScraperBuild extends Build {
  val Organization = "patrickw"
  val Name = "scraper"
  val Version = "1.0"
  val ScalaVersion = "2.10.4"
  val ScalatraVersion = "2.2.2"

  lazy val project = Project (
    "scraper",
    file("."),
    settings = Defaults.defaultSettings ++ ScalatraPlugin.scalatraWithJRebel ++ scalateSettings ++ Seq(
      organization := Organization,
      name := Name,
      version := Version,
      scalaVersion := ScalaVersion,
      resolvers += Classpaths.typesafeReleases,
      libraryDependencies ++= Seq(
        "org.scalatra" %% "scalatra" % ScalatraVersion,
        "org.scalatra" %% "scalatra-scalate" % ScalatraVersion,
        "org.scalatra" %% "scalatra-specs2" % ScalatraVersion % "test",
        "ch.qos.logback" % "logback-classic" % "1.0.6" % "runtime",
        "org.eclipse.jetty" % "jetty-webapp" % "8.1.8.v20121106" % "container",
        "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016" % "container;provided;test" artifacts (Artifact("javax.servlet", "jar", "jar")),

	//
	// patrickw
	//

	//"org.scala-lang" % "scala-actors" % scalaVersion.value,
	//"org.scala-lang" % "scala-reflect" % scalaVersion.value,

	"org.scalatest" %% "scalatest" % "2.1.7" % "test",
	"nu.validator.htmlparser" % "htmlparser" % "1.4",
	"org.ccil.cowan.tagsoup" % "tagsoup" % "1.2.1",
	"org.scalesxml" %% "scales-xml" % "0.4.5",
	"org.scalesxml" %% "scales-jaxen" % "0.4.5" intransitive(),
	"jaxen" % "jaxen" % "1.1.3" intransitive()
      ),
      scalateTemplateConfig in Compile <<= (sourceDirectory in Compile){ base =>
        Seq(
          TemplateConfig(
            base / "webapp" / "WEB-INF" / "templates",
            Seq.empty,  /* default imports should be added here */
            Seq(
              Binding("context", "_root_.org.scalatra.scalate.ScalatraRenderContext", importMembers = true, isImplicit = true)
            ),  /* add extra bindings here */
            Some("templates")
          )
        )
      }
    )
  )
}
