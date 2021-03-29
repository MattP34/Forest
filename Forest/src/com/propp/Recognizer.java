package com.propp;

import java.util.ArrayList;

import static com.propp.TokenType.*;

public class Recognizer {
    private static final boolean debug = true;

    private ArrayList<Lexeme> lexemes;
    private int nextLexemeIndex;
    private Lexeme currentLexeme;

    public Recognizer(ArrayList<Lexeme> lexemes) {
        this.lexemes = lexemes; //not cloning (make sure this you don't mess with Lexer arraylist again)
        this.nextLexemeIndex = 0;
        advance();
    }

    // utility methods
    private boolean check(TokenType expected) {
        return this.currentLexeme.getType() == expected;
    }

    private boolean checkNext(TokenType expected) {
        if (this.lexemes.size() <= this.nextLexemeIndex) return false;
        return this.lexemes.get(this.nextLexemeIndex).getType() == expected;
    }

    private void consume(TokenType expected) {
        if (check(expected)) {
            advance();
            return;
        }
        Forest.error(currentLexeme, "expected " + expected);
    }

    private void advance() {
        this.currentLexeme = lexemes.get(this.nextLexemeIndex);
        nextLexemeIndex++;
    }

    public void program() {
        if (debug) System.out.println("-- program --");
        while (statementPending()) {
            statement();
        }
    }

    private void statement() {
        if (debug) System.out.println("-- statement --");
        if (returnStatementPending()) returnStatement();
        else if (blockPending()) block();
        else if (controlStatementPending()) controlStatement();
        else if (funcDeclarationPending()) funcDeclaration();
        else {
            expression();
            if (check(ASSIGN)) instantiation(true);
        }
        consume(SEMICOLON);
    }

    private void returnStatement() {
        if (debug) System.out.println("-- return --");
        consume(RETURN);
        expressionList();
    }

    private void expressionList() {
        if (debug) System.out.println("-- expression --");
        if (expressionPending()) {
            expression();
            if (check(COMMA)) {
                consume(COMMA);
                expressionList();
            }
        }
    }

    private void block() {
        if (debug) System.out.println("-- block --");
        consume(O_CURLY);
        while (statementPending()) {
            statement();
        }
        consume(C_CURLY);
    }

    private void controlStatement() {
        if (debug) System.out.println("-- control statement --");
        if (whileStatementPending()) whileStatement();
        else ifStatement();
    }

    private void whileStatement() {
        if (debug) System.out.println("-- while --");
        consume(WHILE);
        consume(O_OPREN);
        expression();
        consume(C_OPREN);
        block();
    }

    private void ifStatement() {
        if (debug) System.out.println("-- if --");
        consume(IF);
        consume(O_OPREN);
        expression();
        consume(C_OPREN);
        block();
    }

    private void funcDeclaration() {
        if (debug) System.out.println("-- func decl --");
        consume(FUNCTION_DECL);
        consume(IDENTIFIER);
        consume(O_OPREN);
        parameterList();
        consume(C_OPREN);
        block();
    }

    private void parameterList() {
        if (debug) System.out.println("-- parameter list --");
        consume(IDENTIFIER);
        if (check(COMMA)) {
            consume(COMMA);
            parameterList();
        }
    }

    private void instantiation(boolean alreadyConsumedExpression) {
        if (debug) System.out.println("-- instantiation --");
        if (!alreadyConsumedExpression) expression();
        consume(ASSIGN);
        consume(IDENTIFIER);
    }

    private void expression() {
        if (debug) System.out.println("-- expression --");
        if (operatorListPending()) {
            operatorList();
            consume(O_OPREN);
            expressionList();
            consume(C_OPREN);
        } else if (binaryOperatorPending()) {
            binaryOperator();
            consume(O_OPREN);
            expression();
            consume(C_OPREN);
        } else if (funcCallPending()) funcCall();
        else primary();
    }

    private void funcCall() {
        if (debug) System.out.println("-- func call --");
        consume(IDENTIFIER);
        consume(O_OPREN);
        expressionList();
        consume(C_OPREN);
    }

    private void primary() {
        if (debug) System.out.println("-- primary --");
        if (literalPending()) literal();
        else {
            variable();
            if (check(O_SQUARE)) arrayAccess(true);
        }
    }

    private void literal() {
        if (debug) System.out.println("-- literal --");
        if (check(BOOLEAN)) consume(BOOLEAN);
        else if (check(INTEGER)) consume(INTEGER);
        else if (check(FLOAT)) consume(FLOAT);
        else if (check(CHARACTER)) consume(CHARACTER);
        else consume(STRING);
    }

    private void variable() { //array access check TODO
        if (debug) System.out.println("-- varaible --");
        consume(IDENTIFIER);
        if (check(UNDERSCORE)) {
            consume(UNDERSCORE);
            variable();
        }
    }

    private void arrayAccess(boolean alreadyConsumedVariable) {
        if (debug) System.out.println("-- arrayAccess --");
        if (!alreadyConsumedVariable) variable();
        consume(O_SQUARE);
        expression();
        consume(C_SQUARE);
        while (check(O_SQUARE)) {
            consume(O_SQUARE);
            expression();
            consume(C_SQUARE);
        }
    }

    private void operatorList() {
        if (debug) System.out.println("-- operator --");
        variadicOperator();
        if (!(check(INTEGER) || check(DOLLAR_SIGN))) return;
        if (check(INTEGER)) {
            consume(INTEGER);
        }
        consume(DOLLAR_SIGN);
        operatorList();
    }

    private void variadicOperator() {
        if (debug) System.out.println("-- variadic operator --");
        if (comparatorPending()) comparator();
        else if (mathOperatorPending()) mathOperator();
        else if (booleanOperatorPending()) booleanOperator();
        else if (check(O_SQUARE)) {
            consume(O_SQUARE);
            consume(C_SQUARE);
        }
    }

    private void binaryOperator() {
        if (debug) System.out.println("-- binary operator --");
        if (check(NOT)) consume(NOT);
        else if (check(EQUAL)) consume(EQUAL);
        else if (check(MINUS)) consume(MINUS);
        else {
            //call error
        }
    }

    private void comparator() {
        if (debug) System.out.println("-- comparator --");
        if (check(GREATER_THAN)) consume(GREATER_THAN);
        else if (check(LESS_THAN)) consume(LESS_THAN);
        else if (check(GREATER_THAN_OR_EQUAL)) consume(GREATER_THAN_OR_EQUAL);
        else if (check(LESS_THAN_OR_EQUAL)) consume(LESS_THAN_OR_EQUAL);
        else if (check(EQUALS)) consume(EQUALS);
        else {
            //call error
        }
    }

    private void mathOperator() {
        if (debug) System.out.println("-- math operator --");
        if (check(PLUS)) consume(PLUS);
        else if (check(MINUS)) consume(MINUS);
        else if (check(TIMES)) consume(TIMES);
        else if (check(DIVIDE)) consume(DIVIDE);
        else if (check(POWER)) consume(POWER);
        else {
            //call error
        }
    }

    private void booleanOperator() {
        if (debug) System.out.println("-- boolean oeprator --");
        if (check(OR)) consume(OR);
        else if (check(AND)) consume(AND);
        else {
            //call error
        }
    }

    //pending functions
    private boolean statementPending() {
        return returnStatementPending() ||
                blockPending() ||
                controlStatementPending() ||
                funcDeclarationPending() ||
                instantiationPending() || //not an issue with pending functions but since inst starts with expression it always assumes instantiation
                expressionPending();

    }

    private boolean returnStatementPending() {
        return check(RETURN);
    }

    private boolean blockPending() {
        return check(O_CURLY);
    }

    private boolean controlStatementPending() {
        return whileStatementPending() || ifStatementPending();
    }

    private boolean funcDeclarationPending() {
        return check(FUNCTION_DECL);
    }

    private boolean instantiationPending() {
        return expressionPending();
    }

    private boolean expressionPending() {
        return operatorListPending() ||
                binaryOperatorPending() ||
                funcCallPending() ||
                primaryCallPending();
    }

    private boolean whileStatementPending() {
        return check(WHILE);
    }

    private boolean ifStatementPending() {
        return check(IF);
    }

    private boolean operatorListPending() {
        return variadicOperatorPending();
    }

    private boolean binaryOperatorPending() {
        return check(NOT) || check(EQUAL) || check(MINUS);
    }

    private boolean funcCallPending() {
        return check(IDENTIFIER) && checkNext(O_OPREN);
    }

    private boolean primaryCallPending() {
        return variablePending() || literalPending();
    }

    private boolean variablePending() {
        return check(IDENTIFIER);
    }

    private boolean literalPending() {
        return check(BOOLEAN) || check(INTEGER) || check(FLOAT) || check(CHARACTER) || check(STRING);
    }

    private boolean variadicOperatorPending() {
        return comparatorPending() ||
                mathOperatorPending() ||
                booleanOperatorPending() ||
                check(O_SQUARE);
    }

    private boolean comparatorPending() {
        return check(GREATER_THAN) || check(LESS_THAN) || check(GREATER_THAN_OR_EQUAL) ||
                check(LESS_THAN_OR_EQUAL) || check(EQUALS);
    }

    private boolean mathOperatorPending() {
        return check(PLUS) || check(MINUS) || check(TIMES) || check(DIVIDE) || check(POWER);
    }

    private boolean booleanOperatorPending() {
        return check(OR) || check(AND);
    }

    private boolean expressionListPending() {
        return expressionPending();
    }

    private boolean elifStatementPending() {
        return check(ELIF);
    }

    private boolean elseStatementPending() {
        return check(ELSE);
    }

    private boolean parameterListPending() {
        return check(IDENTIFIER);
    }

    private boolean variableListPending() {
        return variablePending();
    }
}
