package org.example.cnfConverter.models

data class CFG(
    val grammar: Map<String, List<Pair<List<Symbol>, List<Attribute>>>>,
    val startSymbol: String,
    val terminals: Set<Char>,
) {
    override fun toString(): String {
        return buildString {
            append("Начальный символ: ${startSymbol}\n")
            for ((lhs, productions) in grammar) {
                for (prod in productions) {
                    if (prod.first.isEmpty()) append("$lhs -> ε")
                    else append("$lhs -> ${prod.first.joinToString(" ")}")
                    append(" ; ")
                    append(prod.second.joinToString(" && "))
                    append("\n")
                }
            }
        }
    }
}

sealed class Attribute {
    abstract val name: String

    data class Lookahead(
        val nt: String,
        val number: Int,
        val looka: List<Pair<List<Symbol>, List<Attribute>>>,
        override val name: String
    ) : Attribute() {
        override fun toString(): String {
            return "$nt.$number.$name := lookahead ∈ L($nt -> ${looka.first().first.joinToString(" ")})"
        }
    }

    data class Equal(
        val leftParam: EqualParam,
        val rightParam: EqualParam,
        override val name: String
    ) : Attribute() {
        sealed interface EqualParam {
            data class NonTerminalParam(val nt: String, val number: Int) : EqualParam {
                override fun toString(): String {
                    return "$nt.$number"
                }
            }

            data class BooleanParam(val value: Boolean) : EqualParam {
                override fun toString(): String {
                    return value.toString()
                }
            }
        }

        override fun toString(): String {
            val leftString =
                if (leftParam is EqualParam.NonTerminalParam) "${leftParam.nt}.${leftParam.number}.${name}"
                else (leftParam as EqualParam.BooleanParam).value.toString()
            val rightString =
                if (rightParam is EqualParam.NonTerminalParam) "${rightParam.nt}.${rightParam.number}.${name}"
                else (rightParam as EqualParam.BooleanParam).value.toString()
            return "$leftString == $rightString"
        }
    }
}