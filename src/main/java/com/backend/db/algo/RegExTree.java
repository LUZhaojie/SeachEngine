package com.backend.db.algo;

import java.util.ArrayList;



public class RegExTree {

    protected int root;
    protected ArrayList<RegExTree> subTrees;
    public RegExTree(int root, ArrayList<RegExTree> subTrees) {
        this.root = root;
        this.subTrees = subTrees;
    }
    //FROM TREE TO PARENTHESIS
    public String toString() {
        if (subTrees.isEmpty()) return rootToString();
        String result = rootToString()+"("+subTrees.get(0).toString();
        for (int i=1;i<subTrees.size();i++) result+=","+subTrees.get(i).toString();
        return result+")";
    }
    private String rootToString() {
        if (root==Constante.CONCAT) return ".";
        if (root==Constante.ETOILE) return "*";
        if (root==Constante.ALTERN) return "|";
        if (root==Constante.DOT) return ".";
        return Character.toString((char)root);
    }

    //FROM REGEX TO SYNTAX TREE
    public static RegExTree parse(String regEx) throws Exception {

        ArrayList<RegExTree> result = new ArrayList<RegExTree>();
        for (int i=0;i<regEx.length();i++) result.add(new RegExTree(charToRoot(regEx.charAt(i)),new ArrayList<RegExTree>()));

        return parse(result);
    }
    private static int charToRoot(char c) {
        if (c=='.') return Constante.DOT;
        if (c=='*') return Constante.ETOILE;
        if (c=='|') return Constante.ALTERN;
        if (c=='(') return Constante.PARENTHESEOUVRANT;
        if (c==')') return Constante.PARENTHESEFERMANT;
        return (int)c;
    }
    private static RegExTree parse(ArrayList<RegExTree> result) throws Exception {
        while (containParenthese(result)) result=processParenthese(result);
        while (containEtoile(result)) result=processEtoile(result);
        while (containConcat(result)) result=processConcat(result);
        while (containAltern(result)) result=processAltern(result);

        if (result.size()>1) throw new Exception();

        return removeProtection(result.get(0));
    }
    private static boolean containParenthese(ArrayList<RegExTree> trees) {
        for (RegExTree t: trees) if (t.root==Constante.PARENTHESEFERMANT || t.root==Constante.PARENTHESEOUVRANT) return true;
        return false;
    }
    private static ArrayList<RegExTree> processParenthese(ArrayList<RegExTree> trees) throws Exception {
        ArrayList<RegExTree> result = new ArrayList<RegExTree>();
        boolean found = false;
        for (RegExTree t: trees) {
            if (!found && t.root==Constante.PARENTHESEFERMANT) {
                boolean done = false;
                ArrayList<RegExTree> content = new ArrayList<RegExTree>();
                while (!done && !result.isEmpty())
                    if (result.get(result.size()-1).root==Constante.PARENTHESEOUVRANT) { done = true; result.remove(result.size()-1); }
                    else content.add(0,result.remove(result.size()-1));
                if (!done) throw new Exception();
                found = true;
                ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
                subTrees.add(parse(content));
                result.add(new RegExTree(Constante.PROTECTION, subTrees));
            } else {
                result.add(t);
            }
        }
        if (!found) throw new Exception();
        return result;
    }
    private static boolean containEtoile(ArrayList<RegExTree> trees) {
        for (RegExTree t: trees) if (t.root==Constante.ETOILE && t.subTrees.isEmpty()) return true;
        return false;
    }
    private static ArrayList<RegExTree> processEtoile(ArrayList<RegExTree> trees) throws Exception {
        ArrayList<RegExTree> result = new ArrayList<RegExTree>();
        boolean found = false;
        for (RegExTree t: trees) {
            if (!found && t.root==Constante.ETOILE && t.subTrees.isEmpty()) {
                if (result.isEmpty()) throw new Exception();
                found = true;
                RegExTree last = result.remove(result.size()-1);
                ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
                subTrees.add(last);
                result.add(new RegExTree(Constante.ETOILE, subTrees));
            } else {
                result.add(t);
            }
        }
        return result;
    }
    private static boolean containConcat(ArrayList<RegExTree> trees) {
        boolean firstFound = false;
        for (RegExTree t: trees) {
            if (!firstFound && t.root!=Constante.ALTERN) { firstFound = true; continue; }
            if (firstFound) if (t.root!=Constante.ALTERN) return true; else firstFound = false;
        }
        return false;
    }
    private static ArrayList<RegExTree> processConcat(ArrayList<RegExTree> trees) throws Exception {
        ArrayList<RegExTree> result = new ArrayList<RegExTree>();
        boolean found = false;
        boolean firstFound = false;
        for (RegExTree t: trees) {
            if (!found && !firstFound && t.root!=Constante.ALTERN) {
                firstFound = true;
                result.add(t);
                continue;
            }
            if (!found && firstFound && t.root==Constante.ALTERN) {
                firstFound = false;
                result.add(t);
                continue;
            }
            if (!found && firstFound && t.root!=Constante.ALTERN) {
                found = true;
                RegExTree last = result.remove(result.size()-1);
                ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
                subTrees.add(last);
                subTrees.add(t);
                result.add(new RegExTree(Constante.CONCAT, subTrees));
            } else {
                result.add(t);
            }
        }
        return result;
    }
    private static boolean containAltern(ArrayList<RegExTree> trees) {
        for (RegExTree t: trees) if (t.root==Constante.ALTERN && t.subTrees.isEmpty()) return true;
        return false;
    }
    private static ArrayList<RegExTree> processAltern(ArrayList<RegExTree> trees) throws Exception {
        ArrayList<RegExTree> result = new ArrayList<RegExTree>();
        boolean found = false;
        RegExTree gauche = null;
        boolean done = false;
        for (RegExTree t: trees) {
            if (!found && t.root==Constante.ALTERN && t.subTrees.isEmpty()) {
                if (result.isEmpty()) throw new Exception();
                found = true;
                gauche = result.remove(result.size()-1);
                continue;
            }
            if (found && !done) {
                if (gauche==null) throw new Exception();
                done=true;
                ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
                subTrees.add(gauche);
                subTrees.add(t);
                result.add(new RegExTree(Constante.ALTERN, subTrees));
            } else {
                result.add(t);
            }
        }
        return result;
    }
    private static RegExTree removeProtection(RegExTree tree) throws Exception {
        if (tree.root==Constante.PROTECTION && tree.subTrees.size()!=1) throw new Exception();
        if (tree.subTrees.isEmpty()) return tree;
        if (tree.root==Constante.PROTECTION) return removeProtection(tree.subTrees.get(0));

        ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
        for (RegExTree t: tree.subTrees) subTrees.add(removeProtection(t));
        return new RegExTree(tree.root, subTrees);
    }

}
