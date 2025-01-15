package org.example

import cnfConverter.parsers.EarleyParser
import cnfConverter.parsers.clearCache
import org.example.cnfConverter.generator.BFSGenerator
import org.example.cnfConverter.models.CfgMapper
import org.example.cnfConverter.parsers.GrammarParser
import org.example.regexConverter.cfgConverter.CfgConverter
import org.example.regexConverter.parser.RegexParser
import org.example.regexConverter.tokenizer.Tokenizer
import java.io.File

fun main() {
    while (true) {
        try {
            println("Выберите режим\n1. КС-грамматика\n2. Регулярные выражения")
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
    while (true) {
        try {
            val parsedCfg = GrammarParser().parse(cfg)
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
        } catch (e: Exception) {
            println(e.message)
            break
        }
    }
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