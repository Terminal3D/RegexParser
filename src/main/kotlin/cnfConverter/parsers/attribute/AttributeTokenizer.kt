sealed class AttributeToken {

    abstract val pos: Int

    data class AssignToken(override val pos: Int) : AttributeToken()

    data class AssertEqual(override val pos: Int) : AttributeToken()

    data class AssertNonEqual(override val pos: Int) : AttributeToken()

    data class EqualToken(override val pos: Int) : AttributeToken()

    data class NotEqualToken(override val pos: Int) : AttributeToken()

    data class PlusToken(override val pos: Int) : AttributeToken()

    data class AndToken(override val pos: Int) : AttributeToken()

    data class OrToken(override val pos: Int) : AttributeToken()

    data class LBracketToken(override val pos: Int) : AttributeToken()

    data class RBracketToken(override val pos: Int) : AttributeToken()

    data class GreaterToken(override val pos: Int) : AttributeToken()

    data class SmallerToken(override val pos: Int) : AttributeToken()

    data class IntToken(
        override val pos: Int,
        val value: Int
    ) : AttributeToken()

    data class StringToken(
        override val pos: Int,
        val text: String
    ) : AttributeToken()

    data class TrueToken(override val pos: Int) : AttributeToken()
    data class FalseToken(override val pos: Int) : AttributeToken()

    data class NonTerminalArgToken(
        override val pos: Int,
        val nt: String,
        val num: Int,
        val attrName: String
    ) : AttributeToken()

    data class PositiveLookaheadToken(
        override val pos: Int,
        val nonterminalInsideL: String
    ) : AttributeToken()

    data class NegativeLookaheadToken(
        override val pos: Int,
        val nonterminalInsideL: String
    ) : AttributeToken()
}

object AttributeTokenizer {

    enum class ConstType {
        NT, STRING, NUM
    }

    private var pos = 0
    private var input = ""
    private var lineNum = 0
    private var tokens: MutableList<AttributeToken> = mutableListOf()

    private fun skipWhitespace() {
        while (pos < input.length && input[pos].isWhitespace()) {
            pos++
        }
    }

    private fun makeError(expected: String): Throwable {
        return AttributeParserException(
            pos = pos,
            expected = expected,
            received = input[pos].toString(),
            lineNum = lineNum
        )
    }

    private fun getConstValue(): Pair<String, ConstType> {
        return when {
            input[pos] == '"' -> {
                pos++
                val str = getString()
                if (input[pos] != '"') throw makeError("\"") else {
                    pos++
                    Pair(str, ConstType.STRING)
                }
            }

            input[pos].isDigit() -> {
                Pair(getNum(), ConstType.NUM)
            }

            else -> {
                val hasBracket = input[pos] == '['
                if (hasBracket) pos++
                val nt = getNt()
                if (hasBracket && input[pos] != ']') throw makeError("]") else if (hasBracket) {
                    pos++
                }
                Pair(nt, ConstType.NT)
            }
        }
    }

    private fun getString(): String {
        return buildString {
            while (input[pos] != '"') {
                append(input[pos])
                pos++
            }
        }
    }

    private fun getNum(): String {
        return buildString {
            while (pos < input.length && input[pos].isDigit()) {
                append(input[pos])
                pos++
            }
        }
    }

    private fun getNt(): String {
        return buildString {
            while (pos < input.length && input[pos].isLetterOrDigit()) {
                append(input[pos])
                pos++
            }
        }
    }

    fun tokenize(inputString: String, lineNum: Int): List<AttributeToken> {
        tokens = mutableListOf()
        input = inputString
        pos = 0
        this.lineNum = lineNum
        val len = input.codePointCount(0, input.length)
        while (pos < len) {
            try {
                val symbol1 = input[pos]
                when {
                    symbol1.isWhitespace() -> skipWhitespace()
                    input.startsWith(":=", pos) -> {
                        tokens.add(AttributeToken.AssignToken(pos))
                        pos += 2
                    }

                    input.startsWith("===", pos) -> {
                        tokens.add(AttributeToken.AssertEqual(pos))
                        pos += 3
                    }

                    input.startsWith("!==", pos) -> {
                        tokens.add(AttributeToken.AssertNonEqual(pos))
                        pos += 3
                    }

                    input.startsWith("==", pos) -> {
                        tokens.add(AttributeToken.EqualToken(pos))
                        pos += 2
                    }

                    input.startsWith("!=", pos) -> {
                        tokens.add(AttributeToken.NotEqualToken(pos))
                        pos += 2
                    }

                    input.startsWith("&&", pos) -> {
                        tokens.add(AttributeToken.AndToken(pos))
                        pos += 2
                    }

                    input.startsWith("||", pos) -> {
                        tokens.add(AttributeToken.OrToken(pos))
                        pos += 2
                    }

                    input.startsWith("+", pos) -> {
                        tokens.add(AttributeToken.PlusToken(pos))
                        pos += 1
                    }

                    input.startsWith("(", pos) -> {
                        tokens.add(AttributeToken.LBracketToken(pos))
                        pos += 1
                    }

                    input.startsWith(")", pos) -> {
                        tokens.add(AttributeToken.RBracketToken(pos))
                        pos += 1
                    }

                    input.startsWith("true", pos) -> {
                        tokens.add(AttributeToken.TrueToken(pos))
                        pos += 4
                    }

                    input.startsWith("false", pos) -> {
                        tokens.add(AttributeToken.FalseToken(pos))
                        pos += 5
                    }

                    input.startsWith(">", pos) -> {
                        tokens.add(AttributeToken.GreaterToken(pos))
                        pos += 1
                    }

                    input.startsWith("<", pos) -> {
                        tokens.add(AttributeToken.SmallerToken(pos))
                        pos += 1
                    }

                    input.startsWith("lookahead") -> {
                        pos += "lookahead".length
                        skipWhitespace()
                        val type = if (input[pos] == '∈') {
                            true
                        } else if (input[pos] == '∉') {
                            false
                        } else {
                            throw makeError("∈ или ∉")
                        }
                        pos++
                        skipWhitespace()
                        if (input.startsWith("L(", pos)) {
                            pos += 2
                            val nt = getConstValue()
                            if (nt.second != ConstType.NT || nt.first.isEmpty()) {
                                throw makeError("нетерминал")
                            }
                            if (input[pos] != ')') throw makeError(")")
                            pos++
                            tokens.add(
                                if (type) {
                                    AttributeToken.PositiveLookaheadToken(
                                        pos = pos,
                                        nonterminalInsideL = nt.first
                                    )
                                } else {
                                    AttributeToken.NegativeLookaheadToken(
                                        pos = pos,
                                        nonterminalInsideL = nt.first
                                    )
                                }
                            )
                        } else {
                            throw makeError("L(")
                        }
                    }

                    else -> {
                        val const = getConstValue()
                        if (const.first.isEmpty()) throw makeError("Строка, число или нетерминал")
                        tokens.add(
                            when (const.second) {
                                ConstType.NT -> {
                                    if (input[pos] != '.') throw makeError(".")
                                    pos++
                                    val num = getNum()
                                    if (num.isEmpty()) throw makeError("число")
                                    if (input[pos] != '.') throw makeError(".")
                                    pos++
                                    val attrName = getNt()
                                    if (attrName.isEmpty()) throw makeError("аттрибут")
                                    AttributeToken.NonTerminalArgToken(
                                        pos,
                                        const.first,
                                        num.toInt(),
                                        attrName
                                    )
                                }

                                ConstType.STRING -> AttributeToken.StringToken(
                                    pos,
                                    const.first
                                )

                                ConstType.NUM -> AttributeToken.IntToken(
                                    pos,
                                    const.first.toInt()
                                )
                            }
                        )
                    }
                }
            } catch (_: IndexOutOfBoundsException) {
                error("Unexpected eol при парсинге атрибутного условия на строке $lineNum")
            }
        }
        return tokens
    }
}

class AttributeParserException(
    pos: Int,
    expected: String,
    received: String,
    lineNum: Int
) : Exception() {
    override val message =
        "На позиции $pos на строке $lineNum в аттрибутной части ожидали $expected, получили $received"
}
