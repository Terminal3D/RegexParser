package org.example.tokenizer

private const val MAX_CATCH_GROUPS = 9

object Tokenizer {

    private var catchGroupCount = 0
    private var pos = -1
    private var regex = ""
    private var bracketsBalance = 0

    private fun init(regex: String) {
        catchGroupCount = 0
        pos = -1
        bracketsBalance = 0
        Tokenizer.regex = regex
    }

    private fun peek(): Char {
        try {
            return regex[pos + 1]
        } catch (e: Exception) {
            throw Exception("Неожиданный EOL")
        }
    }

    private fun advance() = pos++

    private fun next(): Char = peek().also { advance() }

    fun tokenize(regex: String): List<Token> {
        val tokens = mutableListOf<Token>()
        init(regex)
        while (pos < regex.length - 1) {
            val symbol = next()
            when {
                symbol.isLetter() -> tokens.add(Token.Symbol(pos, symbol))
                symbol == '|' -> tokens.add(Token.OR(pos))
                symbol == '*' -> tokens.add(Token.KleeneStar(pos))
                symbol == ')' -> tokens.add(Token.RightBracket(pos)).also { bracketsBalance-- }
                symbol == '(' -> {
                    bracketsBalance++
                    val nextSymbol = peek()

                    if (nextSymbol != '?') {
                        catchGroupCount++
                        if (catchGroupCount > MAX_CATCH_GROUPS) throw Exception("Превышено макс. количество групп захвата ($MAX_CATCH_GROUPS)")
                        tokens.add(Token.NewCatchGroup(pos, catchGroupCount))
                        continue
                    }
                    advance()
                    val next2Symbol = next()
                    when {
                        next2Symbol.isDigit() -> {
                            val groupNum = next2Symbol.digitToInt()
                            if (groupNum > catchGroupCount) {
                                throw Exception("Группа $groupNum не была проинициализирована на момент использования на позиции: $pos")
                            } else if (groupNum == 0) {
                                throw Exception("Используется недопустимый номер (0) при ссылке на выражение на позиции: $pos   ")
                            }
                            tokens.add(
                                Token.LinkToCatchGroup(
                                    pos,
                                    next2Symbol.digitToInt()
                                )
                            )
                            val next3Symbol = next()
                            if (next3Symbol != ')') throw TokenizeException(next3Symbol, pos)
                            bracketsBalance--
                        }

                        next2Symbol == ':' -> tokens.add(Token.Optional(pos))
                        next2Symbol == '=' -> tokens.add(Token.LookAhead(pos))
                        else -> throw TokenizeException(next2Symbol, pos)
                    }
                }

                else -> throw TokenizeException(symbol, pos)
            }
        }
        if (bracketsBalance != 0) throw Exception("Несбалансированные скобки в выражении")
        return tokens
    }
}

class TokenizeException(symbol: Char, pos: Int) : Exception() {
    override val message: String = "Неожиданный символ $symbol на позиции: $pos"
}