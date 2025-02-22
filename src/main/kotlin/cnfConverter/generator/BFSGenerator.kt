package org.example.cnfConverter.generator

import cnfConverter.parsers.EarleyParser
import org.example.cnfConverter.models.CFG
import org.example.cnfConverter.models.Symbol
import org.example.cnfConverter.models.TokenType
import java.util.LinkedList

private const val ITERATION_LIMIT = 10000000
private const val FAIL_ITERATION_THRESHOLD = 100000

class BFSGenerator(
    private val cfg: CFG,
) {

    data class QueueItem(
        val word: LinkedList<Symbol>,
        val firstNtPos: Int = 0
    )

    private val generatedWords = mutableSetOf<String>()

    fun generateWords(
        wordsNumber: Int,
        wordLength: Int? = null,
        alwaysPositive: Boolean = true,
        condition: ((String) -> Boolean)? = null
    ): List<String> {
        val earleyParser = EarleyParser(cfg)
        val queue = ArrayDeque<QueueItem>()

        val startWord = LinkedList<Symbol>().apply {
            add(Symbol(TokenType.NON_TERMINAL, cfg.startSymbol))
        }
        queue.add(QueueItem(word = startWord, firstNtPos = 0))
        var failCounter = 0

        while (generatedWords.size < wordsNumber && queue.isNotEmpty()) {
            val item = queue.removeFirst()
            failCounter++
            if (failCounter % FAIL_ITERATION_THRESHOLD == 0) {
                println("Не было найдено ни одного нового слова спустя $failCounter итераций.")
                if (failCounter == ITERATION_LIMIT) {
                    println("Лимит неудачных итераций достигнут")
                    break
                }
            }
            if (item.firstNtPos >= item.word.size) {
                val word = item.word.joinToString(separator = "") { it.value }
                if (earleyParser.parse(word, checkAttr = true, clearCache = false) &&
                    (wordLength == null || item.word.size == wordLength) &&
                    (condition == null || condition(word))) {
                    if (!generatedWords.contains(word)) {
                        println(word)
                        generatedWords.add(word)
                    }
                }
                continue
            }

            val ntSymbol = item.word[item.firstNtPos]
            if (ntSymbol.type != TokenType.NON_TERMINAL) {
                val nextNtPos = findNextNtPos(item.word, item.firstNtPos + 1)
                queue.add(QueueItem(item.word, nextNtPos))
                continue
            }

            val prods = cfg.grammar[ntSymbol.value] ?: continue
            for (prod in prods) {

                val newList = LinkedList(item.word)
                newList.removeAt(item.firstNtPos)

                if (prod.first.isNotEmpty()) {
                    for ((index, sym) in prod.first.withIndex()) {
                        newList.add(item.firstNtPos + index, sym)
                    }
                }

                if (wordLength != null && newList.size > wordLength) {
                    if (!alwaysPositive) {
                        val overflowWord = newList.map { symbol ->
                            if (symbol.type == TokenType.NON_TERMINAL) {
                                Symbol(
                                    type = TokenType.TERMINAL,
                                    value = cfg.terminals.random().toString()
                                )
                            } else symbol
                        }
                        val word = overflowWord.joinToString("") { it.value }
                        if (condition == null || condition(word)) {
                            if (!generatedWords.contains(word)) {
                                println(word)
                                generatedWords.add(word)
                            }
                        }
                    }
                    continue
                }

                val nextNtPos = findNextNtPos(newList, item.firstNtPos)

                queue.add(
                    QueueItem(
                        word = newList,
                        firstNtPos = nextNtPos
                    )
                )
            }
        }

        return generatedWords.toList()
    }

    private fun findNextNtPos(word: List<Symbol>, startIndex: Int): Int {
        for (i in startIndex until word.size) {
            if (word[i].type == TokenType.NON_TERMINAL) {
                return i
            }
        }
        return word.size
    }
}
