package org.example.cfgConverter

import org.example.parser.RegexNode

object CfgConverter {

    private val nonTerminals: MutableMap<RegexNode, String> = mutableMapOf()
    private val ntCountMap: MutableMap<Char, Int> = mutableMapOf()
    private val cfg: MutableMap<String, List<List<String>>> = mutableMapOf()
    private val nonTerminalLetters = (('A'..'Z') - 'S').toSet()

    private fun init() {
        nonTerminals.clear()
        ntCountMap.clear()
        cfg.clear()
    }

    fun convertToCFG(regex: RegexNode): String {
        init()
        nonTerminals[regex] = "S"
        convert(regex)
        return getGrammarAsString()
    }

    private fun convert(regex: RegexNode): List<List<String>> {
        val currentNt = getNt(regex)

        if (cfg.containsKey(currentNt)) {
            return cfg[currentNt]!!
        }

        val (rhs, deff) = when (regex) {
            is RegexNode.ConcatNode -> {
                val leftNt = getNt(regex.left)
                val rightNt = getNt(regex.right)

                Pair(
                    listOf(
                        listOf(
                            leftNt,
                            rightNt
                        )
                    )
                ) {
                    convert(regex.left)
                    convert(regex.right)
                }
            }

            is RegexNode.OrNode -> {
                val leftNt = getNt(regex.left)
                val rightNt = getNt(regex.right)

                Pair(
                    listOf(
                        listOf(leftNt),
                        listOf(rightNt)
                    )
                ) {
                    convert(regex.left)
                    convert(regex.right)
                }
            }

            is RegexNode.OptionalNode -> {
                val valueNt = getNt(regex.value)

                Pair(
                    listOf(
                        emptyList(),
                        listOf(valueNt)
                    )
                ) {
                    convert(regex.value)
                }
            }

            is RegexNode.KleeneStarNode -> {
                val valueNt = getNt(regex.value)

                Pair(
                    listOf(
                        emptyList(),
                        listOf(valueNt, currentNt)
                    )
                ) {
                    convert(regex.value)
                }
            }

            is RegexNode.LinkToCatchGroupNode -> {
                val linkedNt = getNt(regex.linkedNode)
                Pair(listOf(listOf(linkedNt))) {}
            }

            is RegexNode.LookAheadNode -> {
                Pair(listOf(emptyList())) {}
            }

            is RegexNode.NewCatchGroupNode -> {
                val valueNt = getNt(regex.value)
                Pair(listOf(listOf(valueNt))) { convert(regex.value) }
            }

            is RegexNode.SymbolNode -> {
                Pair(listOf(listOf(regex.char.toString())), {})
            }

            RegexNode.DummyNode -> throw Exception("Ошибка, недопустимый узел $regex")
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

    fun getGrammarAsString(): String {
        // Если в грамматике нет правила для S, значит ещё не вызывали convertToCFG(...)
        if (!cfg.containsKey("S")) {
            return "Грамматика пуста. Сначала вызовите convertToCFG(...)."
        }

        val lines = mutableListOf<String>()

        // 1. Сначала выводим все альтернативы для S
        val sExpansions = cfg["S"] ?: emptyList()
        sExpansions.forEach { alt ->
            lines.add("S -> " + if (alt.isEmpty()) "ε" else alt.joinToString(" "))
        }

        // 2. Выводим остальные нетерминалы (кроме S)
        for ((nt, expansions) in cfg) {
            if (nt == "S") continue
            expansions.forEach { alt ->
                lines.add("$nt -> " + if (alt.isEmpty()) "ε" else alt.joinToString(" "))
            }
        }

        return lines.joinToString("\n")
    }
}