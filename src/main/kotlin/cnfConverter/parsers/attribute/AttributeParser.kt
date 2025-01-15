package org.example.cnfConverter.parsers.attribute

import AttributeToken
import org.example.cnfConverter.models.Argument
import org.example.cnfConverter.models.ArgumentValue
import org.example.cnfConverter.models.Attribute
import org.example.cnfConverter.models.Symbol
import org.example.cnfConverter.models.TokenType

/*
Грамматика для атрибутной части
AttrList ::= Attr ( , Attr ) *
Attr ::= Arg := Arg | Arg == Arg | Arg != Arg | Arg > Arg | Arg < Arg
Arg ::= ArgBase | Arg == Arg | Arg != Arg | Arg && Arg | Arg || Arg | Arg + Arg
ArgBase ::= NT | Lookahead | [num] | true | false | [string] | ( Arg )
Lookahead ::= lookahead (∈ | ∉) L( NTBase )
NT ::= NTBase . [num] . AttrName
[num] ::= [0-9]+
[string] ::= "[A-Za-z]+[0-9]*"
AttrName ::= [A-Za-z]+[0-9]*
 */

object AttributeParser {

    private var pos = 0
    private var tokens = mutableListOf<AttributeToken>()

    private fun eol() = pos >= tokens.size

    private fun peek(): AttributeToken {
        if (pos >= tokens.size) {
            throw Exception("Unexpected EOF")
        }
        return tokens[pos]
    }

    private fun next() {
        pos++
    }

    fun parse(tokens: List<AttributeToken>): Attribute {
        this.tokens = tokens.toMutableList()
        pos = 0
        val leftArg = parseArgOr()
        val attr = peek()
        next()
        val rightArg = parseArgOr()
        return when (attr) {
            is AttributeToken.AssignToken -> {
                Attribute.Assignment(
                    argument = leftArg,
                    value = rightArg
                )
            }

            is AttributeToken.AssertEqual -> {
                Attribute.CheckEqual(
                    leftArg = leftArg,
                    rightArg = rightArg
                )
            }

            is AttributeToken.AssertNonEqual -> {
                Attribute.CheckNonEqual(
                    leftArg = leftArg,
                    rightArg = rightArg
                )
            }
            is AttributeToken.GreaterToken -> {
                Attribute.CheckGreater(
                    left = leftArg,
                    right = rightArg
                )
            }
            is AttributeToken.SmallerToken -> {
                Attribute.CheckSmaller(
                    left = leftArg,
                    right = rightArg
                )
            }
            else -> throw Exception("Неожиданный токен $attr на позиции ${attr.pos}")
        }
    }

    private fun parseArgOr(): Argument {
        val left = parseArgAnd()
        val right = if (!eol() && peek() is AttributeToken.OrToken) {
            next()
            parseArgOr()
        } else null
        return if (right != null) {
            Argument.OrArg(
                left,
                right
            )
        } else left
    }

    private fun parseArgAnd(): Argument {
        val left = parseArgEqual()
        val right = if (!eol() && peek() is AttributeToken.AndToken) {
            next()
            parseArgAnd()
        } else null
        return if (right != null) {
            Argument.AndArg(
                left,
                right
            )
        } else left
    }

    private fun parseArgEqual(): Argument {
        val left = parseArgPlus()
        val (right, type) = if (!eol() && peek() is AttributeToken.EqualToken) {
            next()
            Pair(parseArgEqual(), true)
        } else if (!eol() && peek() is AttributeToken.NotEqualToken) {
            next()
            Pair(parseArgEqual(), false)
        } else Pair(null, null)
        return if (right != null) {
            if (type == true) {
                Argument.EqualResult(
                    left,
                    right
                )
            } else {
                Argument.NonEqualResult(
                    left,
                    right
                )
            }
        } else left
    }

    private fun parseArgPlus() : Argument {
        val left = parseArgBase()
        val right = if (!eol() && peek() is AttributeToken.PlusToken) {
            next()
            parseArgPlus()
        } else null
        return if (right != null) {
            Argument.Plus(
                left,
                right
            )
        } else left
    }

    private fun parseArgBase() : Argument {
        return when (val token = peek()) {
            is AttributeToken.LBracketToken -> {
                next()
                val arg = parseArgOr()
                val rBracket = peek()
                if (rBracket !is AttributeToken.RBracketToken) throw Exception("Неожиданный токен $token на позиции ${token.pos}")
                next()
                arg
            }

            is AttributeToken.FalseToken -> {
                next()
                Argument.FalseArg
            }
            is AttributeToken.TrueToken -> {
                next()
                Argument.TrueArg
            }
            is AttributeToken.IntToken -> {
                next()
                Argument.IntNumArg(ArgumentValue.IntValue(token.value))
            }
            is AttributeToken.PositiveLookaheadToken -> {
                next()
                Argument.LookAhead.PositiveLookahead(
                    looka = Symbol(
                        type = TokenType.NON_TERMINAL,
                        value = token.nonterminalInsideL
                    )
                )
            }
            is AttributeToken.NegativeLookaheadToken -> {
                next()
                Argument.LookAhead.NegativeLookahead(
                    looka = Symbol(
                        type = TokenType.NON_TERMINAL,
                        value = token.nonterminalInsideL
                    )
                )
            }
            is AttributeToken.NonTerminalArgToken -> {
                next()
                Argument.NonTerminalArg(
                    nt = token.nt,
                    ntNum = token.num,
                    attrName = token.attrName
                )
            }
            is AttributeToken.StringToken -> {
                next()
                Argument.StringArg(
                    value = ArgumentValue.StringValue(token.text)
                )
            }
            else -> throw Exception("Неожиданный токен $token на позиции ${token.pos}")
        }
    }
}