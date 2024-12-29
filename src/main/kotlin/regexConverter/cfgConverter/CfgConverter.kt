package org.example.regexConverter.cfgConverter

import org.example.regexConverter.parser.RegexNode

object CfgConverter {

    private val nonTerminals: MutableMap<RegexNode, String> = mutableMapOf()
    private val ntCountMap: MutableMap<Char, Int> = mutableMapOf()
    private val cfg: MutableMap<String, List<List<String>>> = mutableMapOf()
    private val nonTerminalLetters = (('A'..'Z') - 'S').toSet()
    private val lookaheadsMap = mutableMapOf<String, String>()
    private var laCounter = 0
    private fun init() {
        nonTerminals.clear()
        ntCountMap.clear()
        cfg.clear()
        lookaheadsMap.clear()
        laCounter = 0
    }

    fun convertToCFG(regex: RegexNode): Pair<Map<String, List<List<String>>>, Map<String, String>> {
        init()
        nonTerminals[regex] = "S"
        convert(regex)
        return Pair(cfg, lookaheadsMap)
    }

    private fun convert(regexNode: RegexNode): List<List<String>> {
        val currentNt = getNt(regexNode)

        if (cfg.containsKey(currentNt)) {
            return cfg[currentNt]!!
        }

        val (rhs, deff) = when (regexNode) {
            is RegexNode.ConcatNode -> {
                val leftNt = getNt(regexNode.left)
                val rightNt = getNt(regexNode.right)

                Pair(
                    listOf(
                        listOf(
                            leftNt,
                            rightNt
                        )
                    )
                ) {
                    convert(regexNode.left)
                    convert(regexNode.right)
                }
            }

            is RegexNode.OrNode -> {
                val leftNt = getNt(regexNode.left)
                val rightNt = getNt(regexNode.right)

                Pair(
                    listOf(
                        listOf(leftNt),
                        listOf(rightNt)
                    )
                ) {
                    convert(regexNode.left)
                    convert(regexNode.right)
                }
            }

            is RegexNode.OptionalNode -> {
                val valueNt = getNt(regexNode.value)

                Pair(
                    listOf(
                        emptyList(),
                        listOf(valueNt)
                    )
                ) {
                    convert(regexNode.value)
                }
            }

            is RegexNode.KleeneStarNode -> {
                val valueNt = getNt(regexNode.value)

                Pair(
                    listOf(
                        emptyList(),
                        listOf(valueNt, currentNt)
                    )
                ) {
                    convert(regexNode.value)
                }
            }

            is RegexNode.LinkToCatchGroupNode -> {
                val linkedNt = getNt(regexNode.linkedNode)
                Pair(listOf(listOf(linkedNt))) {}
            }

            is RegexNode.LookAheadNode -> {
                val laNt = getLANt(regexNode.value)
                Pair(listOf(listOf(laNt))) {
                }
            }

            is RegexNode.NewCatchGroupNode -> {
                val valueNt = getNt(regexNode.value)
                Pair(listOf(listOf(valueNt))) { convert(regexNode.value) }
            }

            is RegexNode.SymbolNode -> {
                Pair(listOf(listOf(regexNode.char.toString())), {})
            }

            RegexNode.DummyNode -> throw Exception("Ошибка, недопустимый узел $regexNode")
        }

        cfg[currentNt] = rhs
        deff()
        return rhs
    }

    private fun getNt(node: RegexNode): String {
        return nonTerminals[node] ?: run {
            val newNt = generateNewNT()
            nonTerminals[node] = newNt
            newNt
        }
    }

    private fun generateNewNT(): String {
        val ntLetter = nonTerminalLetters.random()
        val count = ntCountMap[ntLetter] ?: 0
        val newNT = if (count == 0) ntLetter.toString() else "$ntLetter$count"
        ntCountMap[ntLetter] = count + 1
        return newNT
    }

    private fun getLANt(node: RegexNode): String {
        return nonTerminals[node] ?: run {
            val newNt = "\$LA$laCounter"
            nonTerminals[node] = newNt
            val reg = toRegexString(node)
            lookaheadsMap[newNt] = reg
            laCounter++
            cfg[newNt] = listOf(listOf())
            return newNt
        }
    }

    private fun toRegexString(node: RegexNode): String {
        return when (node) {
            is RegexNode.ConcatNode -> {
                toRegexString(node.left) + toRegexString(node.right)
            }

            is RegexNode.OrNode -> {
                toRegexString(node.left) + "|" + toRegexString(node.right)
            }

            // для парсинга встроенными реджексами
            is RegexNode.OptionalNode -> {
                "(" + toRegexString(node.value) + ")?"
            }

            is RegexNode.KleeneStarNode -> {
                toRegexString(node.value) + "*"
            }

            is RegexNode.SymbolNode -> {
                node.char.toString()
            }

            else -> throw Exception("Неожиданная нода в lookahead $node")
        }
    }

}