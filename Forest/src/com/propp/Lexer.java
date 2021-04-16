package com.propp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import static com.propp.TokenType.*;

public class Lexer {
    private static final HashMap<String, TokenType> keywords;

    static {
        keywords = new HashMap<String, TokenType>();
        keywords.put("func", FUNCTION_DECL);
        keywords.put("return", RETURN);

        keywords.put("while", WHILE);
        keywords.put("if", IF);
        keywords.put("elif", ELIF);
        keywords.put("else", ELSE);
        keywords.put("true", BOOLEAN);
        keywords.put("false", BOOLEAN);
    }

    private final String source;
    private final ArrayList<Lexeme> lexemes = new ArrayList<Lexeme>();
    private int currentPosition, startOfCurrentLexeme, lineNumber;

    public Lexer(String sourceCode) {
        this.source = sourceCode;
        this.currentPosition = 0;
        this.startOfCurrentLexeme = 1;
        this.lineNumber = 1;
    }

    public ArrayList<Lexeme> lex() throws IOException {
        while (!isAtEnd()) {
            startOfCurrentLexeme = currentPosition;
            Lexeme nextLexeme = this.getNextLexeme();
            if (nextLexeme != null) this.lexemes.add(nextLexeme);
        }
        this.lexemes.add(new Lexeme(FILE_END, this.lineNumber));
        return this.lexemes;
    }

    private Lexeme getNextLexeme() throws IOException {
        char c = this.advance();
        switch (c) {
            //ignore whitespace
            case ' ':
            case '\t':
            case '\n':
            case '\r':
                return null;
            //single character tokens
            case '(':
                return new Lexeme(O_OPREN, this.lineNumber);
            case ')':
                return new Lexeme(C_OPREN, this.lineNumber);
            case '{':
                return new Lexeme(O_CURLY, this.lineNumber);
            case '}':
                return new Lexeme(C_CURLY, this.lineNumber);
            case '[':
                if (match(']')) return new Lexeme(ARRAY_CREATION, this.lineNumber);
                return new Lexeme(O_SQUARE, this.lineNumber);
            case ']':
                return new Lexeme(C_SQUARE, this.lineNumber);
            case '_':
                return new Lexeme(UNDERSCORE, this.lineNumber);
            case ',':
                return new Lexeme(COMMA, this.lineNumber);
            case '$':
                return new Lexeme(DOLLAR_SIGN, this.lineNumber);
            case '+':
                return new Lexeme(PLUS, this.lineNumber);
            case ';':
                return new Lexeme(SEMICOLON, this.lineNumber);
            //one or two character tokens
            case '-':
                return new Lexeme(match('>') ? ASSIGN : MINUS, this.lineNumber); //check
            case '*':
                return new Lexeme(match('*') ? POWER : TIMES, this.lineNumber);
            case '/':
                if (match('/')) return lexComment();
                return new Lexeme(DIVIDE, this.lineNumber);
            case '!':
                return new Lexeme(match('=') ? NOT_EQUAL : NOT, this.lineNumber);
            case '>':
                return new Lexeme(match('=') ? GREATER_THAN_OR_EQUAL : GREATER_THAN, this.lineNumber);
            case '<':
                return new Lexeme(match('=') ? LESS_THAN_OR_EQUAL : LESS_THAN, this.lineNumber);
            //two character tokens
            case '=':
                if (match('=')) return new Lexeme(EQUALS, this.lineNumber);
                return new Lexeme(EQUAL, this.lineNumber);
            case '|':
                if (match('|')) return new Lexeme(OR, this.lineNumber);
                Forest.error(this.lineNumber, "Missing second '|'");
            case '&':
                if (match('&')) return new Lexeme(AND, this.lineNumber);
                Forest.error(this.lineNumber, "Missing second '&'");
            case '"':
                return lexString();
            case '\'':
                return lexChar();
            default:
                if (isDigit(c)) return lexNumber();
                if (isAlpha(c)) return lexIdentifierOrKeyword();
                Forest.error(this.lineNumber, "Unexpected character: " + c);

        }
        return null;
    }

    private char peek() {
        if (this.isAtEnd()) return '\0';
        return source.charAt(currentPosition);
    } //checks the next character to see if its the end of the file, and if not returns the character

    private char peekNext() {
        if (currentPosition + 1 >= source.length()) return '\0';
        return source.charAt(currentPosition + 1);
    }

    private boolean match(char expected) {
        if (isAtEnd() || source.charAt(currentPosition) != expected) return false;
        currentPosition++;
        return true;
    }

    private char advance() {
        char currentChar = source.charAt(currentPosition);
        if (currentChar == '\n' || currentChar == '\r') lineNumber++;
        currentPosition++;
        return currentChar;
    }

    private boolean isAtEnd() {
        return currentPosition >= source.length();
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z');
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private Lexeme lexNumber() throws IOException {
        boolean isInteger = true;
        while (isDigit(peek())) {
            advance();
        }
        //look for fractional part
        if (peek() == '.') {
            //ensure there is digit following the decimal point
            if (!isDigit(peekNext())) Forest.error(this.lineNumber, "Malformed real number (ends in decimal point).");
            isInteger = false;
            // Consume the .
            advance();
            while (isDigit(peek())) {
                advance();
            }
        }
        String numberString = source.substring(this.startOfCurrentLexeme, this.currentPosition);
        if (isInteger) {
            int number = Integer.parseInt(numberString);
            return new Lexeme(INTEGER, number, this.lineNumber);
        }
        double number = Double.parseDouble(numberString);
        return new Lexeme(FLOAT, number, this.lineNumber);
    }

    private Lexeme lexString() throws IOException {
        while (!isAtEnd()) {
            char val = peek();
            String test = val + " ";
            if (peek() == '\"') {
                advance();
                return new Lexeme(STRING, source.substring(this.startOfCurrentLexeme, this.currentPosition), this.lineNumber);
            }
            advance();
        }
        Forest.error(this.lineNumber, "No closing \"");
        return null;
    }

    private Lexeme lexChar() throws IOException {
        if (peekNext() != '\'') Forest.error(this.lineNumber, "Missing closing '");
        char character = peek();
        advance();
        advance();
        return new Lexeme(CHARACTER, character, this.lineNumber);
    }

    private Lexeme lexComment() {
        while (peek() != '\r' && peek() != '\n' && !isAtEnd()) {
            advance();
        }
        return null;
    }

    private Lexeme lexIdentifierOrKeyword() {
        while (isAlphaNumeric(peek())) advance();
        String text = source.substring(this.startOfCurrentLexeme, this.currentPosition);
        //see if the suspected identifier is actually a keyword
        TokenType type = keywords.get(text);
        //if not, it is a user-defiend identifier
        if (type == null) return new Lexeme(IDENTIFIER, text, this.lineNumber);
        if (type == BOOLEAN) {
            if (text.equals("true")) return new Lexeme(type, true, this.lineNumber);
            return new Lexeme(type, false, this.lineNumber);
        }
        return new Lexeme(type, this.lineNumber);
    }
}
