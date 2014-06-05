package patrickw

import org.scalatra._
import scalate.ScalateSupport
import java.net.URL

class ScraperServlet extends ScraperStack {
  val rm = RuleManager(KnownInputs.twitterProfiles, Parser.TagSoup)

  get("/") {
    <html>
      <body>
        <h1>Instructions</h1>
        <h3>The following paths are active:</h3>
        <ul>
          <li><a href="/extract/scala_lang">/extract/{{username}}</a> returns an extraction for the specified username</li>
          <li><a href="/rules">/rules</a> returns the current ruleset</li>
          <li><a href="/history">/history</a> returns the ruleset history</li>
        </ul>
      </body>
    </html>
  }

  get("/extract/:username") {
    rm.extractAndEvolve(new URL(s"https://twitter.com/${params("username")}"))
  }

  get("/rules") {
    rm.rules
  }

  get("/history") {
    rm.history
  }
}
