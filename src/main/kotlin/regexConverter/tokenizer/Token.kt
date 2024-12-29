package org.example.regexConverter.tokenizer

sealed class Token(val pos: Int) {
    data class OR(val position: Int) : Token(position)
    data class RightBracket(val position: Int) : Token(position)
    data class Optional(val position: Int) : Token(position)
    data class LookAhead(val position: Int) : Token(position)
    data class NewCatchGroup(val position: Int, val number: Int) : Token(position)
    data class LinkToCatchGroup(val position: Int, val number: Int) : Token(position)
    data class KleeneStar(val position: Int) : Token(position)
    data class Symbol(val position: Int, val value: Char) : Token(position)

    override fun toString(): String = when (this) {
        is OR -> "|"
        is RightBracket -> ")"
        is Optional -> "(?:[rg])"
        is LookAhead -> "(?=[rg])"
        is NewCatchGroup -> "([rg]), номер: $number"
        is LinkToCatchGroup -> "([num]), num: $number"
        is KleeneStar -> "*"
        is Symbol -> "[a-z], значение: $value"
    }
}