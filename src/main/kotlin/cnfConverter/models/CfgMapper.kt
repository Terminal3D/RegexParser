package org.example.cnfConverter.models

object CfgMapper {

    private const val T_REGEX = "^[a-z]\$"

    fun map(cfg: Map<String, List<List<String>>>, lookaheadMap: Map<String, Pair<Boolean, String>>): CFG {
        val newCsg: MutableMap<String, List<Pair<List<Symbol>, List<Attribute>>>> = mutableMapOf()
        val terminals = mutableSetOf<Char>()
        cfg.forEach { (key, value) ->

            val prods = value.map { prod ->
                val attributes = lookaheadMap[key]?.let { la ->
                    listOf(
                        Attribute.CheckEqual(
                            leftArg = if (la.first) {
                                Argument.LookAhead.PositiveLookahead(
                                    looka = Symbol(
                                        value = la.second,
                                        type = TokenType.NON_TERMINAL
                                    )
                                )
                            } else {
                                Argument.LookAhead.NegativeLookahead(
                                    looka = Symbol(
                                        value = la.second,
                                        type = TokenType.NON_TERMINAL
                                    )
                                )
                            },
                            rightArg = Argument.TrueArg
                        )
                    )
                } ?: emptyList()
                val production = prod.map { el ->
                    when {
                        (Regex(T_REGEX).matches(el)) -> Symbol(
                            type = TokenType.TERMINAL,
                            value = el
                        ).also { terminals.add(el[0]) }

                        el == "ε" -> Symbol(type = TokenType.EPSILON, value = "ε")
                        else -> Symbol(type = TokenType.NON_TERMINAL, value = el)
                    }
                }
                Pair(production, attributes)
            }
            newCsg[key] = prods
        }
        return CFG(
            grammar = newCsg,
            startSymbol = "S",
            terminals = terminals,
        )
    }
}