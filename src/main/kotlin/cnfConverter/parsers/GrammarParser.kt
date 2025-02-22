package org.example.cnfConverter.parsers

import org.example.cnfConverter.models.Attribute
import org.example.cnfConverter.models.CFG
import org.example.cnfConverter.models.Symbol
import org.example.cnfConverter.models.TokenType
import org.example.cnfConverter.parsers.attribute.AttributeParser

class GrammarParser {

    private val terminals = mutableSetOf<Char>()

    companion object {
        const val NT_REGEX = "^([A-Z][0-9]?|\\[[A-Za-z]+[0-9]*\\])\$"
        const val T_REGEX = "^[a-z]\$"
    }

    fun parse(grammar: String): CFG {
        val ruleMap = mutableMapOf<String, MutableList<Pair<List<Symbol>, List<Attribute>>>>()

        var startSymbol = ""

        val lines = grammar.lines().filter { it.trim().isNotEmpty() }

        lines.forEachIndexed { lineNum, line ->
            val (lhs, rhs, attrs) = parseRule(line, lineNum + 1)
            if (startSymbol.isEmpty()) {
                startSymbol = lhs
            }
            ruleMap.getOrPut(lhs) { mutableListOf() }.add(Pair(rhs, attrs))
        }

        return CFG(
            grammar = ruleMap,
            startSymbol = startSymbol,
            terminals = terminals
        )
    }

    private fun parseRule(line: String, lineNum: Int): Triple<String, List<Symbol>, List<Attribute>> {

        val split = line.split(";")
        val grammarPart = split[0]
        val attributePart = split.getOrNull(1) ?: ""
        val attrs = parseAttributePart(attributePart.trim(), lineNum)
        val parsedGrammar = parseGrammarPart(grammarPart, line)
        return Triple(parsedGrammar.first, parsedGrammar.second, attrs)
    }

    private fun parseAttributePart(attributePart: String, lineNum: Int) : List<Attribute> {
        if (attributePart.isEmpty()) return emptyList()
        val attributeParts = attributePart.split(",").map { it.trim() }
        return attributeParts.map { attr ->
            AttributeParser.parse(AttributeTokenizer.tokenize(attr, lineNum))
        }
    }

    private fun parseGrammarPart(grammarPart: String, line: String) : Pair<String, List<Symbol>> {
        val grammarParts = grammarPart.split("->", limit = 2).map { it.trim() }

        if (grammarParts.size != 2) {
            throw IllegalArgumentException("Неверный формат правила (отсутствует '->'): '$line'")
        }

        val lhs = parseNT(grammarParts[0]).value

        val rhs = parseRhs(grammarParts[1])

        return Pair(lhs, rhs)
    }

    private fun parseRhs(rhs: String): List<Symbol> {

        val symbols = mutableListOf<Symbol>()
        var index = 0
        val length = rhs.length

        while (index < length) {
            when {
                rhs[index].isWhitespace() || rhs[index] == 'ε' -> {
                    index++
                }

                rhs[index] == '[' -> {
                    val endBracket = rhs.indexOf(']', index)
                    if (endBracket == -1) {
                        throw IllegalArgumentException("Отсутствует закрывающая ']' для нетерминала в правой части: '$rhs'")
                    }
                    val nt = rhs.substring(index, endBracket + 1)
                    symbols.add(parseNT(nt))
                    index = endBracket + 1
                }

                rhs[index].isUpperCase() -> {
                    val start = index
                    index++
                    while (index < length && (rhs[index].isLetter())) {
                        index++
                    }
                    while (index < length && rhs[index].isDigit()) {
                        index++
                    }
                    val nt = rhs.substring(start, index)
                    symbols.add(parseNT(nt))
                }

                else -> {
                    val t = parseT(rhs, index)
                    symbols.add(t)
                    index += 1
                }
            }
        }

        return symbols
    }

    private fun parseNT(part: String): Symbol {
        // val ntRegex = Regex(NT_REGEX)
        // if (!ntRegex.matches(part)) {
        //     throw IllegalArgumentException("Неверный нетерминал: '$part'")
        // }
        return Symbol(TokenType.NON_TERMINAL, part)
    }

    private fun parseT(rhs: String, index: Int): Symbol {
        val terminalChar = rhs[index]
        terminals.add(terminalChar)
        return Symbol(TokenType.TERMINAL, terminalChar.toString())
    }
}