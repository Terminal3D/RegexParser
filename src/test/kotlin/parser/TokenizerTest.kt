package parser

import org.example.regexConverter.tokenizer.Token
import org.example.regexConverter.tokenizer.TokenizeException
import org.example.regexConverter.tokenizer.Tokenizer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TokenizerTest {
    @Test
    fun `test basic symbols`() {
        val regex = "abc"
        val tokens = Tokenizer.tokenize(regex)

        assertEquals(3, tokens.size)
        assertEquals(Token.Symbol(0, 'a'), tokens[0])
        assertEquals(Token.Symbol(1, 'b'), tokens[1])
        assertEquals(Token.Symbol(2, 'c'), tokens[2])
    }

    @Test
    fun `test alternation`() {
        val regex = "a|b"
        val tokens = Tokenizer.tokenize(regex)

        assertEquals(3, tokens.size)
        assertEquals(Token.Symbol(0, 'a'), tokens[0])
        assertEquals(Token.OR(1), tokens[1])
        assertEquals(Token.Symbol(2, 'b'), tokens[2])
    }

    @Test
    fun `test Kleene star`() {
        val regex = "a*"
        val tokens = Tokenizer.tokenize(regex)

        assertEquals(2, tokens.size)
        assertEquals(Token.Symbol(0, 'a'), tokens[0])
        assertEquals(Token.KleeneStar(1), tokens[1])
    }

    @Test
    fun `test capture group`() {
        val regex = "(a)"
        val tokens = Tokenizer.tokenize(regex)

        assertEquals(3, tokens.size)
        assert(tokens[0] is Token.NewCatchGroup)
        assertEquals(1, (tokens[0] as Token.NewCatchGroup).number)
        assertEquals(Token.Symbol(1, 'a'), tokens[1])
        assertEquals(Token.RightBracket(2), tokens[2])
    }

    @Test
    fun `test max capture groups exceeded`() {
        val regex = "(a)(b)(c)(d)(e)(f)(g)(h)(i)(j)"

        assertThrows<Exception> {
            Tokenizer.tokenize(regex)
        }.also { exception ->
            assertEquals("Превышено макс. количество групп захвата (9)", exception.message)
        }
    }

    @Test
    fun `test optional group`() {
        val regex = "(?:a)"
        val tokens = Tokenizer.tokenize(regex)

        assertEquals(3, tokens.size)
        assertEquals(Token.NonCatchGroup(2), tokens[0])
        assertEquals(Token.Symbol(3, 'a'), tokens[1])
        assertEquals(Token.RightBracket(4), tokens[2])
    }

    @Test
    fun `test lookahead`() {
        val regex = "(?=a)"
        val tokens = Tokenizer.tokenize(regex)

        assertEquals(3, tokens.size)
        assertEquals(Token.LookAhead(2), tokens[0])
        assertEquals(Token.Symbol(3, 'a'), tokens[1])
        assertEquals(Token.RightBracket(4), tokens[2])
    }

    @Test
    fun `test back reference`() {
        val regex = "(a)(?1)"
        val tokens = Tokenizer.tokenize(regex)

        assertEquals(4, tokens.size)
        assert(tokens[0] is Token.NewCatchGroup)
        assertEquals(Token.RightBracket(2), tokens[2])
        assertEquals(Token.LinkToCatchGroup(5, 1), tokens[3])
    }

    @Test
    fun `test unbalanced brackets`() {
        val regex = "(a"

        assertThrows<Exception> {
            Tokenizer.tokenize(regex)
        }.also { exception ->
            assertEquals("Несбалансированные скобки в выражении", exception.message)
        }
    }

    @Test
    fun `test invalid character`() {
        val regex = "a@b"

        assertThrows<TokenizeException> {
            Tokenizer.tokenize(regex)
        }.also { exception ->
            assertEquals("Неожиданный символ @ на позиции: 1", exception.message)
        }
    }

    @Test
    fun `test complex expression`() {
        val regex = "a(?:b|c)*(d)(?=e)"
        val tokens = Tokenizer.tokenize(regex)

        assertEquals(13, tokens.size)
        assertEquals(Token.Symbol(0, 'a'), tokens[0])
        assertEquals(Token.NonCatchGroup(3), tokens[1])
        assertEquals(Token.Symbol(4, 'b'), tokens[2])
        assertEquals(Token.OR(5), tokens[3])
        assertEquals(Token.Symbol(6, 'c'), tokens[4])
        assertEquals(Token.RightBracket(7), tokens[5])
        assertEquals(Token.KleeneStar(8), tokens[6])
        assert(tokens[7] is Token.NewCatchGroup)
        assertEquals(Token.LookAhead(14), tokens[10])
    }

    @Test
    fun `test unexpected special character after question mark`() {
        val regex = "(?x)"

        assertThrows<TokenizeException> {
            Tokenizer.tokenize(regex)
        }.also { exception ->
            assertEquals("Неожиданный символ x на позиции: 2", exception.message)
        }
    }

    @Test
    fun `test invalid back reference closure`() {
        val regex = "(?x)"

        assertThrows<TokenizeException> {
            Tokenizer.tokenize(regex)
        }.also { exception ->
            assertEquals("Неожиданный символ x на позиции: 2", exception.message)
        }
    }

    @Test
    fun `test refer to uninitialized capture group`() {
        val regex = "(a)(?2)(b)"
        assertThrows<Exception> {
            Tokenizer.tokenize(regex)
        }.also { exception ->
            assertEquals("Группа 2 не была проинициализирована на момент использования на позиции: 5", exception.message)
        }
    }
}