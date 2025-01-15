package org.example

import cnfConverter.parsers.EarleyParser
import cnfConverter.parsers.clearCache
import org.example.cnfConverter.generator.BFSGenerator
import org.example.cnfConverter.models.CfgMapper
import org.example.cnfConverter.parsers.GrammarParser
import org.example.regexConverter.cfgConverter.CfgConverter
import org.example.regexConverter.parser.RegexParser
import org.example.regexConverter.tokenizer.Tokenizer

fun main() {
    while (true) {
        try {
            println("Выберите режим\n1. Атрибутная грамматика\n2. Регулярные выражения")
            val mode = readln().trim().toInt()
            if (mode == 1) {
                grammar()
            } else if (mode == 2) {
                regex()
            }
        } catch (_: Exception) {

        }
    }

}

fun grammar() {
    val cfg = """
    S -> S S ; S.1.a + S.2.a < S.1.b + S.2.b, S.0.a := S.1.a + S.2.a, S.0.b := S.1.b + S.2.b 
    S -> a ; S.0.a := 1, S.0.b := 0
    S -> b ; S.0.b := 1, S.0.a := 0
    """.trimIndent()

    val cfg2 = """
        S -> a S b S ; S.1.val > S.2.val, S.0.val := true
        S -> c ; S.0.val := false
        S -> d ; S.0.val := true
    """.trimIndent()

    val cfg3 = """
        S -> b S b S ; S.1.a1 == S.1.a2 + S.2.a1, S.0.a1 := S.1.a1 + S.2.a1, S.0.a2 := S.1.a1
        S -> a ; S.0.a1 := 1, S.0.a2 := 0
    """.trimIndent()

    val cfg4 = """
        S -> S m S ; S.1.attr >= S.2.attr, S.0.attr := S.1.attr + S.2.attr
        S -> A ; S.0.attr := A.1.attr
        A -> a A ; A.0.attr := A.1.attr + 1
        A -> a ; A.0.attr := 1
    """.trimIndent()
    val parsedCfg = GrammarParser().parse(cfg4)
    println(parsedCfg)
    BFSGenerator(parsedCfg).generateWords(10)
    println("Чтобы выйти из проверки слов введите exit")
    var cmd = readln().trim()
    val earleyParser = EarleyParser(parsedCfg)
    while (cmd != "exit") {
        println(earleyParser.parseAllTrees(cmd, checkAttr = true, clearCache = false))
        cmd = readln().trim()
    }
    clearCache()
}

fun regex() {
    while (true) {
        val inputRegex = readln()
        try {
            val parsed = RegexParser.parse(Tokenizer.tokenize(inputRegex.trim()))
            println("Регулярка корректна")
            val cfg = CfgConverter.convertToCFG(parsed)
            val cfgForParser = CfgMapper.map(cfg.first, cfg.second)
            println(cfgForParser)
            BFSGenerator(cfgForParser).generateWords(10)
            println("Чтобы выйти из проверки слов введите exit")
            var cmd = readln().trim()
            val earleyParser = EarleyParser(cfgForParser)
            while (cmd != "exit") {
                println(earleyParser.parseAllTrees(cmd, checkAttr = true, clearCache = false))
                cmd = readln().trim()
            }
            clearCache()
        } catch (e: Exception) {
            println(e.message)
            continue
        }
    }
}