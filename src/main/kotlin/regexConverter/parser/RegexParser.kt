package org.example.regexConverter.parser

import org.example.regexConverter.tokenizer.Token

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
    private var links: MutableSet<RegexNode> = mutableSetOf()

    private var pos = -1

    private fun peek(): Token? = tokens.getOrNull(pos + 1)

    private fun advance() = pos++

    private fun next(): Token = peek().also { advance() } ?: throw Exception("Неожиданный EOL")

    private fun init(tokens: List<Token>) {
        RegexParser.tokens = tokens
        pos = -1
        inLookAhead = false
        catchGroups.clear()
        links.clear()
    }

    fun parse(tokens: List<Token>): RegexNode {
        init(tokens)
        val node = parseRg()
        if (peek() != null) {
            throw ParseException(next())
        }
        links.forEach { linkNode ->
            (linkNode as RegexNode.LinkToCatchGroupNode).linkedNode =
                catchGroups[linkNode.num] ?: throw Exception("Группы захвата ${linkNode.num} не существует")
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
                val linkNode = RegexNode.LinkToCatchGroupNode(RegexNode.DummyNode, token.number)
                links.add(linkNode)
                linkNode
            }

            is Token.LookAhead -> {
                if (inLookAhead) throw Exception("Нельзя вкладывать lookahead в lookahead")
                inLookAhead = true
                val inside = parseRg()
                assertRightBracket(inside)
                inLookAhead = false
                RegexNode.LookAheadNode(value = RegexNode.NonCatchGroupNode(inside))
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

            is Token.NonCatchGroup -> {
                val inside = parseRg()
                assertRightBracket(inside)
                RegexNode.NonCatchGroupNode(inside)
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