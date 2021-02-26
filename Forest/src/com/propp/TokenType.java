package com.propp;

public enum TokenType {
    //one character token
    O_OPREN, C_OPREN,
    O_CURLY, C_CURLY,
    O_SQUARE, C_SQUARE,

    UNDERSCORE,
    COMMA,
    DOLLAR_SIGN,

    //one or two character tokens
    NOT, NOT_EQUAL,

    //two character token
    GREATER_THAN, LESS_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL, EQUALs,
    OR, AND,

    PLUS,MINUS,TIMES,DIVIDE,POWER,

    WHILE, IF, ELIF, ELSE,

    STRING, CHARACTER, INTEGER, FLOAT, BOOLEAN,

    IDENTIFIER,

    SEMICOLON,
    FILE_END
}
