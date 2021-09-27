package com.propp;

import static com.propp.TokenType.*;

public class EnvironmentTestFile {
    public static void main(String[] args) {
        Environment global = new Environment(null);
        Environment func1 = new Environment(global);
        global.addVariable(new Lexeme(IDENTIFIER, "x", 1), new Lexeme(INTEGER, 5, 1));
        func1.addVariable(new Lexeme(IDENTIFIER, "y", 2), new Lexeme(FLOAT, 10.5, 2));
        System.out.println(func1.getVariableValue(new Lexeme(IDENTIFIER, "y", 2)));
        System.out.println(func1.getVariableValue(new Lexeme(IDENTIFIER, "x", 1)));
        System.out.println(global.getVariableValue(new Lexeme(IDENTIFIER, "x", 1)));
        System.out.println(global.getVariableValue(new Lexeme(IDENTIFIER, "y", 2)));
        System.out.println(func1.variableExists(new Lexeme(IDENTIFIER, "y", 2)));
    }
}
