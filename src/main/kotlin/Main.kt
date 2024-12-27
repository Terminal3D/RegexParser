package org.example

import org.example.cfgConverter.CfgConverter
import org.example.parser.RegexParser
import org.example.tokenizer.Tokenizer

fun main() {
    while (true) {
        val inputRegex = readln()
        try {
            val parsed = RegexParser.parse(Tokenizer.tokenize(inputRegex.trim()))
            println(CfgConverter.convertToCFG(parsed))
        } catch (e: Exception) {
            println(e.message)
            continue
        }
        println("Регулярка корректна")
    }
}