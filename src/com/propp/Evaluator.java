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
                return evalUnaryOperation(tree, environment);
            case ARRAY_ACCESS:
                return arrayAccess(tree, environment);
            case VARIABLE:
                return evalVariable(tree, environment);
            case ASSIGN:
                return assignVariable(tree, environment);
            case IF:
            case ELIF:
                return evalIf(tree, environment);
            case ELSE:
                return evalElse(tree, environment);
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
        if (root.getLeft().stringValue.equals("print")) {
            Forest.error(root.getLineNumber(), " can not declare function with name print");
            return null;
        }
        environment.addVariable(root.getLeft(), root.getRight());
        return createSingleList(root.getRight());
    }

    private List<Lexeme> functionCall(Lexeme root, Environment environment) {
        if (root.getLeft().stringValue.equals("print")) {
            print(root.getLeft().getLeft(), environment);
            return null;
        }
        if (!environment.variableExists(root.getLeft())) {
            Forest.error(root.getLineNumber(), "function " + root.getLeft().stringValue + " not declared");
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

    private void print(Lexeme root, Environment environment) {
        String str = "";
        List<Lexeme> expressionList = eval(root, environment);
        for (int i = 0; i < expressionList.size(); i++) {
            if (expressionList.get(i) == null) return;
            switch (expressionList.get(i).getType()) {
                case INTEGER:
                    str += expressionList.get(i).intValue;
                    break;
                case FLOAT:
                    str += expressionList.get(i).doubleValue;
                    break;
                case STRING:
                    str += expressionList.get(i).stringValue;
                    break;
                case BOOLEAN:
                    str += expressionList.get(i).booleanValue;
                    break;
                case CHARACTER:
                    str += expressionList.get(i).characterValue;
                    break;
                default:
                    Forest.error(root.getLineNumber(), "unsupported type " + expressionList.get(i).getType() + "for print statement");
            }
            if (i < expressionList.size() - 1) {
                str += ",";
            }
        }
        System.out.println(str);
    }

    private List<Lexeme> evalStatementList(Lexeme statementList, Environment environment) {
        if (statementList == null) return null;
        if (statementList.getLeft() == null) return null;
        if (statementList.getLeft().getType() == RETURN) {
            return eval(statementList.getLeft(), environment); //evaluating return function
        }
        List<Lexeme> temp = eval(statementList.getLeft(), environment);
        if (returning) return temp;
        return evalStatementList(statementList.getRight(), environment);
    }

    private List<Lexeme> evalReturnStatement(Lexeme root, Environment environment) {
        if (this.functionCounter == 0) {
            Forest.error(root.getLineNumber(), " return statement outside of a function");
        }
        List<Lexeme> temp = eval(root.getLeft(), environment);
        this.returning = true;
        return temp;
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
        Lexeme variableListNode = root.getLeft();
        List<Lexeme> variables = new ArrayList<Lexeme>();
        while (variableListNode != null) {
            if (variableListNode.getLeft() != null) variables.add(variableListNode.getLeft());
            variableListNode = variableListNode.getRight();
        }
        if (expressionVals.size() != variables.size()) {
            Forest.error(root.getLineNumber(), " mismatching number of expressions and variables. Expressions:" + expressionVals.size() + " Variables:" + variables.size());
            return null;
        }
        for (int i = 0; i < variables.size(); i++) {
            if (variables.get(i).getLeft() != null) {
                Lexeme identifier = variables.get(i);
                Lexeme temp = identifier.getLeft();
                if (temp.getType() != INTEGER) {
                    Forest.error(temp.getLineNumber(), "expected Integer for array location found " + variables.get(i).getLeft().getType());
                    return null;
                }
                if (!environment.variableExists(identifier)) {
                    Forest.error(identifier.getLineNumber(), "array not instatiated");
                    return null;
                }
                Lexeme array = environment.getVariableValue(identifier);
                while (temp != null) {
                    if (array.getType() != ARRAY) {
                        Forest.error(identifier.getLineNumber(), identifier.stringValue + " is not an array of required dimension");
                    }
                    if (temp.getLeft() == null) {//no more dimensions of the array
                        if (array.arrayValue.length <= temp.intValue) {
                            Forest.error(identifier.getLineNumber(), "Array index out of bounds. Length:" + array.arrayValue.length + " Value:" + temp.intValue);
                            return null;
                        }
                        array.arrayValue[temp.intValue] = expressionVals.get(i);
                    } else {
                        array = array.arrayValue[temp.intValue];
                    }
                    temp = temp.getLeft();
                }
            } else {
                environment.addVariable(variables.get(i), expressionVals.get(i));
            }
        }
        return null;
    }

    private List<Lexeme> arrayAccess(Lexeme root, Environment environment) {
        List<Lexeme> array = eval(root.getLeft(), environment);
        if (array.size() != 1) {
            Forest.error(root.getLineNumber(), "not single variable for array");
            return null;
        }
        Lexeme arr = array.get(0);
        Lexeme temp = root.getRight();
        while (temp != null) {
            if (arr.getType() != ARRAY) {
                Forest.error(root.getLineNumber(), "not enough dimensions for array");
                return null;
            }
            if (temp.getLeft() == null || temp.getLeft().getType() != INTEGER) {
                Forest.error(root.getLineNumber(), "must have integer position for array");
                return null;
            }
            if (arr.arrayValue.length <= temp.getLeft().intValue) {
                Forest.error(root.getLineNumber(), "Array index out of bounds. Length:" + arr.arrayValue.length + " Value:" + temp.getLeft().intValue);
                return null;
            }
            arr = arr.arrayValue[temp.getLeft().intValue];
            temp = temp.getRight();
        }
        return createSingleList(arr);
    }

    private List<Lexeme> evalWhile(Lexeme root, Environment environment) {
        Environment loopEnvir = new Environment(environment);
        while (checkIfReturnsTrue(root.getLeft(), environment)) {
            List<Lexeme> temp = evalStatementList(root.getRight(), loopEnvir);
            if (this.returning) {
                return temp;
            }
        }
        return null;
    }

    private List<Lexeme> evalIf(Lexeme root, Environment environment) {
        Environment ifEnvir = new Environment(environment);
        if (checkIfReturnsTrue(root.getLeft(), environment)) {
            List<Lexeme> temp = evalStatementList(root.getRight().getLeft(), ifEnvir);
            if (this.returning) {
                return temp;
            }
        } else {
            if (root.getRight().getRight() != null) {
                return eval(root.getRight().getRight(), environment);
            }
        }
        return null;
    }

    private List<Lexeme> evalElse(Lexeme root, Environment environment) {
        Environment elseEnvir = new Environment(environment);
        List<Lexeme> temp = evalStatementList(root.getRight().getLeft(), elseEnvir);
        if (this.returning) {
            return temp;
        }
        return null;
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

    private List<Lexeme> createArray(List<Lexeme> expressionValues, Environment environment) {
        if (expressionValues.size() == 0) {
            Forest.error(0, "empty expressions for array creation");
            return null;
        }
        Lexeme array = new Lexeme(ARRAY, new Lexeme[expressionValues.get(0).intValue], expressionValues.get(0).getLineNumber());
        arrayCreationHelper(array, expressionValues, environment);
        return createSingleList(array);
    }

    private void arrayCreationHelper(Lexeme array, List<Lexeme> expressionValues, Environment environment) {
        if (expressionValues.size() == 0) return;
        if (expressionValues.get(0).getType() != INTEGER) {
            Forest.error(array.getLineNumber(), "array size must be an integer. Found a " + expressionValues.get(0).getType());
        }
        for (int i = 0; i < expressionValues.get(0).intValue; i++) {
            if (expressionValues.size() == 1) {
                array.arrayValue[i] = new Lexeme(INTEGER, 0, array.getLineNumber());
            } else {
                if (expressionValues.get(1).getType() != INTEGER) {
                    Forest.error(array.getLineNumber(), "array size must be an integer. Found a " + expressionValues.get(0).getType());
                }
                array.arrayValue[i] = new Lexeme(ARRAY, new Lexeme[expressionValues.get(1).intValue], array.getLineNumber());
                arrayCreationHelper(array.arrayValue[i], expressionValues.subList(1, expressionValues.size()), environment);
            }
        }
    }

    private List<Lexeme> evalUnaryOperation(Lexeme root, Environment environment) {
        Lexeme operator = root.getLeft();
        List<Lexeme> expressionVals = eval(root.getRight(), environment);
        if (expressionVals.size() != 1) {
            Forest.error(root.getLineNumber(), "invalid number of arguments for operator " + operator.getType() + " expected 1 founnd " + expressionVals.size());
        }
        switch (operator.getType()) {
            case NOT:
                switch (expressionVals.get(0).getType()) {
                    case BOOLEAN:
                        return createSingleList(new Lexeme(BOOLEAN, !(expressionVals.get(0).booleanValue), root.getRight().getLineNumber()));
                    default:
                        Forest.error(root.getLineNumber(), "invalid operation: " + operator.getType() + " for type " + expressionVals.get(0).getType());
                        return null;
                }
            case EQUAL:
                return createSingleList(expressionVals.get(0));
            default:
                Forest.error(operator.getLineNumber(), "operator " + operator.getType() + " not supported");
                return null;
        }
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
        if (opList.getRight() != null && opList.getRight().getLeft() != null) { //if there is another operator
            if (opList.getRight().getRight() == null) numOfExpressions = 2; //if there is no specified number
            else numOfExpressions = opList.getRight().getRight().intValue + 1;
            nextOpList = opList.getRight().getLeft();
        } else {
            numOfExpressions = expressionValues.size();
            if (opList.getRight() != null && opList.getRight().getRight() != null) {
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
        if (operator.getType() == ARRAY_CREATION) {
            return createArray(expressionValues, environment);
        }
        if (firstOperand != null && operator.getType() == MINUS && numOfExpressions == 1) {
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
            return createSingleList(firstOperand);
        }
        for (int i = 1; i < numOfExpressions; i++) {
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
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.doubleValue == (double) (int) (secondOperand.characterValue), root.getLineNumber());
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
                        case STRING:
                            switch (secondOperand.getType()) {
                                case STRING:
                                    firstOperand = new Lexeme(BOOLEAN, firstOperand.stringValue.equals(secondOperand.stringValue), root.getLineNumber());
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
