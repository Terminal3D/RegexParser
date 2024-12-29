package org.example.cnfConverter.models

enum class TokenType {
    NON_TERMINAL,
    TERMINAL,
    EPSILON,
}

data class Symbol(val type: TokenType, val value: String) {
    override fun toString(): String {
        return value
    }
}