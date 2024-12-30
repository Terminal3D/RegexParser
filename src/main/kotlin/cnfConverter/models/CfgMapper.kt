package org.example.cnfConverter.models

object CfgMapper {

    private const val T_REGEX = "^[a-z]\$"

    fun map(cfg: Map<String, List<List<String>>>, lookaheadMap: Map<String, List<List<String>>>): CFG {
        val newCsg: MutableMap<String, List<Pair<List<Symbol>, List<Attribute>>>> = mutableMapOf()
        val terminals = mutableSetOf<Char>()
        cfg.forEach { (key, value) ->

            val prods = value.map { prod ->
                val attributes = lookaheadMap[key]?.let { la ->
                    listOf(
                        Attribute.Lookahead(
                            nt = key,
                            number = 0,
                            name = "ok",
                            looka = la.map { prod ->
                                Pair(
                                    prod.map { el ->
                                        when {
                                            (Regex(T_REGEX).matches(el)) -> Symbol(
                                                type = TokenType.TERMINAL,
                                                value = el
                                            ).also { terminals.add(el[0]) }

                                            el == "ε" -> Symbol(type = TokenType.EPSILON, value = "ε")
                                            else -> Symbol(type = TokenType.NON_TERMINAL, value = el)
                                        }
                                    },
                                    emptyList()
                                )
                            }
                        ),
                        Attribute.Equal(
                            leftParam = Attribute.Equal.EqualParam.NonTerminalParam(
                                nt = key,
                                number = 0
                            ),
                            rightParam = Attribute.Equal.EqualParam.BooleanParam(
                                value = true
                            ),
                            name = "ok"
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