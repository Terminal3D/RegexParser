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
                    append(prod.second.joinToString(", "))
                    append("\n")
                }
            }
        }
    }
}

class EvaluateError(message: String) : Exception(message)

sealed interface ArgumentValue {
    data class IntValue(val value: Int) : ArgumentValue {
        override fun toString() = value.toString()
    }

    data class BooleanValue(val value: Boolean) : ArgumentValue {
        override fun toString() = value.toString()
    }

    data class StringValue(val value: String) : ArgumentValue {
        override fun toString() = value
    }
}

sealed class Argument {
    data class NonTerminalArg(
        val nt: String,
        val ntNum: Int,
        val atrName: String
    ) : Argument() {
        override fun toString(): String = super.toString()
    }

    sealed class LookAhead : Argument() {
        abstract val looka: Symbol

        data class PositiveLookahead(override val looka: Symbol) : LookAhead() {
            override fun toString() = super.toString()
        }
        data class NegativeLookahead(override val looka: Symbol) : LookAhead() {
            override fun toString() = super.toString()
        }

        override fun toString() = super.toString()
    }

    data object TrueArg : Argument() {
        override fun toString(): String = super.toString()
    }

    data object FalseArg : Argument() {
        override fun toString(): String = super.toString()
    }

    data class AndArg(val left: Argument, val right: Argument) : Argument() {
        override fun toString(): String = super.toString()

        companion object {
            fun evaluate(leftVal: ArgumentValue, rightVal: ArgumentValue): ArgumentValue {
                if (leftVal is ArgumentValue.BooleanValue && rightVal is ArgumentValue.BooleanValue) {
                    return ArgumentValue.BooleanValue(leftVal.value && rightVal.value)
                }
                throw EvaluateError(
                    "Невозможно применить '&&' к ${leftVal::class.simpleName} и ${rightVal::class.simpleName}"
                )
            }
        }
    }

    data class OrArg(val left: Argument, val right: Argument) : Argument() {
        override fun toString(): String = super.toString()

        companion object {
            fun evaluate(leftVal: ArgumentValue, rightVal: ArgumentValue): ArgumentValue {
                if (leftVal is ArgumentValue.BooleanValue && rightVal is ArgumentValue.BooleanValue) {
                    return ArgumentValue.BooleanValue(leftVal.value || rightVal.value)
                }
                throw EvaluateError(
                    "Невозможно применить '||' к ${leftVal::class.simpleName} и ${rightVal::class.simpleName}"
                )
            }
        }
    }

    data class IntNumArg(val value: ArgumentValue.IntValue) : Argument() {
        override fun toString(): String = super.toString()
    }

    data class StringArg(val value: ArgumentValue.StringValue) : Argument() {
        override fun toString(): String = super.toString()
    }

    data class EqualResult(val left: Argument, val right: Argument) : Argument() {
        override fun toString(): String = super.toString()

        companion object {
            private fun compareValues(leftVal: ArgumentValue, rightVal: ArgumentValue): Boolean {
                return when {
                    leftVal is ArgumentValue.IntValue && rightVal is ArgumentValue.IntValue ->
                        leftVal.value == rightVal.value

                    leftVal is ArgumentValue.BooleanValue && rightVal is ArgumentValue.BooleanValue ->
                        leftVal.value == rightVal.value

                    leftVal is ArgumentValue.StringValue && rightVal is ArgumentValue.StringValue ->
                        leftVal.value == rightVal.value

                    else -> throw EvaluateError(
                        "Невозможно сравнить следующие два типа данных: " +
                            "${leftVal::class.simpleName} и ${rightVal::class.simpleName}"
                    )
                }
            }

            fun evaluate(leftVal: ArgumentValue, rightVal: ArgumentValue): ArgumentValue.BooleanValue {
                val result = compareValues(leftVal, rightVal)
                return ArgumentValue.BooleanValue(result)
            }
        }
    }

    data class NonEqualResult(val left: Argument, val right: Argument) : Argument() {
        override fun toString(): String = super.toString()

        companion object {
            private fun compareValues(leftVal: ArgumentValue, rightVal: ArgumentValue): Boolean {
                return when {
                    leftVal is ArgumentValue.IntValue && rightVal is ArgumentValue.IntValue ->
                        leftVal.value != rightVal.value

                    leftVal is ArgumentValue.BooleanValue && rightVal is ArgumentValue.BooleanValue ->
                        leftVal.value != rightVal.value

                    leftVal is ArgumentValue.StringValue && rightVal is ArgumentValue.StringValue ->
                        leftVal.value != rightVal.value

                    else -> throw EvaluateError(
                        "Невозможно сравнить следующие два типа данных: " +
                            "${leftVal::class.simpleName} и ${rightVal::class.simpleName}"
                    )
                }
            }

            fun evaluate(leftVal: ArgumentValue, rightVal: ArgumentValue): ArgumentValue.BooleanValue {
                val result = compareValues(leftVal, rightVal)
                return ArgumentValue.BooleanValue(result)
            }
        }
    }

    data class Plus(val left: Argument, val right: Argument) : Argument() {
        override fun toString(): String = super.toString()
        fun evaluate(leftVal: ArgumentValue, rightVal: ArgumentValue): ArgumentValue {
            return when {
                leftVal is ArgumentValue.IntValue && rightVal is ArgumentValue.IntValue -> {
                    ArgumentValue.IntValue(leftVal.value + rightVal.value)
                }

                leftVal is ArgumentValue.BooleanValue && rightVal is ArgumentValue.BooleanValue -> {
                    ArgumentValue.BooleanValue(leftVal.value.xor(rightVal.value))
                }

                leftVal is ArgumentValue.StringValue && rightVal is ArgumentValue.StringValue -> {
                    ArgumentValue.StringValue(leftVal.value + rightVal.value)
                }

                else -> throw EvaluateError(
                    "Невозможно применить 'сложение' к ${leftVal::class.simpleName} и ${rightVal::class.simpleName}"
                )
            }
        }
    }

    override fun toString(): String {
        return when (this) {
            is NonTerminalArg -> this.nt + this.ntNum + this.atrName
            is LookAhead -> {
                when (this) {
                    is LookAhead.NegativeLookahead -> "lookahead ∈ L(${looka.value})"
                    is LookAhead.PositiveLookahead -> "lookahead ∉ L(${looka.value})"
                }
            }
            is EqualResult -> "$left == $right"
            is NonEqualResult -> "$left != $right"
            is IntNumArg -> this.value.toString()
            is StringArg -> this.value.toString()
            is AndArg -> "$left && $right"
            is OrArg -> "$left || $right"
            is Plus -> "$left + $right"
            TrueArg -> "true"
            FalseArg -> "false"
        }
    }
}

sealed class Attribute {

    data class Assignment(
        val argument: Argument,
        val value: Argument
    ) : Attribute() {
        override fun toString(): String = super.toString()
    }

    data class CheckEqual(
        val leftArg: Argument,
        val rightArg: Argument,
    ) : Attribute() {
        override fun toString(): String = super.toString()
    }

    data class CheckNonEqual(
        val leftArg: Argument,
        val rightArg: Argument,
    ) : Attribute() {
        override fun toString(): String = super.toString()
    }

    override fun toString(): String {
        return when (this) {
            is Assignment -> "$argument := $value"
            is CheckEqual -> "$leftArg == $rightArg"
            is CheckNonEqual -> "$leftArg != $rightArg"
        }
    }
}