package com.propp;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Forest {
    public static void main(String[] args) throws IOException {
        try {
            if (singlePathProvided(args)) runFile(args[0]);
            else {
                System.out.println(("Usage: forest [path to .tree file]"));
                System.exit(64);
            }
        } catch (IOException e) {
            throw new IOException(e.toString());
        }
    }

    private static boolean singlePathProvided(String[] args) {
        return args.length == 1;
    }

    public static void runFile(String path) throws IOException {
        String soruceCode = getSourceCodeFromFile(path);
        run(soruceCode);
    }

    private static String getSourceCodeFromFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        return new String(bytes, Charset.defaultCharset());
    }

    public static void run(String sourceCode) throws IOException {
        Lexer lexer = new Lexer(sourceCode);
        ArrayList<Lexeme> lexemes = lexer.lex();
        Parser rec = new Parser(lexemes);
        Lexeme root = rec.program();
        Parser.printTree(root);
        Evaluator evaluator = new Evaluator();
        List<Lexeme> output = evaluator.eval(root, new Environment(null));
        for (Lexeme lex : output) {
            System.out.println(lex.getValueString());
        }
    }

    private static void printLexemes(ArrayList<Lexeme> lexemes) {
        for (Lexeme lexeme : lexemes) System.out.println(lexeme.toString());
    }

    public static void error(int lineNumber, String msg) {
        report(lineNumber, msg);
    }

    public static void error(Lexeme lexeme, String msg) {
        error(lexeme.getLineNumber(), msg);
    }

    private static void report(int lineNumber, String msg) {
        System.err.println("line:" + lineNumber + " " + msg);
    }
}