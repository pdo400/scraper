package patrickw

import java.net.URL

import patrickw.Key._

object KnownInputs {
  val twitterProfiles: KnownExtractions = Map(
    (new URL("https://twitter.com/camplejohn"), Map(Name -> "Doug Camplejohn")),
    (new URL("https://twitter.com/pdo400"), Map(Name -> "Patrick Whitesell")),
    (new URL("https://twitter.com/BillyBobThorntn"), Map(Name -> "Billy Bob Thornton")),
    (new URL("https://twitter.com/ladygaga"), Map(Name -> "Lady Gaga")),
    (new URL("https://twitter.com/SFGiants"), Map(Name -> "San Francisco Giants")),
    (new URL("https://twitter.com/49ers"), Map(Name -> "San Francisco 49ers")),
    (new URL("https://twitter.com/WSJ"), Map(Name -> "Wall Street Journal")),
    (new URL("https://twitter.com/linuxfoundation"), Map(Name -> "The Linux Foundation")),
    (new URL("https://twitter.com/Fliptop"), Map(Name -> "Fliptop")),
    (new URL("https://twitter.com/scala_lang"), Map(Name -> "Scala"))
  )
}
