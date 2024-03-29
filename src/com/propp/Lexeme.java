package com.propp;

import static com.propp.TokenType.PARAMETER_LIST;

public class Lexeme {
    private final TokenType type;
    private final int lineNumber;

    public final Lexeme[] arrayValue;
    public final String stringValue;
    public final Integer intValue;
    public final Double doubleValue;
    public final Boolean booleanValue;
    public final Character characterValue;

    private Lexeme left, right;

    public Lexeme(TokenType type, int lineNumber) {
        this.type = type;
        this.lineNumber = lineNumber;
        this.stringValue = null;
        this.intValue = null;
        this.doubleValue = null;
        this.booleanValue = null;
        this.characterValue = null;
        this.arrayValue = null;
        constructerHelper();
    }

    public Lexeme(TokenType type, String stringValue, int lineNumber) {
        this.type = type;
        this.lineNumber = lineNumber;
        this.stringValue = stringValue;
        this.intValue = null;
        this.doubleValue = null;
        this.booleanValue = null;
        this.characterValue = null;
        this.arrayValue = null;
        constructerHelper();
    }

    public Lexeme(TokenType type, int intValue, int lineNumber) {
        this.type = type;
        this.lineNumber = lineNumber;
        this.intValue = Integer.valueOf(intValue);
        this.stringValue = null;
        this.doubleValue = null;
        this.booleanValue = null;
        this.characterValue = null;
        this.arrayValue = null;
        constructerHelper();
    }

    public Lexeme(TokenType type, double doubleValue, int lineNumber) {
        this.type = type;
        this.lineNumber = lineNumber;
        this.doubleValue = Double.valueOf(doubleValue);
        this.stringValue = null;
        this.intValue = null;
        this.booleanValue = null;
        this.characterValue = null;
        this.arrayValue = null;
        constructerHelper();
    }

    public Lexeme(TokenType type, boolean booleanValue, int lineNumber) {
        this.type = type;
        this.lineNumber = lineNumber;
        this.booleanValue = Boolean.valueOf(booleanValue);
        this.stringValue = null;
        this.intValue = null;
        this.doubleValue = null;
        this.characterValue = null;
        this.arrayValue = null;
        constructerHelper();
    }

    public Lexeme(TokenType type, char characterValue, int lineNumber) {
        this.type = type;
        this.lineNumber = lineNumber;
        this.booleanValue = null;
        this.stringValue = null;
        this.intValue = null;
        this.doubleValue = null;
        this.characterValue = Character.valueOf(characterValue);
        this.arrayValue = null;
        constructerHelper();
    }

    public Lexeme(TokenType type, Lexeme[] arrayValue, int lineNumber) {
        this.type = type;
        this.lineNumber = lineNumber;
        this.booleanValue = null;
        this.stringValue = null;
        this.intValue = null;
        this.doubleValue = null;
        this.characterValue = null;
        this.arrayValue = arrayValue;
        constructerHelper();
    }

    private void constructerHelper() {
        this.left = null;
        this.right = null;
    }

    public TokenType getType() {
        return this.type;
    }

    public void setLeft(Lexeme child) {
        this.left = child;
    }

    public void setRight(Lexeme child) {
        this.right = child;
    }

    public Lexeme getLeft() {
        return this.left;
    }

    public Lexeme getRight() {
        return this.right;
    }

    public int getLineNumber() {
        return this.lineNumber;
    }

    public String getValueString() {
        String str = "";
        if (this.stringValue != null) str = this.stringValue;
        if (this.intValue != null) str = this.intValue.toString();
        if (this.doubleValue != null) str = this.doubleValue.toString();
        if (this.booleanValue != null) str = this.booleanValue.toString();
        if (this.characterValue != null) str = this.characterValue.toString();
        return str;
    }

    public String toString() {
        String str = "";
        if (this.stringValue != null) str = this.stringValue;
        if (this.intValue != null) str = this.intValue.toString();
        if (this.doubleValue != null) str = this.doubleValue.toString();
        if (this.booleanValue != null) str = this.booleanValue.toString();
        if (this.characterValue != null) str = this.characterValue.toString();
        return this.type.toString() + ((str.length() > 0) ? ":" : "") + str + " [line " + lineNumber + "]";
    }

    public int getParamExpresListLength() {
        if (this.type != PARAMETER_LIST && this.type != PARAMETER_LIST) return 0;
        Lexeme node = this;
        int counter = 0;
        while (node != null) {
            counter++;
            node = this.getRight();
        }
        return counter;
    }

    @Override
    public int hashCode() { //TODO fix
        if (this.stringValue == null) return 1;
        return (this.stringValue).hashCode() + getParamExpresListLength();
    }

    @Override
    public boolean equals(Object o) {
        if (o.getClass() != Lexeme.class) return false;
        Lexeme lex = (Lexeme) o;
        return lex.type == this.type && lex.stringValue.equals(this.stringValue) && this.getParamExpresListLength() == ((Lexeme) o).getParamExpresListLength();
    }
}
