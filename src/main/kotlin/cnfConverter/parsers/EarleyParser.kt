package cnfConverter.parsers

import org.example.cnfConverter.models.Argument
import org.example.cnfConverter.models.ArgumentValue
import org.example.cnfConverter.models.Attribute
import org.example.cnfConverter.models.CFG
import org.example.cnfConverter.models.Symbol
import org.example.cnfConverter.models.TokenType

data class ParseTree(
    val symbol: String,
    val ruleIndex: Int? = null,
    val children: List<ParseTree> = emptyList()
) {
    override fun toString(): String = buildString {
        append("\n")
        printUtfTree(this, prefix = "", childPrefix = "")
    }

    private fun printUtfTree(
        sb: StringBuilder,
        prefix: String,
        childPrefix: String
    ) {
        sb.append(prefix).append(symbol).append("\n")

        val lastChildIndex = children.size - 1
        for ((index, child) in children.withIndex()) {
            val isLast = (index == lastChildIndex)
            val branchPrefix = if (isLast) "└── " else "├── "
            val nextChildPrefix = if (isLast) "$childPrefix    " else "$childPrefix│   "

            child.printUtfTree(
                sb = sb,
                prefix = childPrefix + branchPrefix,
                childPrefix = nextChildPrefix
            )
        }
    }
}

data class EarleyItem(
    val lhs: String,
    val rhs: List<Symbol>,
    val dot: Int,
    val origin: Int,
    val ruleIndex: Int,
    val backpointers: MutableList<List<ParseTree>> = mutableListOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EarleyItem) return false
        return (lhs == other.lhs &&
            rhs == other.rhs &&
            dot == other.dot &&
            origin == other.origin &&
            ruleIndex == other.ruleIndex)
    }

    override fun hashCode(): Int {
        var result = lhs.hashCode()
        result = 31 * result + rhs.hashCode()
        result = 31 * result + dot
        result = 31 * result + origin
        result = 31 * result + ruleIndex
        return result
    }
}

private val cachedLookaheadChecks: MutableMap<String, MutableMap<String, Boolean>> = mutableMapOf()

fun clearCache() {
    cachedLookaheadChecks.clear()
}

class EarleyParser(private val cfg: CFG) {

    private lateinit var earleySets: Array<MutableSet<EarleyItem>>

    fun parse(input: String, checkAttr: Boolean = true, clearCache: Boolean = true): Boolean =
        parseAllTrees(input, checkAttr, clearCache).isNotEmpty()

    fun parseAllTrees(input: String, checkAttr: Boolean = true, clearCache: Boolean = true): List<ParseTree> {
        if (clearCache) {
            clearCache()
        }

        val tokens: List<Symbol> = input.map { ch ->
            Symbol(type = TokenType.TERMINAL, value = ch.toString())
        }
        val n = tokens.size

        earleySets = Array(n + 1) { mutableSetOf() }

        val startProds = cfg.grammar[cfg.startSymbol].orEmpty()
        for ((ruleIndex, prodPair) in startProds.withIndex()) {
            val (rhsSymbols, _) = prodPair
            val item = EarleyItem(
                lhs = cfg.startSymbol,
                rhs = rhsSymbols,
                dot = 0,
                origin = 0,
                ruleIndex = ruleIndex
            )
            item.backpointers.add(emptyList())
            earleySets[0].add(item)
        }

        closure(0)

        for (i in 0 until n) {
            val currentSetSnapshot = earleySets[i].toList()
            val nextToken = tokens[i]
            for (item in currentSetSnapshot) {
                if (item.dot < item.rhs.size) {
                    val nextSym = item.rhs[item.dot]
                    if (nextSym.type == TokenType.TERMINAL && nextSym.value == nextToken.value) {
                        val advanced = EarleyItem(
                            lhs = item.lhs,
                            rhs = item.rhs,
                            dot = item.dot + 1,
                            origin = item.origin,
                            ruleIndex = item.ruleIndex,
                            backpointers = mutableListOf()
                        )
                        for (bp in item.backpointers) {
                            val newBp = bp + ParseTree(
                                symbol = nextSym.value,
                                ruleIndex = null,
                                children = emptyList()
                            )
                            advanced.backpointers.add(newBp)
                        }
                        earleySets[i + 1].addOrMerge(advanced)
                    }
                }
            }
            closure(i + 1)
        }

        val results = mutableListOf<ParseTree>()
        for (item in earleySets[n]) {
            if (item.lhs == cfg.startSymbol &&
                item.dot == item.rhs.size &&
                item.origin == 0
            ) {
                for (bp in item.backpointers) {
                    results.add(
                        ParseTree(
                            symbol = cfg.startSymbol,
                            ruleIndex = item.ruleIndex,
                            children = bp
                        )
                    )
                }
            }
        }
        return if (checkAttr) {
            results.filter { it.checkAttr(word = input).first }
        } else results
    }

    private fun closure(i: Int) {
        val queue = ArrayDeque(earleySets[i])
        val visited = earleySets[i].toMutableSet()

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()

            if (current.dot < current.rhs.size) {
                val nextSym = current.rhs[current.dot]
                if (nextSym.type == TokenType.NON_TERMINAL) {
                    val nt = nextSym.value
                    val prods = cfg.grammar[nt].orEmpty()
                    for ((ruleIndex, prodPair) in prods.withIndex()) {
                        val (rhsSymbols, _) = prodPair
                        val predicted = EarleyItem(
                            lhs = nt,
                            rhs = rhsSymbols,
                            dot = 0,
                            origin = i,
                            ruleIndex = ruleIndex,
                            backpointers = mutableListOf()
                        )
                        predicted.backpointers.add(emptyList())

                        if (!visited.contains(predicted)) {
                            earleySets[i].add(predicted)
                            queue.add(predicted)
                            visited.add(predicted)
                        }
                    }
                }
            }

            if (current.dot == current.rhs.size) {
                val completedNt = current.lhs
                val originPos = current.origin

                val parents = earleySets[originPos].filter { par ->
                    par.dot < par.rhs.size &&
                        par.rhs[par.dot].type == TokenType.NON_TERMINAL &&
                        par.rhs[par.dot].value == completedNt
                }
                for (parent in parents) {
                    val advanced = EarleyItem(
                        lhs = parent.lhs,
                        rhs = parent.rhs,
                        dot = parent.dot + 1,
                        origin = parent.origin,
                        ruleIndex = parent.ruleIndex,
                        backpointers = mutableListOf()
                    )
                    for (parentBp in parent.backpointers) {
                        for (childBp in current.backpointers) {
                            val newChild = ParseTree(
                                symbol = completedNt,
                                ruleIndex = current.ruleIndex,
                                children = childBp
                            )
                            val newBp = parentBp + newChild
                            advanced.backpointers.add(newBp)
                        }
                    }
                    earleySets[i].addOrMerge(advanced)
                    if (!visited.contains(advanced)) {
                        queue.add(advanced)
                        visited.add(advanced)
                    }
                }
            }
        }
    }

    private fun MutableSet<EarleyItem>.addOrMerge(item: EarleyItem) {
        val existing = this.find { it == item }
        if (existing != null) {
            existing.backpointers.addAll(item.backpointers)
        } else {
            this.add(item)
        }
    }

    private fun ParseTree.checkAttr(
        word: String,
        pos: Int = 0
    ): Triple<Boolean, Int, MutableMap<String, ArgumentValue>> {
        var currentPos = pos

        if (ruleIndex == null) {
            if (currentPos >= word.length) {
                return Triple(false, currentPos, mutableMapOf())
            }
            currentPos++
            return Triple(true, currentPos, mutableMapOf())
        }

        val attributeContext = mutableMapOf<Int, MutableMap<String, ArgumentValue>>()
        var ntIndex = 0
        children.forEach { child ->
            val isNT = child.ruleIndex != null
            if (isNT) ntIndex++
            val (childOk, newPos, childContext) = child.checkAttr(word, currentPos)
            if (!childOk) return Triple(false, newPos, mutableMapOf())
            currentPos = newPos
            if (isNT) {
                attributeContext[ntIndex] = childContext
            }
        }

        val (_, attrs) = cfg.grammar[symbol]!![ruleIndex]

        for (attr in attrs) {
            when (attr) {
                is Attribute.CheckEqual -> {
                    val leftVal = evaluateArgument(attr.leftArg, word, currentPos, attributeContext)
                    val rightVal = evaluateArgument(attr.rightArg, word, currentPos, attributeContext)
                    if (!Attribute.CheckEqual.evaluate(leftVal, rightVal)) {
                        return Triple(false, currentPos, mutableMapOf())
                    }
                }

                is Attribute.CheckNonEqual -> {
                    val leftVal = evaluateArgument(attr.leftArg, word, currentPos, attributeContext)
                    val rightVal = evaluateArgument(attr.rightArg, word, currentPos, attributeContext)
                    if (!Attribute.CheckNonEqual.evaluate(leftVal, rightVal)) {
                        return Triple(false, currentPos, mutableMapOf())
                    }
                }

                is Attribute.Assignment -> {
                    val argument = (attr.argument as? Argument.NonTerminalArg)
                        ?: throw Exception("Присваивать можно только к атрибуту нетерминала")
                    if (attributeContext[argument.ntNum] == null) attributeContext[argument.ntNum] = mutableMapOf()
                    attributeContext[argument.ntNum]!!.put(
                        argument.attrName,
                        evaluateArgument(
                            argument = attr.value,
                            currentPos = currentPos,
                            context = attributeContext,
                            word = word
                        )
                    )
                }

                is Attribute.CheckGreater -> {
                    val leftVal = evaluateArgument(attr.left, word, currentPos, attributeContext)
                    val rightVal = evaluateArgument(attr.right, word, currentPos, attributeContext)
                    if (!Attribute.CheckGreater.evaluate(leftVal, rightVal)) {
                        return Triple(false, currentPos, mutableMapOf())
                    }
                }
                is Attribute.CheckLesser -> {
                    val leftVal = evaluateArgument(attr.left, word, currentPos, attributeContext)
                    val rightVal = evaluateArgument(attr.right, word, currentPos, attributeContext)
                    if (!Attribute.CheckLesser.evaluate(leftVal, rightVal)) {
                        return Triple(false, currentPos, mutableMapOf())
                    }
                }

                is Attribute.CheckGreaterOrEqual -> {
                    val leftVal = evaluateArgument(attr.left, word, currentPos, attributeContext)
                    val rightVal = evaluateArgument(attr.right, word, currentPos, attributeContext)
                    if (!Attribute.CheckGreaterOrEqual.evaluate(leftVal, rightVal)) {
                        return Triple(false, currentPos, mutableMapOf())
                    }
                }
                is Attribute.CheckLesserOrEqual -> {
                    val leftVal = evaluateArgument(attr.left, word, currentPos, attributeContext)
                    val rightVal = evaluateArgument(attr.right, word, currentPos, attributeContext)
                    if (!Attribute.CheckLesserOrEqual.evaluate(leftVal, rightVal)) {
                        return Triple(false, currentPos, mutableMapOf())
                    }
                }
            }
        }
        return Triple(true, currentPos, attributeContext[0] ?: mutableMapOf())
    }

    private fun evaluateArgument(
        argument: Argument,
        word: String,
        currentPos: Int,
        context: MutableMap<Int, MutableMap<String, ArgumentValue>>
    ): ArgumentValue {
        return when (argument) {
            is Argument.NonTerminalArg -> {
                val atrName = argument.attrName
                context[argument.ntNum]?.get(atrName)
                    ?: throw Exception("Атрибут $atrName не проинициализирован на момент обращения у $argument")
            }

            is Argument.EqualResult -> Argument.EqualResult.evaluate(
                leftVal = evaluateArgument(
                    argument = argument.left,
                    word = word,
                    currentPos = currentPos,
                    context = context
                ),
                rightVal = evaluateArgument(
                    argument = argument.right,
                    word = word,
                    currentPos = currentPos,
                    context = context
                )
            )

            is Argument.NonEqualResult -> Argument.NonEqualResult.evaluate(
                leftVal = evaluateArgument(
                    argument = argument.left,
                    word = word,
                    currentPos = currentPos,
                    context = context
                ),
                rightVal = evaluateArgument(
                    argument = argument.right,
                    word = word,
                    currentPos = currentPos,
                    context = context
                )
            )

            is Argument.AndArg -> Argument.AndArg.evaluate(
                leftVal = evaluateArgument(
                    argument = argument.left,
                    word = word,
                    currentPos = currentPos,
                    context = context
                ),
                rightVal = evaluateArgument(
                    argument = argument.right,
                    word = word,
                    currentPos = currentPos,
                    context = context
                )
            )

            is Argument.OrArg -> Argument.OrArg.evaluate(
                leftVal = evaluateArgument(
                    argument = argument.left,
                    word = word,
                    currentPos = currentPos,
                    context = context
                ),
                rightVal = evaluateArgument(
                    argument = argument.right,
                    word = word,
                    currentPos = currentPos,
                    context = context
                )
            )

            is Argument.Plus -> Argument.Plus.evaluate(
                leftVal = evaluateArgument(
                    argument = argument.left,
                    word = word,
                    currentPos = currentPos,
                    context = context
                ),
                rightVal = evaluateArgument(
                    argument = argument.right,
                    word = word,
                    currentPos = currentPos,
                    context = context
                )
            )

            is Argument.IntNumArg -> argument.value
            is Argument.StringArg -> argument.value
            is Argument.BracketsArg -> evaluateArgument(
                argument = argument.value,
                word = word,
                currentPos = currentPos,
                context = context
            )
            Argument.FalseArg -> ArgumentValue.BooleanValue(false)
            Argument.TrueArg -> ArgumentValue.BooleanValue(true)
            is Argument.LookAhead -> {
                val lookAheadNt = argument.looka.value
                val remainder = word.substring(currentPos)
                val newGrammar = cfg.grammar.toMutableMap()
                val matchResult = EarleyParser(
                    CFG(
                        newGrammar,
                        startSymbol = lookAheadNt,
                        terminals = cfg.terminals
                    )
                )
                // Сразу добавляю пустую строку для пустого лукахеда
                val substring = mutableListOf("")
                for (i in remainder.indices) {
                    substring.add(remainder.substring(startIndex = 0, endIndex = i + 1))
                }
                val ok = substring.any {
                    val t = cachedLookaheadChecks
                    if (cachedLookaheadChecks[lookAheadNt] == null) {
                        cachedLookaheadChecks[lookAheadNt] = mutableMapOf()
                    }
                    cachedLookaheadChecks[lookAheadNt]!!.getOrPut(it, {
                        matchResult.parse(it, checkAttr = true, clearCache = false)
                    })
                }
                when (argument) {
                    is Argument.LookAhead.NegativeLookahead -> ArgumentValue.BooleanValue(!ok)
                    is Argument.LookAhead.PositiveLookahead -> ArgumentValue.BooleanValue(ok)
                }
            }
        }
    }
}
