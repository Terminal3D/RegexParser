package org.example.regexConverter.parser

sealed class RegexNode {

    private val _id: Int = nextId()

    companion object {
        private var counter = 0
        private fun nextId() = counter++
    }

    final override fun equals(other: Any?): Boolean = this === other
    final override fun hashCode(): Int = _id

    class ConcatNode(val left: RegexNode, val right: RegexNode) : RegexNode()
    class OrNode(val left: RegexNode, val right: RegexNode) : RegexNode()
    class NonCatchGroupNode(val value: RegexNode) : RegexNode()
    class KleeneStarNode(val value: RegexNode) : RegexNode()
    class LinkToCatchGroupNode(var linkedNode: RegexNode, val num: Int) : RegexNode()
    class NewCatchGroupNode(var value: RegexNode = DummyNode) : RegexNode()
    class PositiveLookAheadNode(val value: RegexNode) : RegexNode()
    class NegativeLookAheadNode(val value: RegexNode) : RegexNode()
    class SymbolNode(val char: Char) : RegexNode()

    object DummyNode : RegexNode()

    override fun toString(): String {
        return when(this) {
            is ConcatNode -> "concat"
            DummyNode -> "dummy"
            is KleeneStarNode -> "*"
            is LinkToCatchGroupNode -> "link"
            is PositiveLookAheadNode -> "positive lookahead"
            is NewCatchGroupNode -> "new catch group"
            is NonCatchGroupNode -> "non catch group"
            is OrNode -> "|"
            is SymbolNode -> "[a-z]"
            is NegativeLookAheadNode -> "negative lookahead"
        }
    }
}
