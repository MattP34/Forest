package com.propp;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

public class Forest {
    public static void main(String[] args) {
        try {
            if(singlePathProvided(args)) runFile(args[0]);
            else {
                System.out.println(("Usage: forest [path to .tree file]"));
                System.exit(64);
            }
        } catch(IOException e) {
            System.out.println(e.toString());
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

    public static void run(String sourceCode) {
        Lexer lexer = new Lexer(sourceCode);
        ArrayList<Lexeme> lexemes = lexer.lex();
        printLexemes(lexemes);
    }

    private static void printLexemes(ArrayList<Lexeme> lexemes) {
        for(Lexeme lexeme: lexemes) System.out.println(lexeme.toString());
    }
}