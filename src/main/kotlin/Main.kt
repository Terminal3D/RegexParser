package org.example

import cnfConverter.parsers.EarleyParser
import org.example.cnfConverter.generator.BFSGenerator
import org.example.cnfConverter.models.CfgMapper
import org.example.regexConverter.cfgConverter.CfgConverter
import org.example.regexConverter.parser.RegexParser
import org.example.regexConverter.tokenizer.Tokenizer

fun main() {
    while (true) {
        val inputRegex = readln()
        try {
            val parsed = RegexParser.parse(Tokenizer.tokenize(inputRegex.trim()))
            println("Регулярка корректна")
            val cfg = CfgConverter.convertToCFG(parsed)
            val cfgForParser = CfgMapper.map(cfg.first, cfg.second)
            println(cfgForParser)
            BFSGenerator(cfgForParser).generateWords(3).forEach { println(it) }
            println("Чтобы выйти из проверки слов введите exit")
            var cmd = readln().trim()
            val earleyParser = EarleyParser(cfgForParser)
            while (cmd != "exit") {
                println(earleyParser.parseAllTrees(cmd))
                cmd = readln().trim()
            }
        } catch (e: Exception) {
            println(e.message)
            continue
        }
    }
}