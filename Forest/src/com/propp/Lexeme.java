package com.propp;

public class Lexeme {
    private final TokenType type;
    private final int lineNumber;

    private final String stringValue;
    private final Integer intValue;
    private final Double doubleValue;
    private final Boolean booleanValue;

    public Lexeme(TokenType type, int lineNumber) {
        this.type = type;
        this.lineNumber = lineNumber;
        this.stringValue = null;
        this.intValue = null;
        this.doubleValue = null;
        this.booleanValue = null;
    }

    public Lexeme(TokenType type, String stringValue, int lineNumber) {
        this.type = type;
        this.lineNumber = lineNumber;
        this.stringValue = stringValue;
        this.intValue = null;
        this.doubleValue = null;
        this.booleanValue = null;
    }

    public Lexeme(TokenType type, int intValue, int lineNumber) {
        this.type = type;
        this.lineNumber = lineNumber;
        this.intValue = Integer.valueOf(intValue);
        this.stringValue = null;
        this.doubleValue = null;
        this.booleanValue = null;
    }

    public Lexeme(TokenType type, double doubleValue, int lineNumber) {
        this.type = type;
        this.lineNumber = lineNumber;
        this.doubleValue = Double.valueOf(doubleValue);
        this.stringValue = null;
        this.intValue = null;
        this.booleanValue = null;
    }

    public Lexeme(TokenType type, boolean booleanValue, int lineNumber) {
        this.type = type;
        this.lineNumber = lineNumber;
        this.booleanValue = Boolean.valueOf(booleanValue);
        this.stringValue = null;
        this.intValue = null;
        this.doubleValue = null;
    }

    public TokenType getType() {
        return this.type;
    }

    public int getLineNumber() {
        return this.lineNumber;
    }

    public String toString() {
        String str = "";
        if(this.stringValue != null) str = this.stringValue;
        if(this.intValue != null) str = this.intValue.toString();
        if(this.doubleValue != null) str = this.doubleValue.toString();
        if(this.booleanValue != null) str = this.booleanValue.toString();
        return this.type.toString() + ((str.length()>0)?":":"") + str + " [line " + lineNumber + "]";
    }
}
