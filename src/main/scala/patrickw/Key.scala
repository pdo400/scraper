package patrickw

sealed abstract class Key

object Key {
  val All: List[Key] = List(Name)

  case object Name extends Key
}
