package com.propp;

import java.util.ArrayList;
import java.util.List;

import static com.propp.TokenType.*;

public class Evaluator {
    private int functionCounter;
    private boolean returning;

    public Evaluator() {
        functionCounter = 0;
        returning = false;
    }

    public List<Lexeme> eval(Lexeme tree, Environment environment) {
        ArrayList<Lexeme> valueList = new ArrayList<Lexeme>();
        if (tree == null) return valueList;
        switch (tree.getType()) {
            case PROGRAM:
                combineList(valueList, eval(tree.getLeft(), environment));
                combineList(valueList, eval(tree.getRight(), environment)); //might be null be handled in function call
                break;
            case FUNCTION_DECL:
                return functionDeclaration(tree, environment);
            case FUNC_CALL:
                return functionCall(tree, environment);
            case RETURN:
                return evalReturnStatement(tree, environment);
            case EXPRESSION_LIST:
                return evalExpressionList(tree, environment);
            case VARIADIC_OPERATION:
                return variadicOperation(tree, environment, null);
            case UNARY_OPERATION:
                break; //TODO
            case ARRAY_ACCESS:
                break; //TODO
            case VARIABLE:
                return evalVariable(tree, environment);
            case ASSIGN:
                return assignVariable(tree, environment);
            case ARRAY_CREATION:
                break; //TODO
            case IF:
                break; //TODO
            case ELIF:
                break; //TODO
            case ELSE:
                break; //TODO
            case WHILE:
                return evalWhile(tree, environment);
            case INTEGER:
            case FLOAT:
            case STRING:
            case BOOLEAN:
            case CHARACTER:
                return createSingleList(tree);
            default:
                Forest.error(tree.getLineNumber(), "unsupported Token " + tree.getType());
                break;
        }
        return valueList;
    }

    private static List<Lexeme> createSingleList(Lexeme lex) {
        List<Lexeme> array = new ArrayList<Lexeme>();
        array.add(lex);
        return array;
    }

    private List<Lexeme> combineList(List<Lexeme> valList, List<Lexeme> addList) {
        if (addList != null) {
            for (Lexeme val : addList) {
                valList.add(val);
            }
        }
        return valList;
    }

    private List<Lexeme> functionDeclaration(Lexeme root, Environment environment) {
        environment.addVariable(root.getLeft(), root.getRight());
        return createSingleList(root.getRight());
    }

    private List<Lexeme> functionCall(Lexeme root, Environment environment) {
        if (!environment.variableExists(root.getLeft())) {
            Forest.error(root.getLineNumber(), "function: " + root.getLeft().stringValue + " not declared");
            return null;
        }
        Lexeme statementList = environment.getVariableValue(root.getLeft());
        Lexeme paramListNode = environment.getIdentifier(root.getLeft()).getLeft();
        if (statementList.getType() != STATEMENT_LIST) {
            Forest.error(root.getLineNumber(), root.getLeft().stringValue + " is not a function");
            return null;
        }
        Environment newEnvir = new Environment(environment);
        List<Lexeme> expressionList = eval(root.getLeft().getLeft(), environment);
        List<Lexeme> paramList = new ArrayList<Lexeme>();
        while (paramListNode != null) {
            if (paramListNode.getLeft() != null) paramList.add(paramListNode.getLeft());
            paramListNode = paramListNode.getRight();
        }
        if (expressionList.size() != paramList.size()) {
            Forest.error(root.getLineNumber(), "wrong number of parameters for function " + root.getLeft().stringValue + " expected " + paramList.size() + " but found " + expressionList.size());
            return null;
        }
        for (int i = 0; i < expressionList.size(); i++) {
            newEnvir.addVariable(paramList.get(i), expressionList.get(i));
        }
        functionCounter++;
        List<Lexeme> temp = evalStatementList(statementList, newEnvir);
        returning = false;
        functionCounter--;
        return temp;
    }

    private List<Lexeme> evalStatementList(Lexeme statementList, Environment environment) {
        if (statementList == null) return null;
        if (statementList.getLeft() == null) return null;
        if (statementList.getLeft().getType() == RETURN) {
            if (this.functionCounter == 0) {
                Forest.error(statementList.getLeft().getLineNumber(), " return statement outside of function");
            }
            List<Lexeme> temp = eval(statementList.getLeft(), environment);
            returning = true;
            return temp;
        }
        List<Lexeme> temp = eval(statementList.getLeft(), environment);
        if (returning) return temp;
        return evalStatementList(statementList.getRight(), environment);
    }

    private List<Lexeme> evalReturnStatement(Lexeme root, Environment environment) {
        return eval(root.getLeft(), environment);
    }

    private List<Lexeme> evalExpressionList(Lexeme root, Environment environment) {
        List<Lexeme> expressionsVals = new ArrayList<Lexeme>();
        while (root != null) {
            if (root.getLeft() != null) {
                combineList(expressionsVals, eval(root.getLeft(), environment));
            }
            root = root.getRight();
        }
        return expressionsVals;
    }

    private List<Lexeme> evalVariable(Lexeme root, Environment environment) {
        return createSingleList(environment.getVariableValue(root.getLeft())); //TODO add variable chains
    }

    private List<Lexeme> assignVariable(Lexeme root, Environment environment) {
        List<Lexeme> expressionVals = eval(root.getRight(), environment);
        if (expressionVals.size() == 0) {
            Forest.error(root.getLineNumber(), " missing expression for assignment");
        }
        environment.addVariable(root.getLeft(), expressionVals.get(0));
        return createSingleList(root.getLeft());
    }

    private List<Lexeme> evalWhile(Lexeme root, Environment environment) {
        Environment loopEnvir = new Environment(environment);
        while (eval(root.getLeft(), environment)
    }

    private boolean checkIfReturnsTrue(Lexeme expression, Environment environment) {
        Lexeme root = new Lexeme(VARIADIC_OPERATION, 0);
        root.setLeft(new Lexeme(OPERATOR_LIST, 0));
        root.getLeft().setLeft(new Lexeme(EQUALS, 0));
        root.setRight(new Lexeme(EXPRESSION_LIST, 0));
        root.getRight().setLeft(expression);
        root.getRight().setRight(new Lexeme(EXPRESSION_LIST, 0));
        root.getRight().getRight().setLeft(new Lexeme(BOOLEAN, true, 0));
        List<Lexeme> result = variadicOperation(root, environment, null);
        if (result.size() == 0) {
            Forest.error(expression.getLineNumber(), " found no value in conditional");
            return false;
        }
        if (result.get(0).getType() != BOOLEAN) {
            Forest.error(expression.getLineNumber(), " invalid expression in conditional (did not return type boolean)");
        }
        return result.get(0).booleanValue;
    }

    private List<Lexeme> variadicOperation(Lexeme root, Environment environment, List<Lexeme> expressionValues) {
        Lexeme opList = root.getLeft();
        Lexeme operator = opList.getLeft();
        Lexeme nextOpList = null;
        if (expressionValues == null) {
            Lexeme expressionList = root.getRight();
            expressionValues = evalExpressionList(root.getRight(), environment);
        }
        int numOfExpressions;
        if (opList.getRight().getLeft() != null) { //if there is another operator
            if (opList.getRight().getRight() == null) numOfExpressions = 2; //if there is no specified number
            else numOfExpressions = opList.getRight().getRight().intValue + 1;
            nextOpList = opList.getRight().getLeft();
        } else {
            numOfExpressions = expressionValues.size();
            if (opList.getRight().getRight() != null) {
                if (numOfExpressions != opList.getRight().getRight().intValue + 1) {
                    Forest.error(root.getRight().getLineNumber(), "wrong number of operands for given operation, expected:" + (opList.getRight().getRight().intValue + 1) + " found:" + numOfExpressions);
                }
            }
        }
        //change
        if (expressionValues.size() == 0) {
            Forest.error(root.getLineNumber(), "no expressions found");
            return null;
        }
        Lexeme firstOperand = expressionValues.get(0);
        Lexeme secondOperand;
        for (int i = 1; i < numOfExpressions; i++) {
            if (firstOperand == null || i >= expressionValues.size()) {
                if (firstOperand != null && operator.getType() == MINUS) {
                    switch (firstOperand.getType()) {
                        case INTEGER:
                            firstOperand = new Lexeme(INTEGER, -firstOperand.intValue, firstOperand.getLineNumber());
                            break;
                        case FLOAT:
                            firstOperand = new Lexeme(FLOAT, -firstOperand.doubleValue, firstOperand.getLineNumber());
                            break;
                        case BOOLEAN:
                            firstOperand = new Lexeme(INTEGER, -((firstOperand.getLeft().booleanValue) ? 1 : 0), firstOperand.getLineNumber());
                        case CHARACTER:
                            firstOperand = new Lexeme(INTEGER, -((int) firstOperand.characterValue), firstOperand.getLineNumber());
                            break;
                        default:
                            Forest.error(firstOperand.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType());
                            return null;
                    }
                } else {
                    Forest.error(firstOperand.getLineNumber(), "not enough operands for given operation, expected:" + numOfExpressions + " found:" + (i + 1));
                    return null;
                }
            }
            secondOperand = expressionValues.get(i);
            if (secondOperand == null) {
                Forest.error(firstOperand.getLineNumber(), "not enough operands for given operation, found null value");
                return null;
            }
            switch (operator.getType()) {
                case PLUS:
                    switch (firstOperand.getType()) {
                        case INTEGER:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(INTEGER, firstOperand.intValue + secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(FLOAT, firstOperand.intValue + secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case STRING:
                                    firstOperand = new Lexeme(STRING, firstOperand.intValue + secondOperand.stringValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(INTEGER, firstOperand.intValue + ((secondOperand.booleanValue) ? 1 : 0), root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(INTEGER, firstOperand.intValue + (int) (secondOperand.characterValue), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        case FLOAT:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(FLOAT, firstOperand.doubleValue + secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(FLOAT, firstOperand.doubleValue + secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case STRING:
                                    firstOperand = new Lexeme(STRING, firstOperand.doubleValue + secondOperand.stringValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(FLOAT, firstOperand.doubleValue + ((secondOperand.booleanValue) ? 1 : 0), root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(FLOAT, firstOperand.doubleValue + (int) (secondOperand.characterValue), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        case STRING:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(STRING, firstOperand.stringValue + secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(STRING, firstOperand.stringValue + secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case STRING:
                                    firstOperand = new Lexeme(STRING, firstOperand.stringValue + secondOperand.stringValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(STRING, firstOperand.stringValue + secondOperand.booleanValue, root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(STRING, firstOperand.stringValue + (int) (secondOperand.characterValue), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        case BOOLEAN:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(INTEGER, ((firstOperand.booleanValue) ? 1 : 0) + secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(FLOAT, ((firstOperand.booleanValue) ? 1 : 0) + secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case STRING:
                                    firstOperand = new Lexeme(STRING, firstOperand.booleanValue + secondOperand.stringValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.booleanValue ^ secondOperand.booleanValue, root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(INTEGER, ((firstOperand.booleanValue) ? 1 : 0) + (int) (secondOperand.characterValue), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        case CHARACTER:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(INTEGER, (int) (firstOperand.characterValue) + secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(FLOAT, (int) (firstOperand.characterValue) + secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case STRING:
                                    firstOperand = new Lexeme(STRING, firstOperand.characterValue + secondOperand.stringValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(INTEGER, (int) firstOperand.characterValue + ((firstOperand.booleanValue) ? 1 : 0), root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(INTEGER, (char) ((int) (firstOperand.characterValue) + (int) (secondOperand.characterValue)), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        default:
                            Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                            return null;
                    }
                    break;
                case MINUS:
                    switch (firstOperand.getType()) {
                        case INTEGER:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(INTEGER, firstOperand.intValue - secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(FLOAT, firstOperand.intValue - secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(INTEGER, firstOperand.intValue - ((secondOperand.booleanValue) ? 1 : 0), root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(INTEGER, firstOperand.intValue - (int) (secondOperand.characterValue), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        case FLOAT:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(FLOAT, firstOperand.doubleValue - secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(FLOAT, firstOperand.doubleValue - secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(FLOAT, firstOperand.doubleValue - ((secondOperand.booleanValue) ? 1 : 0), root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(FLOAT, firstOperand.doubleValue - (int) (secondOperand.characterValue), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        case BOOLEAN:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(INTEGER, ((firstOperand.booleanValue) ? 1 : 0) - secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(FLOAT, ((firstOperand.booleanValue) ? 1 : 0) - secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.booleanValue ^ secondOperand.booleanValue, root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(INTEGER, ((firstOperand.booleanValue) ? 1 : 0) - (int) (secondOperand.characterValue), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        case CHARACTER:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(INTEGER, (int) (firstOperand.characterValue) - secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(FLOAT, (int) (firstOperand.characterValue) - secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(INTEGER, (int) firstOperand.characterValue - ((firstOperand.booleanValue) ? 1 : 0), root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(STRING, (char) (byte) ((int) (firstOperand.characterValue) - (int) (secondOperand.characterValue)), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        default:
                            Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                            return null;
                    }
                    break;
                case TIMES:
                    switch (firstOperand.getType()) {
                        case INTEGER:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(INTEGER, firstOperand.intValue * secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(FLOAT, firstOperand.intValue * secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(INTEGER, firstOperand.intValue * ((secondOperand.booleanValue) ? 1 : 0), root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(INTEGER, firstOperand.intValue * (int) (secondOperand.characterValue), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        case FLOAT:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(FLOAT, firstOperand.doubleValue * secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(FLOAT, firstOperand.doubleValue * secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(FLOAT, firstOperand.doubleValue * ((secondOperand.booleanValue) ? 1 : 0), root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(FLOAT, firstOperand.doubleValue * (int) (secondOperand.characterValue), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        case BOOLEAN:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(FLOAT, ((secondOperand.booleanValue) ? 1 : 0) * secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(FLOAT, ((secondOperand.booleanValue) ? 1 : 0) * secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(FLOAT, firstOperand.booleanValue && secondOperand.booleanValue, root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(FLOAT, ((secondOperand.booleanValue) ? 1 : 0) * (int) (secondOperand.characterValue), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        case CHARACTER:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(INTEGER, ((int) firstOperand.characterValue) * secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(FLOAT, ((int) firstOperand.characterValue) * secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(INTEGER, ((int) firstOperand.characterValue) * ((secondOperand.booleanValue) ? 1 : 0), root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(INTEGER, ((int) firstOperand.characterValue) * (int) (secondOperand.characterValue), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        default:
                            Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                            return null;
                    }
                    break;
                case DIVIDE:
                    switch (secondOperand.getType()) {
                        case INTEGER:
                            if (secondOperand.intValue == 0) {
                                Forest.error(root.getLineNumber(), "divide by 0");
                                return null;
                            }
                            break;
                        case FLOAT:
                            if (secondOperand.doubleValue == 0.0) {
                                Forest.error(root.getLineNumber(), "divide by 0");
                                return null;
                            }
                            break;
                        case BOOLEAN:
                            if (secondOperand.booleanValue == false) {
                                Forest.error(root.getLineNumber(), "divide by 0");
                                return null;
                            }
                            break;
                        case CHARACTER:
                            if ((int) secondOperand.characterValue == 0) {
                                Forest.error(root.getLineNumber(), "divide by 0");
                                return null;
                            }
                            break;
                    }
                    switch (firstOperand.getType()) {
                        case INTEGER:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = intDivision(firstOperand, secondOperand);
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(FLOAT, firstOperand.intValue / secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(INTEGER, firstOperand.intValue / ((secondOperand.booleanValue) ? 1 : 0), root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = intDivision(firstOperand, new Lexeme(INTEGER, (int) (secondOperand.characterValue), secondOperand.getLineNumber()));
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        case FLOAT:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(FLOAT, firstOperand.doubleValue / secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(FLOAT, firstOperand.doubleValue / secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(FLOAT, firstOperand.doubleValue / ((secondOperand.booleanValue) ? 1 : 0), root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(FLOAT, firstOperand.doubleValue / (int) (secondOperand.characterValue), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        case BOOLEAN:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = intDivision(new Lexeme(INTEGER, (firstOperand.booleanValue) ? 1 : 0, firstOperand.getLineNumber()), secondOperand);
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(FLOAT, ((firstOperand.booleanValue) ? 1 : 0) / secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    break;
                                case CHARACTER:
                                    firstOperand = intDivision(new Lexeme(INTEGER, (firstOperand.booleanValue) ? 1 : 0, firstOperand.getLineNumber()), new Lexeme(INTEGER, (int) (secondOperand.characterValue), secondOperand.getLineNumber()));
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        case CHARACTER:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(FLOAT, ((int) (firstOperand.characterValue)) / secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(FLOAT, ((int) (firstOperand.characterValue)) / secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(FLOAT, ((int) (firstOperand.characterValue)) / ((secondOperand.booleanValue) ? 1 : 0), root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(FLOAT, ((int) (firstOperand.characterValue)) / (int) (secondOperand.characterValue), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        default:
                            Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                            return null;
                    }
                    break;
                case GREATER_THAN:
                    switch (firstOperand.getType()) {
                        case INTEGER:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.intValue > secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.intValue > secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.intValue > ((secondOperand.booleanValue) ? 1 : 0), root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.intValue > (int) (secondOperand.characterValue), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        case FLOAT:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.doubleValue > secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.doubleValue > secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.doubleValue > ((secondOperand.booleanValue) ? 1 : 0), root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.doubleValue > (int) (secondOperand.characterValue), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        case BOOLEAN:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(BOOLEAN, ((firstOperand.booleanValue) ? 1 : 0) > secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(BOOLEAN, ((firstOperand.booleanValue) ? 1 : 0) > secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(BOOLEAN, ((firstOperand.booleanValue) ? 1 : 0) > ((secondOperand.booleanValue) ? 1 : 0), root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(BOOLEAN, ((firstOperand.booleanValue) ? 1 : 0) > (int) (secondOperand.characterValue), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        case CHARACTER:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(BOOLEAN, ((int) (firstOperand.characterValue)) > secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(BOOLEAN, ((int) (firstOperand.characterValue)) > secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(BOOLEAN, ((int) (firstOperand.characterValue)) > ((secondOperand.booleanValue) ? 1 : 0), root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(BOOLEAN, ((int) (firstOperand.characterValue)) > (int) (secondOperand.characterValue), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        default:
                            Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                            return null;
                    }
                    break;
                case GREATER_THAN_OR_EQUAL:
                    switch (firstOperand.getType()) {
                        case INTEGER:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.intValue >= secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.intValue >= secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.intValue >= ((secondOperand.booleanValue) ? 1 : 0), root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.intValue >= (int) (secondOperand.characterValue), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        case FLOAT:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.doubleValue >= secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.doubleValue >= secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.doubleValue >= ((secondOperand.booleanValue) ? 1 : 0), root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.doubleValue >= (int) (secondOperand.characterValue), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        case BOOLEAN:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(BOOLEAN, ((firstOperand.booleanValue) ? 1 : 0) >= secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(BOOLEAN, ((firstOperand.booleanValue) ? 1 : 0) >= secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(BOOLEAN, ((firstOperand.booleanValue) ? 1 : 0) >= ((secondOperand.booleanValue) ? 1 : 0), root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(BOOLEAN, ((firstOperand.booleanValue) ? 1 : 0) >= (int) (secondOperand.characterValue), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        case CHARACTER:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(BOOLEAN, ((int) (firstOperand.characterValue)) >= secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(BOOLEAN, ((int) (firstOperand.characterValue)) >= secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(BOOLEAN, ((int) (firstOperand.characterValue)) >= ((secondOperand.booleanValue) ? 1 : 0), root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(BOOLEAN, ((int) (firstOperand.characterValue)) >= (int) (secondOperand.characterValue), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        default:
                            Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                            return null;
                    }
                    break;
                case LESS_THAN:
                    switch (firstOperand.getType()) {
                        case INTEGER:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.intValue < secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.intValue < secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.intValue < ((secondOperand.booleanValue) ? 1 : 0), root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.intValue < (int) (secondOperand.characterValue), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        case FLOAT:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.doubleValue < secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.doubleValue < secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.doubleValue < ((secondOperand.booleanValue) ? 1 : 0), root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.doubleValue < (int) (secondOperand.characterValue), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        case BOOLEAN:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(BOOLEAN, ((firstOperand.booleanValue) ? 1 : 0) < secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(BOOLEAN, ((firstOperand.booleanValue) ? 1 : 0) < secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(BOOLEAN, ((firstOperand.booleanValue) ? 1 : 0) < ((secondOperand.booleanValue) ? 1 : 0), root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(BOOLEAN, ((firstOperand.booleanValue) ? 1 : 0) < (int) (secondOperand.characterValue), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        case CHARACTER:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(BOOLEAN, ((int) (firstOperand.characterValue)) < secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(BOOLEAN, ((int) (firstOperand.characterValue)) < secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(BOOLEAN, ((int) (firstOperand.characterValue)) < ((secondOperand.booleanValue) ? 1 : 0), root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(BOOLEAN, ((int) (firstOperand.characterValue)) < (int) (secondOperand.characterValue), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        default:
                            Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                            return null;
                    }
                    break;
                case LESS_THAN_OR_EQUAL:
                    switch (firstOperand.getType()) {
                        case INTEGER:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.intValue <= secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.intValue <= secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.intValue <= ((secondOperand.booleanValue) ? 1 : 0), root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.intValue <= (int) (secondOperand.characterValue), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        case FLOAT:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.doubleValue <= secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.doubleValue <= secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.doubleValue <= ((secondOperand.booleanValue) ? 1 : 0), root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.doubleValue <= (int) (secondOperand.characterValue), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        case BOOLEAN:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(BOOLEAN, ((firstOperand.booleanValue) ? 1 : 0) <= secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(BOOLEAN, ((firstOperand.booleanValue) ? 1 : 0) <= secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(BOOLEAN, ((firstOperand.booleanValue) ? 1 : 0) <= ((secondOperand.booleanValue) ? 1 : 0), root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(BOOLEAN, ((firstOperand.booleanValue) ? 1 : 0) <= (int) (secondOperand.characterValue), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        case CHARACTER:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(BOOLEAN, ((int) (firstOperand.characterValue)) <= secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(BOOLEAN, ((int) (firstOperand.characterValue)) <= secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(BOOLEAN, ((int) (firstOperand.characterValue)) <= ((secondOperand.booleanValue) ? 1 : 0), root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(BOOLEAN, ((int) (firstOperand.characterValue)) <= (int) (secondOperand.characterValue), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        default:
                            Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                            return null;
                    }
                    break;
                case EQUALS:
                    switch (firstOperand.getType()) {
                        case INTEGER:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.intValue == secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.intValue.doubleValue() == secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.intValue == ((secondOperand.booleanValue) ? 1 : 0), root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.intValue == (int) (secondOperand.characterValue), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        case FLOAT:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.doubleValue == secondOperand.intValue.doubleValue(), root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.doubleValue == secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.doubleValue == ((secondOperand.booleanValue) ? 1 : 0), root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.doubleValue == (int) (secondOperand.characterValue), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        case BOOLEAN:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(BOOLEAN, ((firstOperand.booleanValue) ? 1 : 0) == secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(BOOLEAN, ((firstOperand.booleanValue) ? 1 : 0) == secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(BOOLEAN, ((firstOperand.booleanValue) ? 1 : 0) == ((secondOperand.booleanValue) ? 1 : 0), root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(BOOLEAN, ((firstOperand.booleanValue) ? 1 : 0) == (int) (secondOperand.characterValue), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        case CHARACTER:
                            switch (secondOperand.getType()) {
                                case INTEGER:
                                    firstOperand = new Lexeme(BOOLEAN, ((int) (firstOperand.characterValue)) == secondOperand.intValue, root.getLineNumber());
                                    break;
                                case FLOAT:
                                    firstOperand = new Lexeme(BOOLEAN, ((int) (firstOperand.characterValue)) == secondOperand.doubleValue, root.getLineNumber());
                                    break;
                                case BOOLEAN:
                                    firstOperand = new Lexeme(BOOLEAN, ((int) (firstOperand.characterValue)) == ((secondOperand.booleanValue) ? 1 : 0), root.getLineNumber());
                                    break;
                                case CHARACTER:
                                    firstOperand = new Lexeme(BOOLEAN, ((int) (firstOperand.characterValue)) == (int) (secondOperand.characterValue), root.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        default:
                            Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                            return null;
                    }
                    break;
                case AND:
                    switch (firstOperand.getType()) {
                        case BOOLEAN:
                            switch (secondOperand.getType()) {
                                case BOOLEAN:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.booleanValue && secondOperand.booleanValue, secondOperand.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        default:
                            Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                            return null;
                    }
                    break;
                case OR:
                    switch (firstOperand.getType()) {
                        case BOOLEAN:
                            switch (secondOperand.getType()) {
                                case BOOLEAN:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.booleanValue || secondOperand.booleanValue, secondOperand.getLineNumber());
                                    break;
                                default:
                                    Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                                    return null;
                            }
                            break;
                        default:
                            Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " between types " + firstOperand.getType() + " and " + secondOperand.getType());
                            return null;
                    }
                    break;
                default:
                    Forest.error(firstOperand.getLineNumber(), "operator " + firstOperand.getType() + " not supported");
            }
        }
        if (nextOpList != null) {
            Lexeme nextRoot = new Lexeme(VARIADIC_OPERATION, root.getLineNumber());
            nextRoot.setLeft(nextOpList);
            nextRoot.setRight(new Lexeme(EXPRESSION_LIST, root.getLineNumber()));
            nextRoot.getRight().setLeft(firstOperand);
            Lexeme expressionList = root.getRight();
            for (int i = 0; i < numOfExpressions; i++) {
                expressionList = expressionList.getRight();
            }
            nextRoot.getRight().setRight(expressionList);
            if (numOfExpressions >= expressionValues.size()) {
                Forest.error(expressionList.getLineNumber(), " not enough expression values");
            }
            expressionValues.add(numOfExpressions, firstOperand); //TODO effiency, might change to linked list
            return variadicOperation(nextRoot, environment, expressionValues.subList(numOfExpressions, expressionValues.size()));
        }
        List<Lexeme> valList = new ArrayList<Lexeme>();
        valList.add(firstOperand);
        return valList;
    }

    private static Lexeme intDivision(Lexeme a, Lexeme b) {
        if (a.intValue % b.intValue == 0) {
            return new Lexeme(INTEGER, a.intValue / b.intValue, a.getLineNumber());
        }
        return new Lexeme(FLOAT, (double) a.intValue / b.intValue, a.getLineNumber());
    }
}
