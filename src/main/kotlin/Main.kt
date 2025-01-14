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
    // grammar()
    while (true) {
        val inputRegex = readln()
        try {
            val parsed = RegexParser.parse(Tokenizer.tokenize(inputRegex.trim()))
            println("Регулярка корректна")
            val cfg = CfgConverter.convertToCFG(parsed)
            val cfgForParser = CfgMapper.map(cfg.first, cfg.second)
            println(cfgForParser)
            // BFSGenerator(cfgForParser).generateWords(10)
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

fun grammar() {
    val cfg = """
    S -> CG1 CG3 
    A -> a 
    B -> b 
    CG2 -> CC1 
    UN1 -> A 
    UN1 -> CG2 
    CG1 -> UN1 
    RE1 -> UN2 
    CG3 -> UN2 
    CC1 -> B B 
    UN2 -> A 
    UN2 -> RE1 
    """.trimIndent()
    val parsedCfg = GrammarParser().parse(cfg)
    println(parsedCfg)
    BFSGenerator(parsedCfg).generateWords(10)
}
