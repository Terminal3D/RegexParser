package org.example.parser

import org.example.tokenizer.Token

/*
Грамматика без левой рекурсии
[rg] ::= [rg]' ( | [rg]')*
[rg]' ::= [unary] ([unary])*
[unary] ::= [base] (kleeneStar)*
[base] ::= ([rg]) | (?:[rg]) | (?[num]) | [a − z] | (?=[rg])
[num] ::= [1 − 9]
 */


object RegexParser {

    private var catchGroups: MutableMap<Int, RegexNode> = mutableMapOf()
    private var tokens: List<Token> = emptyList()
    private var inLookAhead: Boolean = false

    private var pos = -1

    private fun peek(): Token? = tokens.getOrNull(pos + 1)

    private fun advance() = pos++

    private fun next(): Token = peek().also { advance() } ?: throw Exception("Неожиданный EOL")

    private fun init(tokens: List<Token>) {
        this.tokens = tokens
        this.pos = -1
        this.inLookAhead = false
        catchGroups.clear()
    }

    fun parse(tokens: List<Token>): RegexNode {
        init(tokens)
        val node = parseRg()
        if (peek() != null) {
            throw ParseException(next())
        }
        return node
    }

    private fun parseRg(): RegexNode {
        var leftNode = parseConcat()
        while (peek() is Token.OR) {
            next()
            val right = parseConcat()
            leftNode = RegexNode.OrNode(leftNode, right)
        }
        return leftNode
    }

    private fun parseConcat(): RegexNode {
        var node = parseUnary()

        while (true) {
            val t = peek()
            if (t == null || t is Token.OR || t is Token.RightBracket) {
                break
            }
            val right = parseUnary()
            node = RegexNode.ConcatNode(node, right)
        }

        return node
    }

    private fun parseUnary(): RegexNode {
        var base = parseBase()

        while (peek() is Token.KleeneStar) {
            next()
            base = RegexNode.KleeneStarNode(base)
        }

        return base
    }

    private fun parseBase(): RegexNode {
        return when (val token = next()) {
            is Token.Symbol -> {
                RegexNode.SymbolNode(token.value)
            }

            is Token.LinkToCatchGroup -> {
                if (inLookAhead) throw Exception("Группа захвата внутри опережающей проверки запрещена")
                val node = catchGroups[token.number]
                    ?: throw Exception("Группа захвата #${token.number} не инициализирована")
                RegexNode.LinkToCatchGroupNode(node)
            }

            is Token.LookAhead -> {
                if (inLookAhead) throw Exception("Нельзя вкладывать lookahead в lookahead")
                inLookAhead = true
                val inside = parseRg()
                assertRightBracket(inside)
                inLookAhead = false
                RegexNode.LookAheadNode(inside)
            }

            is Token.NewCatchGroup -> {
                if (inLookAhead) throw Exception("Группа захвата внутри опережающей проверки")
                val groupNode = RegexNode.NewCatchGroupNode()
                catchGroups[catchGroups.size + 1] = groupNode
                val inside = parseRg()
                groupNode.value = inside
                assertRightBracket(inside)
                groupNode
            }

            is Token.RightBracket -> {
                throw Exception("Неожиданная закрывающая скобка")
            }

            is Token.Optional -> {
                val inside = parseRg()
                assertRightBracket(inside)
                RegexNode.OptionalNode(inside)
            }

            else -> {
                throw ParseException(token)
            }
        }
    }

    private fun assertRightBracket(node: RegexNode): RegexNode {
        val rb = next()
        return if (rb is Token.RightBracket) node else throw ParseException(rb)
    }
}

class ParseException(token: Token) : Exception() {
    override val message = "Неожиданный токен: $token на позиции: ${token.pos}"
}