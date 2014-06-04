name := "scraper"

version := "1.0"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-actors" % scalaVersion.value,
  "org.scala-lang" % "scala-reflect" % scalaVersion.value
)

libraryDependencies += "org.scalatest" %% "scalatest" % "2.1.7" % "test"

libraryDependencies += "nu.validator.htmlparser" % "htmlparser" % "1.4"

libraryDependencies += "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2.1"

libraryDependencies ++= Seq(
  "org.scalesxml" %% "scales-xml" % "0.4.5",
  "org.scalesxml" %% "scales-jaxen" % "0.4.5" intransitive(),
  "jaxen" % "jaxen" % "1.1.3" intransitive())
