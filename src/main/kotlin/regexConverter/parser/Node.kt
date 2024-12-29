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
    class OptionalNode(val value: RegexNode) : RegexNode()
    class KleeneStarNode(val value: RegexNode) : RegexNode()
    class LinkToCatchGroupNode(val linkedNode: RegexNode) : RegexNode()
    class NewCatchGroupNode(var value: RegexNode = DummyNode) : RegexNode()
    class LookAheadNode(val value: RegexNode) : RegexNode()
    class SymbolNode(val char: Char) : RegexNode()

    object DummyNode : RegexNode()

    override fun toString(): String {
        return when(this) {
            is ConcatNode -> "concat"
            DummyNode -> "dummy"
            is KleeneStarNode -> "*"
            is LinkToCatchGroupNode -> "link"
            is LookAheadNode -> "lookahead"
            is NewCatchGroupNode -> "new catch group"
            is OptionalNode -> "?"
            is OrNode -> "|"
            is SymbolNode -> "[a-z]"
        }
    }
}
