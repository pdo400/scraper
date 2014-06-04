package patrickw

sealed abstract class RuleError

object RuleError {
  case object EmptyPath extends RuleError
  case object NonMatchingText extends RuleError
}
