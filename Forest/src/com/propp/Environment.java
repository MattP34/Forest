package com.propp;

import java.util.HashMap;

public class Environment {

    private Environment parent;
    private HashMap<Lexeme, Lexeme> variables;

    public Environment(Environment parent) {
        this.parent = parent;
        this.variables = new HashMap<Lexeme, Lexeme>();
    }

    //not instatation, modifying and creating are the same statement so 1 method for both
    public void addVariable(Lexeme identifier, Lexeme value) {
        variables.put(identifier, value);
    }

    public boolean variableExists(Lexeme identifier) {
        return variables.containsKey(identifier) || (this.parent != null && this.parent.variableExists(identifier));
    }

    public Lexeme getVariableValue(Lexeme identifier) {
        if (!variables.containsKey(identifier)) {
            if (this.parent == null) return variableNotFound(identifier);
            Lexeme val = this.parent.getVariableValue(identifier);
            if (val == null) return variableNotFound(identifier);
            return val;
        }
        return variables.get(identifier);
    }

    public Lexeme getIdentifier(Lexeme identifier) { //TODO make this more time efficient
        if (!variableExists(identifier)) return null;
        if (this.variables.containsKey(identifier)) {
            for (Lexeme key : this.variables.keySet()) {
                if (key.equals(identifier)) return key;
            }
        } else {
            return this.parent.getIdentifier(identifier);
        }
        return null;
    }

    private static Lexeme variableNotFound(Lexeme identifier) {
        Forest.error(identifier.getLineNumber(), "Variable " + identifier.stringValue + " not declared");
        return null;
    }

    public void printEnvironment() {
        System.out.println("Environment:" + this);
        for (Lexeme lex : variables.keySet()) {
            System.out.println(lex.stringValue + ":" + variables.get(lex));
        }
    }
}
