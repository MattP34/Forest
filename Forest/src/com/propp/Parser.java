package com.propp;

import java.util.ArrayList;

import static com.propp.TokenType.*;

public class Parser {
    private static final boolean debug = false;

    private ArrayList<Lexeme> lexemes;
    private int nextLexemeIndex;
    private Lexeme currentLexeme;

    public Parser(ArrayList<Lexeme> lexemes) {
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

    private Lexeme consume(TokenType expected) {
        if (check(expected)) {
            Lexeme consumed = this.currentLexeme;
            advance();
            return consumed;
        }
        Forest.error(currentLexeme, "expected " + expected);
        return null;
    }

    private void advance() {
        this.currentLexeme = lexemes.get(this.nextLexemeIndex);
        nextLexemeIndex++;
    }

    public Lexeme program() {
        if (debug) System.out.println("-- program --");
        Lexeme root = new Lexeme(PROGRAM, 0); //should line number be 0?
        if (statementPending()) {
            root.setLeft(statement());
        }
        if (statementPending()) {
            root.setRight(program());
        }
        return root;
    }

    private Lexeme statement() {
        if (debug) System.out.println("-- statement --");
        Lexeme root;
        if (returnStatementPending()) root = returnStatement();
        else if (blockPending()) root = block();
        else if (controlStatementPending()) root = controlStatement();
        else if (funcDeclarationPending()) root = funcDeclaration();
        else {
            root = expression();
            if (check(ASSIGN)) root = instantiation(root, true);
        }
        consume(SEMICOLON);
        return root;
    }

    private Lexeme returnStatement() {
        if (debug) System.out.println("-- return --");
        Lexeme temp = consume(RETURN);
        Lexeme root = new Lexeme(RETURN, temp.getLineNumber());
        root.setLeft(expressionList());
        return root;
    }

    private Lexeme expressionList() {
        if (debug) System.out.println("-- expression --");
        if (expressionPending()) {
            Lexeme exp = expression();
            Lexeme root = new Lexeme(EXPRESSION_LIST, exp.getLineNumber());
            root.setLeft(exp);
            if (check(COMMA)) {
                consume(COMMA);
                root.setRight(expressionList());
            }
            return root;
        }
        return new Lexeme(EXPRESSION_LIST, 0);
    }

    private Lexeme block() {
        if (debug) System.out.println("-- block --");
        int line = consume(O_CURLY).getLineNumber();
        Lexeme root = statementList(line);
        consume(C_CURLY);
        return root;
    }

    public Lexeme statementList(int lineNumber) {
        Lexeme root = new Lexeme(STATEMENT_LIST, lineNumber);
        if (statementPending()) {
            root.setLeft(statement());
        }
        if (statementPending()) {
            root.setRight(statementList(root.getLeft().getLineNumber()));
        }
        return root;
    }

    private Lexeme controlStatement() {
        if (debug) System.out.println("-- control statement --");
        if (whileStatementPending()) return whileStatement();
        else return ifStatement();
    }

    private Lexeme whileStatement() {
        if (debug) System.out.println("-- while --");
        int line = consume(WHILE).getLineNumber();
        Lexeme root = new Lexeme(WHILE, line);
        consume(O_OPREN);
        root.setLeft(expression());
        consume(C_OPREN);
        root.setRight(block());
        return root;
    }

    private Lexeme ifStatement() {
        if (debug) System.out.println("-- if --");
        int line = consume(IF).getLineNumber();
        Lexeme root = new Lexeme(IF, line);
        consume(O_OPREN);
        root.setLeft(expression());
        line = consume(C_OPREN).getLineNumber();
        root.setRight(new Lexeme(GLUE, line));
        root.getRight().setLeft(block());
        if (elifStatementPending()) root.getRight().setRight(elifStatement());
        else if (elseStatementPending()) root.getRight().setRight(elseStatement());
        return root;
    }

    private Lexeme elifStatement() {
        if (debug) System.out.println("-- elif --");
        int line = consume(ELIF).getLineNumber();
        Lexeme root = new Lexeme(ELIF, line);
        consume(O_OPREN);
        root.setLeft(expression());
        line = consume(C_OPREN).getLineNumber();
        root.setRight(new Lexeme(GLUE, line));
        root.getRight().setLeft(block());
        if (elifStatementPending()) root.getRight().setRight(elifStatement());
        else if (elseStatementPending()) root.getRight().setRight(elseStatement());
        return root;
    }

    private Lexeme elseStatement() {
        if (debug) System.out.println("-- else --");
        int line = consume(ELSE).getLineNumber();
        Lexeme root = new Lexeme(ELSE, line);
        root.setLeft(block());
        return root;
    }

    private Lexeme funcDeclaration() {
        if (debug) System.out.println("-- func decl --");
        int line = consume(FUNCTION_DECL).getLineNumber();
        Lexeme root = new Lexeme(FUNCTION_DECL, line);
        root.setLeft(consume(IDENTIFIER));
        consume(O_OPREN);
        root.getLeft().setLeft(parameterList());
        consume(C_OPREN);
        root.setRight(block());
        return root;
    }

    private Lexeme parameterList() {
        if (debug) System.out.println("-- parameter list --");
        if (!check(IDENTIFIER)) return null;
        Lexeme lex = consume(IDENTIFIER);
        Lexeme root = new Lexeme(PARAMETER_LIST, lex.getLineNumber());
        root.setLeft(lex);
        if (check(COMMA)) {
            consume(COMMA);
            root.setRight(parameterList());
        }
        return root;
    }

    private Lexeme instantiation(Lexeme expressionTemp, boolean alreadyConsumedExpression) {
        if (debug) System.out.println("-- instantiation --");
        if (!alreadyConsumedExpression) expressionTemp = expression();
        Lexeme root = new Lexeme(ASSIGN, expressionTemp.getLineNumber());
        root.setRight(expressionTemp);
        consume(ASSIGN);
        root.setLeft(consume(IDENTIFIER));
        return root;
    }

    private Lexeme expression() {
        if (debug) System.out.println("-- expression --");
        Lexeme root = null;
        if (operatorListPending()) {
            Lexeme opList = operatorList();
            root = new Lexeme(VARIADIC_OPERATION, opList.getLineNumber());
            root.setLeft(opList);
            consume(O_OPREN);
            root.setRight(expressionList());
            consume(C_OPREN);
        } else if (unaryOperatorPending()) {
            Lexeme unOp = unaryOperator();
            root = new Lexeme(UNARY_OPERATION, unOp.getLineNumber());
            root.setLeft(unOp);
            consume(O_OPREN);
            root.setRight(expression());
            consume(C_OPREN);
        } else if (funcCallPending()) return funcCall();
        else return primary();
        return root;
    }

    private Lexeme funcCall() {
        if (debug) System.out.println("-- func call --");
        Lexeme identifier = consume(IDENTIFIER);
        Lexeme root = new Lexeme(FUNC_CALL, identifier.getLineNumber());
        root.setLeft(identifier);
        consume(O_OPREN);
        root.getLeft().setLeft(expressionList());
        consume(C_OPREN);
        return root;
    }

    private Lexeme primary() {
        if (debug) System.out.println("-- primary --");
        if (literalPending()) return literal();
        else {
            Lexeme var = variable();
            if (check(O_SQUARE)) return arrayAccess(var, true);
            else return var;
        }
    }

    private Lexeme literal() {
        if (debug) System.out.println("-- literal --");
        if (check(BOOLEAN)) return consume(BOOLEAN);
        else if (check(INTEGER)) return consume(INTEGER);
        else if (check(FLOAT)) return consume(FLOAT);
        else if (check(CHARACTER)) return consume(CHARACTER);
        else return consume(STRING);
    }

    private Lexeme variable() {
        if (debug) System.out.println("-- varaible --");
        Lexeme identifier = consume(IDENTIFIER);
        Lexeme root = new Lexeme(VARIABLE, identifier.getLineNumber());
        root.setLeft(identifier);
        if (check(UNDERSCORE)) {
            consume(UNDERSCORE);
            root.setRight(variable());
        }
        return root;
    }

    private Lexeme arrayAccess(Lexeme var, boolean alreadyConsumedVariable) {
        if (debug) System.out.println("-- arrayAccess --");
        if (!alreadyConsumedVariable) var = variable();
        Lexeme root = new Lexeme(ARRAY_ACCESS, var.getLineNumber());
        root.setLeft(var);
        int line = consume(O_SQUARE).getLineNumber();
        root.setRight(new Lexeme(GLUE, line));
        root.getRight().setLeft(expression());
        consume(C_SQUARE);
        Lexeme node = root.getRight();
        while (check(O_SQUARE)) {
            line = consume(O_SQUARE).getLineNumber();
            node.setRight(new Lexeme(GLUE, line));
            node.getRight().setLeft(expression());
            consume(C_SQUARE);
            node = node.getRight();
        }
        return root;
    }

    private Lexeme operatorList() {
        if (debug) System.out.println("-- operator --");
        Lexeme op = variadicOperator();
        Lexeme root = new Lexeme(OPERATOR_LIST, op.getLineNumber());
        root.setLeft(op);
        root.setRight(new Lexeme(GLUE, op.getLineNumber()));
        if (!((check(INTEGER) || check(DOLLAR_SIGN)))) {
            return root;
        }
        if (check(INTEGER)) {
            root.getRight().setRight(consume(INTEGER));
        }
        if (check(DOLLAR_SIGN)) {
            consume(DOLLAR_SIGN);
            root.getRight().setLeft(operatorList());
        }
        return root;
    }

    private Lexeme variadicOperator() {
        if (debug) System.out.println("-- variadic operator --");
        if (comparatorPending()) return comparator();
        else if (mathOperatorPending()) return mathOperator();
        else if (booleanOperatorPending()) return booleanOperator();
        else if (check(ARRAY_CREATION)) return consume(ARRAY_CREATION);
        else {
            return null;
            //call error TODO
        }
    }

    private Lexeme unaryOperator() {
        if (debug) System.out.println("-- binary operator --");
        if (check(NOT)) return consume(NOT);
        else if (check(EQUAL)) return consume(EQUAL);
        else if (check(MINUS)) return consume(MINUS);
        else {
            return null;
            //call error TODO
        }
    }

    private Lexeme comparator() {
        if (debug) System.out.println("-- comparator --");
        if (check(GREATER_THAN)) return consume(GREATER_THAN);
        else if (check(LESS_THAN)) return consume(LESS_THAN);
        else if (check(GREATER_THAN_OR_EQUAL)) return consume(GREATER_THAN_OR_EQUAL);
        else if (check(LESS_THAN_OR_EQUAL)) return consume(LESS_THAN_OR_EQUAL);
        else if (check(EQUALS)) return consume(EQUALS);
        else {
            return null;
            //call error TODO
        }
    }

    private Lexeme mathOperator() {
        if (debug) System.out.println("-- math operator --");
        if (check(PLUS)) return consume(PLUS);
        else if (check(MINUS)) return consume(MINUS);
        else if (check(TIMES)) return consume(TIMES);
        else if (check(DIVIDE)) return consume(DIVIDE);
        else if (check(POWER)) return consume(POWER);
        else {
            return null;
            //call error TODO
        }
    }

    private Lexeme booleanOperator() {
        if (debug) System.out.println("-- boolean oeprator --");
        if (check(OR)) return consume(OR);
        else if (check(AND)) return consume(AND);
        else {
            return null;
            //call error TODO
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
                unaryOperatorPending() ||
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

    private boolean unaryOperatorPending() {
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

    public static void printTree(Lexeme root) {
        String printableTree = getPrintableTree(root, 1);
        System.out.println(printableTree);
    }

    private static String getPrintableTree(Lexeme root, int level) {
        String treeString = root.toString();
        System.out.println(treeString);
        StringBuilder spacer = new StringBuilder("\n");
        spacer.append("\t".repeat(level));

        if (root.getLeft() != null)
            treeString += spacer + "with left child: " + getPrintableTree(root.getLeft(), level + 1);
        if (root.getRight() != null)
            treeString += spacer + "and right child: " + getPrintableTree(root.getRight(), level + 1);

        return treeString;
    }
}
