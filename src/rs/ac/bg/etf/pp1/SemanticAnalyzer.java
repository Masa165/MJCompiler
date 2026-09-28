package rs.ac.bg.etf.pp1;

import static rs.etf.pp1.symboltable.Tab.charType;
import static rs.etf.pp1.symboltable.Tab.chrObj;
import static rs.etf.pp1.symboltable.Tab.intType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import java_cup.runtime.virtual_parse_stack;
import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Scope;
import rs.etf.pp1.symboltable.concepts.Struct;

public class SemanticAnalyzer extends VisitorAdaptor {
    Logger log = Logger.getLogger(getClass());
    int nVars;
    public static Scope mainScope;
    public static Obj adrObj;
    public static Obj exprObj;
    public static Obj indObj;
    public static Obj val1Obj;
    public static Obj val2Obj;
    public static Obj val3Obj;

    private static class DeclInfo {
        String name;
        Struct type;
        int val;

        DeclInfo(String name, Struct type) {
            this.name = name;
            this.type = type;
        }

        DeclInfo(String name, Struct type, int val) {
            this.name = name;
            this.type = type;
            this.val = val;
        }
    }

    boolean errorDetected = false;

    private void reportSymbolUsage(SyntaxNode node, Obj obj) {
        if (obj == null || obj == Tab.noObj)
            return;
        int line = (node == null) ? 0 : node.getLine();
        String msg = String.format("Koriscenje simbola: %s, %s na liniji %d",
                obj.getName(), obj.toString(), line);
        log.info(msg);
    }

    public void report_error(String message, SyntaxNode info) {
        errorDetected = true;
        StringBuilder msg = new StringBuilder(message);
        int line = (info == null) ? 0 : info.getLine();
        if (line != 0)
            msg.append(" na liniji ").append(line);
        log.error(msg.toString());
    }

    public void report_info(String message, SyntaxNode info) {
        StringBuilder msg = new StringBuilder(message);
        int line = (info == null) ? 0 : info.getLine();
        if (line != 0)
            msg.append(" na liniji ").append(line);
        log.info(msg.toString());
    }

    public void visit(ProgramNamee programNamee) {
        programNamee.obj = Tab.insert(Obj.Prog, programNamee.getProgramName(), Tab.noType);
        Tab.openScope();
    }

    public void visit(ProgramRoot programRoot) {
        adrObj = Tab.insert(Obj.Var, "#adr", intType);
        exprObj = Tab.insert(Obj.Var, "#expr", intType);
        indObj = Tab.insert(Obj.Var, "#ind", intType);
        val1Obj = Tab.insert(Obj.Var, "#val1", intType);
        val2Obj = Tab.insert(Obj.Var, "#val2", intType);
        val3Obj = Tab.insert(Obj.Var, "#val3", intType);
        nVars = Tab.currentScope.getnVars();
        Obj mainObj = Tab.find("main");
        if (mainObj == Tab.noObj) {
            report_error("Program mora sadrzati metodu main", null);
        } else if (mainObj.getKind() != Obj.Meth || mainObj.getType() != Tab.noType) {
            report_error("main mora biti void metoda bez parametara", null);
        }
        Tab.chainLocalSymbols(programRoot.getProgramName().obj);
        Tab.closeScope();

    }

    public void visit(IdentType identType) {
        Obj tyObj = Tab.find(identType.getIdentificator());
        if (tyObj == Tab.noObj) {
            report_error("Nije prondjen tip: " + identType.getIdentificator() + " u tabeli simbola.", identType);
            identType.struct = Tab.noType;
        } else {
            if (tyObj.getKind() == Obj.Type) {
                identType.struct = tyObj.getType();
            } else {
                report_error("Ime: " + identType.getIdentificator() + " ne predstavlja tip.", null);
                identType.struct = Tab.noType;
            }
        }
    }

    public void visit(MethodVoidName methodVoidName) {
        methodVoidName.obj = Tab.insert(Obj.Meth, methodVoidName.getMethodName(), Tab.noType);
        Tab.openScope();
        mainScope = Tab.currentScope;
        if (methodVoidName.obj == Tab.noObj) {
            report_error("Funkcija " + methodVoidName.getMethodName() + "je vec definisana", null);
        } else {
            report_info("Obradjuje se funkcija " + methodVoidName.getMethodName(), methodVoidName);
        }

    }

    public void visit(MethodDeclVoid methodDeclVoid) {
        Tab.chainLocalSymbols(methodDeclVoid.getMethodDeclName().obj);
        Tab.closeScope();
    }

    private List<DeclInfo> collectDeclarations(VarDec node) {
        if (node instanceof SimpleVar) {
            SimpleVar sv = (SimpleVar) node;
            List<DeclInfo> list = new ArrayList<>();
            list.add(new DeclInfo(sv.getIdent(), sv.getType().struct));
            return list;
        }
        if (node instanceof ArrayVar) {
            ArrayVar av = (ArrayVar) node;
            Struct arrayType = new Struct(Struct.Array, av.getType().struct);
            List<DeclInfo> list = new ArrayList<>();
            list.add(new DeclInfo(av.getIdent(), arrayType));
            return list;

        }
        if (node instanceof SimpleVarDecls) {
            SimpleVarDecls svd = (SimpleVarDecls) node;
            List<DeclInfo> leftList = collectDeclarations(svd.getVarDec());
            Struct commonType = leftList.get(0).type;
            leftList.add(new DeclInfo(svd.getIdent(), commonType));
            return leftList;
        }
        if (node instanceof ArrayVarDecls) {
            ArrayVarDecls avd = (ArrayVarDecls) node;
            List<DeclInfo> leftList = collectDeclarations(avd.getVarDec());
            Struct commonType = leftList.get(0).type;
            Struct arrayType = new Struct(Struct.Array, commonType);
            leftList.add(new DeclInfo(avd.getIdent(), arrayType));
            return leftList;
        }

        return Collections.emptyList();
    }

    // VarDecl
    public void visit(VarDeclSingle varDeclSingle) {
        VarDec varDec = varDeclSingle.getVarDec();
        List<DeclInfo> decls = collectDeclarations(varDec);
        for (DeclInfo d : decls) {
            Obj obj = Tab.insert(Obj.Var, d.name, d.type);
            if (obj == Tab.noObj) {
                report_error("Dupla deklaracija: " + d.name, varDeclSingle);
            } else {
                report_info("Deklarisana promenljiva " + (d.type.getKind() == Struct.Array ? "niza " : "") + d.name,
                        varDeclSingle);
            }
        }
    }

    private boolean areTypesTheSame(Struct left, Struct right) {
        if (left == null || right == null)
            return false;
        if (left.getKind() == Struct.Array && right.getKind() == Struct.Array) {
            return areTypesTheSame(left.getElemType(), right.getElemType());
        }
        return left.getKind() == right.getKind();
    }

    private List<DeclInfo> collectDeclsConst(ConstDec node) {
        if (node instanceof SimpleConstDec) {
            SimpleConstDec scd = (SimpleConstDec) node;
            List<DeclInfo> list = new ArrayList<>();
            if (!areTypesTheSame(scd.getType().struct, scd.getConst().obj.getType()))
                report_error("Nekompatibilni tip dodela vrednosti simbolickoj konstanti " + scd.getIdent(), null);
            list.add(new DeclInfo(scd.getIdent(), scd.getType().struct, scd.getConst().obj.getAdr()));
            return list;
        }
        if (node instanceof ConstDecList) {
            ConstDecList cdl = (ConstDecList) node;
            List<DeclInfo> leftList = collectDeclsConst(cdl.getConstDec());
            Struct commonType = leftList.get(0).type;
            if (!areTypesTheSame(commonType, cdl.getConst().obj.getType()))
                report_error("Nekompatibilni tip dodela vrednosti simbolickoj konstanti " + cdl.getIdent(), null);
            leftList.add(new DeclInfo(cdl.getIdent(), commonType, cdl.getConst().obj.getAdr()));
            return leftList;

        }
        return Collections.emptyList();
    }

    public void visit(ConstDeclSingle constDeclSingle) {
        ConstDec constDec = constDeclSingle.getConstDec();
        List<DeclInfo> decls = collectDeclsConst(constDec);
        for (DeclInfo d : decls) {
            Obj obj = MyTab.insert(Obj.Con, d.name, d.type, d.val, 0);
            if (obj == Tab.noObj) {
                report_error("Dupla deklaracija: " + d.name, constDeclSingle);
            } else {
                report_info("Deklarisana konstanta " + d.name, constDeclSingle);
            }
        }

    }

    public void visit(Num num) {
        num.obj = new Obj(Obj.Con, "", intType, num.getVal(), 0);
    }

    public void visit(Char c) {
        c.obj = new Obj(Obj.Con, "", charType, c.getVal(), 0);
    }

    public void visit(BoolFalse boolFalse) {
        boolFalse.obj = new Obj(Obj.Con, "", MyTab.boolType, 0, 0);
    }

    public void visit(BoolTrue boolTrue) {
        boolTrue.obj = new Obj(Obj.Con, "", MyTab.boolType, 1, 0);
    }

    public void visit(Numm numm) {
        numm.obj = numm.getNumConst().obj;
    }

    public void visit(Charr c) {
        c.obj = c.getCharConst().obj;
    }

    public void visit(Booll booll) {
        booll.obj = booll.getBoolConst().obj;
    }

    // Factor
    public void visit(NumFact numFact) {
        numFact.struct = numFact.getNumConst().obj.getType();
    }

    public void visit(CharFact charFact) {
        charFact.struct = charFact.getCharConst().obj.getType();
    }

    public void visit(BoolFact boolFact) {
        boolFact.struct = boolFact.getBoolConst().obj.getType();
    }

    public void visit(ExprFact exprFact) {
        exprFact.struct = exprFact.getExpr().struct;
    }

    public void visit(DesignatorFact designatorFact) {
        designatorFact.struct = designatorFact.getDesignator().obj.getType();
    }

    public void visit(CreationFactor creationFactor) {
        if (creationFactor.getExpr().struct != Tab.intType) {
            report_error("Izraz za duzinu niza mora biti tipa int", creationFactor);
        }
        creationFactor.struct = new Struct(Struct.Array, creationFactor.getType().struct);
    }

    // Term
    public void visit(FactorsTerm factorsTerm) {
        if (!areTypesTheSame(factorsTerm.getFactor().struct, factorsTerm.getTerm().struct)) {
            report_error("Nekompatibilni tipovi ", factorsTerm);
            factorsTerm.struct = Tab.noType;
            return;
        } else {
            if (factorsTerm.getFactor().struct.getKind() != Struct.Int) {
                report_error("Operandi nisu tipa int, ", factorsTerm);
                factorsTerm.struct = Tab.noType;
                return;
            }
        }
        factorsTerm.struct = factorsTerm.getTerm().struct;
    }

    public void visit(FactorTerm factorTerm) {
        factorTerm.struct = factorTerm.getFactor().struct;
    }

    // designator
    public void visit(LengthDesignator ld) {
        Obj des = Tab.find(ld.getIdent());
        if (des == Tab.noObj) {
            report_error("Nedeklarisana promenljiva: " + ld.getIdent(), ld);
            ld.obj = Tab.noObj;
            return;
        } else {
            if (des.getType().getKind() != Struct.Array) {
                report_error("Promenljiva '" + des.getName() + "' nad kojom se pokusava .length nije tipa niza", ld);
                ld.obj = Tab.noObj;
                return;
            }
        }
        ld.obj = new Obj(Obj.Con, "", intType);
        reportSymbolUsage(ld, des);
    }

    public void visit(IndexDesingator id) {
        Obj des = Tab.find(id.getIdent());
        if (des == Tab.noObj) {
            report_error("Nedeklarisana promenljiva: " + id.getIdent(), id);
            id.obj = Tab.noObj;
            return;
        } else {
            if (des.getType().getKind() != Struct.Array) {
                report_error("Promenljiva '" + des.getName() + "' nad kojom se pokusava indeksiranje nije tipa niz",
                        id);
                id.obj = Tab.noObj;
                return;
            }
        }
        if (id.getExpr().struct != Tab.intType) {
            report_error("Izraz za indeksiranje nije tipa int", id);
            id.obj = Tab.noObj;
            return;
        }
        id.obj = new Obj(Obj.Elem, "", des.getType().getElemType(), 0, des.getLevel());
        reportSymbolUsage(id, des);
    }

    public void visit(FindAnyDesignator node) {
        SyntaxNode parent = node.getParent();
        if (!(parent instanceof FindAnyStatement)) {
            report_error("findAny se moze koristiti samo u naredbi findAny", node);
           
        }

        Obj des = Tab.find(node.getIdent());
        if (des == Tab.noObj) {
            report_error("Nedeklarisan identifikator: " + node.getIdent(), node);
            node.obj = Tab.noObj;
            return;
        }
        if (des.getType().getKind() != Struct.Array) {
            report_error("findAny se moze primeniti samo na niz", node);
            node.obj = Tab.noObj;
            return;
        }
        node.obj = des;
    }

    public void visit(IdentDesignator id) {
        Obj des = Tab.find(id.getIdent());
        if (des == Tab.noObj) {
            report_error("Promenljiva '" + id.getIdent() + "' nije deklarisana", id);
            id.obj = Tab.noObj;
            return;
        }
        id.obj = des;
        reportSymbolUsage(id, des);
    }
    public void visit(SIndexDesignator sid){
         Obj des = Tab.find(sid.getIdent());
        if (des == Tab.noObj) {
            report_error("Nedeklarisana promenljiva: " + sid.getIdent(), sid);
            sid.obj = Tab.noObj;
            return;
        } else {
            if (des.getType().getKind() != Struct.Array) {
                report_error("Promenljiva '" + des.getName() + "' nad kojom se pokusava indeksiranje nije tipa niz",sid);
                sid.obj = Tab.noObj;
                return;
            }
        }
        if (sid.getExpr().struct != Tab.intType) {
            report_error("Izraz za indeksiranje nije tipa int", sid);
            sid.obj = Tab.noObj;
            return;
        }
        if(des.getType().getElemType()!=Tab.intType){
            report_error("Array mora biti tipa int", sid);
            sid.obj=Tab.noObj;
            return;
        }
        sid.obj = new Obj(Obj.Elem, "", des.getType().getElemType(), 0, des.getLevel());
        reportSymbolUsage(sid, des);
    }

    // TermExpr
    public void visit(TermExprr termExpr) {
        termExpr.struct = termExpr.getTerm().struct;
    }

    public void visit(MinusTermExpr minusTermExpr) {
        if (minusTermExpr.getTerm().struct.getKind() != Struct.Int) {
            report_error("Nekompatibilni tip operanda, nije tipa int", minusTermExpr);
            minusTermExpr.struct = Tab.noType;
            return;
        }
        minusTermExpr.struct = minusTermExpr.getTerm().struct;
    }

    public void visit(TermsExpr t) {
        if (!(t.getTermExpr().struct == Tab.intType && t.getTerm().struct == Tab.intType)) {
            report_error("Nekompatibilni tipovi u izrazu", t);
            t.struct = Tab.noType;
            return;
        }
        t.struct = t.getTermExpr().struct;
    }

    // CondFact
    private boolean isReferenceType(Struct type) {
        if (type == null)
            return false;
        int kind = type.getKind();
        return kind == Struct.Array || kind == Struct.Class;
    }

    private boolean areTypesCompatible(Struct left, Struct right) {
        if (left == right)
            return true;
        if (left == null || right == null)
            return false;
        if (left == Tab.nullType) {
            return isReferenceType(right);
        }
        if (right == Tab.nullType) {
            return isReferenceType(left);
        }
        if (left.getKind() != right.getKind())
            return false;
        if (left.getKind() == Struct.Array) {
            return areTypesCompatible(left.getElemType(), right.getElemType());
        }
        /// PAZI
        return true;
    }

    public void visit(CondFactRel condFactRel) {
        TermExpr leftTerm = condFactRel.getTermExpr();
        TermExpr rightTerm = condFactRel.getTermExpr1();
        Relop relopNode = condFactRel.getRelop();

        Struct leftType = leftTerm.struct;
        Struct rightType = rightTerm.struct;

        // Provera da li su tipovi validni
        if (leftType == null || leftType == Tab.noType) {
            report_error("Levi operand ternarnog operatora nema ispravan tip", leftTerm);
            return;
        }
        if (rightType == null || rightType == Tab.noType) {
            report_error("Desni operand ternarnog operatora nema ispravan tip", rightTerm);
            return;
        }
        if (!areTypesCompatible(leftType, rightType)) {
            report_error("Tipovi operanda ternarnog operatora nisu kompatibilni", condFactRel);
            return;
        }

        boolean leftRef = isReferenceType(leftType);
        boolean rightRef = isReferenceType(rightType);
        if (leftRef || rightRef) {
            if (!(relopNode instanceof Equal || relopNode instanceof NotEqual)) {
                report_error("Za referentne tipove u ternarnom operatoru dozvoljeni su samo == i !=", relopNode);
            }
        }
    }

    // Expr
    public void visit(TermExprProd termExprProd) {
        termExprProd.struct = termExprProd.getTermExpr().struct;
    }

    public void visit(CondExpr condExpr) {
        if (!areTypesTheSame(condExpr.getExpr().struct, condExpr.getExpr1().struct)) {
            report_error("Vrednosti izraza u ternarnom operatoru moraju biti iste", condExpr);
            condExpr.struct = Tab.noType;
            return;
        }

        condExpr.struct = condExpr.getExpr().struct;

    }
    public void visit(ExprQuestion eq){
        if(eq.getExpr().struct!=Tab.intType || eq.getExpr1().struct!=Tab.intType){
            report_error("U expr??expr izraz/i nisu tipa int ", eq);
            eq.struct=Tab.noType;
            return;
        }
        eq.struct = eq.getExpr().struct;
    }

    // DesignatorStatement
    public void visit(AssignmentStatement as) {
        if (as.getDesignator().obj.getKind() != Obj.Var && as.getDesignator().obj.getKind() != Obj.Elem) {
            report_error("Designator nije tipa promeljive ili elementa niza", as);
            return;
        }
        if (!areTypesCompatible(as.getDesignator().obj.getType(), as.getExpr().struct)) {
            report_error("Designator i Expr nisu kompatibilni", as);
        }

    }
    public void visit(QuestionStatement qs){
        if(qs.getDesignator().obj.getType()!=Tab.intType || qs.getExpr().struct!=Tab.intType){
            report_error("Designator ili expr nisu tipa int", qs);
        }
    }
    public void visit(IncrementStatement is) {
        if (is.getDesignator().obj.getKind() != Obj.Var && is.getDesignator().obj.getKind() != Obj.Elem) {
            report_error("Designator nije tipa promeljive ili elementa niza", is);
            return;
        }
        if (is.getDesignator().obj.getType() != Tab.intType) {
            report_error("Designator nije tipa int", is);
        }
    }

    public void visit(DecrementStatement ds) {
        if (ds.getDesignator().obj.getKind() != Obj.Var && ds.getDesignator().obj.getKind() != Obj.Elem) {
            report_error("Designator nije tipa promeljive ili elementa niza", ds);
            return;
        }
        if (ds.getDesignator().obj.getType() != Tab.intType) {
            report_error("Designator nije tipa int", ds);
        }
    }

    private boolean isNumericType(Struct type) {
        if (type == null)
            return false;
        int kind = type.getKind();
        return kind == Struct.Int || kind == Struct.Char || kind == MyTab.BoolKind;
    }

    // Statement
    public void visit(ReadStatement rs) {
        if (rs.getDesignator().obj.getKind() != Obj.Var && rs.getDesignator().obj.getKind() != Obj.Elem) {
            report_error("Identifikator designator nije tipa promeljive ili elementa niza", rs);
            return;
        }
        if (!isNumericType(rs.getDesignator().obj.getType())) {
            report_error("Designator nije tipa int, char ili bool", rs);
        }
    }

    public void visit(PrintStatement ps) {
        if (!isNumericType(ps.getExpr().struct)) {
            report_error("Expr nije tipa int, char ili bool.", ps);
        }
    }

    public void visit(PrintNumStatement pns) {
        if (!isNumericType(pns.getExpr().struct)) {
            report_error("Expr nije tipa int, char ili bool.", pns);
        }
    }

    public void visit(FindAnyStatement fas) {
          Designator to = fas.getDesignator();    // leva strana (bool)
    Designator from = fas.getDesignator1(); // desna strana (niz)

    // Provera da li su obj postavljeni
    if (to.obj == null || to.obj == Tab.noObj) {
        report_error("Levi designator nema ispravan objekat", to);
        return;
    }
    if (from.obj == null || from.obj == Tab.noObj) {
        report_error("Desni designator nema ispravan objekat", from);
        return;
    }
       // System.out.println(fas.getDesignator1().obj.getType().getKind());
        if (fas.getDesignator1().obj.getType().getKind() != Struct.Array) {
            report_error("Designator1 nije tipa array", fas);
        }
        if (fas.getDesignator().obj.getType().getKind() != MyTab.BoolKind) {
            report_error("Designator nije tipa bool ", fas);
        }
    }

    public boolean passed() {
        return !errorDetected;
    }

}
