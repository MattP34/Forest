package com.propp;

public enum TokenType {
    //one character token
    O_OPREN, C_OPREN,
    O_CURLY, C_CURLY,
    O_SQUARE, C_SQUARE,

    UNDERSCORE,
    COMMA,
    DOLLAR_SIGN,

    PLUS,MINUS,TIMES,DIVIDE,POWER,

    //one or two character tokens
    NOT, NOT_EQUAL, ASSIGN, GREATER_THAN, LESS_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL, EQUALS, EQUAL,

    //two character token
    OR, AND,

    WHILE, IF, ELIF, ELSE,

    FUNCTION_DECL, RETURN,

    STRING, CHARACTER, INTEGER, FLOAT, BOOLEAN,

    IDENTIFIER,

    SEMICOLON,
    FILE_END
}
