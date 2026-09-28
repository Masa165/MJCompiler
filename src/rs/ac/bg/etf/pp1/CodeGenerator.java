package rs.ac.bg.etf.pp1;


import rs.ac.bg.etf.pp1.CounterVisitor.FormParamCounter;
import rs.ac.bg.etf.pp1.CounterVisitor.VarCounter;
import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

class CodeGenerator extends VisitorAdaptor {
    
    private int mainPc;

    public int getMainPc() {
        return mainPc;
    }

    public void visit(MethodVoidName methodTypeName){
		
		if("main".equalsIgnoreCase(methodTypeName.getMethodName())){
			mainPc = Code.pc;
		}
		methodTypeName.obj.setAdr(Code.pc);
		// Collect arguments and local variables
		SyntaxNode methodNode = methodTypeName.getParent();
	
		VarCounter varCnt = new VarCounter();
		methodNode.traverseTopDown(varCnt);
		
		FormParamCounter fpCnt = new FormParamCounter();
		methodNode.traverseTopDown(fpCnt);
		
		// Generate the entry
		Code.put(Code.enter);
		Code.put(fpCnt.getCount());
		Code.put(fpCnt.getCount() + varCnt.getCount());
	
	}
	
	public void visit(MethodDeclVoid methodDecl){
		Code.put(Code.exit);
		Code.put(Code.return_);
	}

    // Statement
    public void visit(PrintStatement printStmt) {
        if (printStmt.getExpr().struct == Tab.charType) {
            Code.loadConst(1);
            Code.put(Code.bprint);
        } else {

            // za int i bool !za bool se ispisuje 1 ili 0
            Code.loadConst(5);
            Code.put(Code.print);
        }
    }

    public void visit(PrintNumStatement pns) {
        if (pns.getExpr().struct == Tab.charType) {
            Code.loadConst(pns.getNumConst().obj.getAdr());
            Code.put(Code.bprint);
        } else {

            // za int i bool !za bool se ispisuje 1 ili 0
            Code.loadConst(pns.getNumConst().obj.getAdr());
            Code.put(Code.print);
        }
    }

    public void visit(ReadStatement rs) {
        if(rs.getDesignator().obj.getType()==Tab.charType)Code.put(Code.bread);
        else Code.put(Code.read);
        Code.store(rs.getDesignator().obj);
    }
    public void visit(FindAnyStatement fas){
        //[expr]
        Code.store(SemanticAnalyzer.exprObj);
        Code.load(fas.getDesignator1().obj);
        Code.store(SemanticAnalyzer.adrObj);
        Code.put(Code.const_n);
        Code.store(SemanticAnalyzer.indObj);
        Obj elem=new Obj(Obj.Elem, "",fas.getDesignator1().obj.getType().getElemType() );
        //for
        int start=Code.pc;
        Code.load(SemanticAnalyzer.indObj);//[ind]
        Code.load(SemanticAnalyzer.adrObj);//[ind,adr]
        Code.put(Code.arraylength);//[ind,len]
        int matchNotFound=Code.pc+1;
        Code.putFalseJump(Code.lt, 0);//[]

        //obrada
        Code.load(SemanticAnalyzer.exprObj);
        Code.load(SemanticAnalyzer.adrObj);
        Code.load(SemanticAnalyzer.indObj);//[expr,adr,ind]
        Code.load(elem);//[expr,elemVal]
        int matchFound=Code.pc+1;
        Code.putFalseJump(Code.ne, 0);//[]
        Code.load(SemanticAnalyzer.indObj);
        Code.put(Code.const_1);
        Code.put(Code.add);
        Code.store(SemanticAnalyzer.indObj);
        Code.putJump(start);

        //matchFound
        Code.fixup(matchFound);
        Code.put(Code.const_1);
        Code.store(fas.getDesignator().obj);
        int end=Code.pc+1;
        Code.putJump(0);

        //matchNotFound
        Code.fixup(matchNotFound);
        Code.put(Code.const_n);
        Code.store(fas.getDesignator().obj);

        //end
        Code.fixup(end);
    }

    // Designator
    public void visit(LengthDesignator ld) {
        Obj des = MyTab.find(ld.getIdent(), SemanticAnalyzer.mainScope);
        Code.load(des);
        Code.put(Code.arraylength);
        // [len]
    }

    public void visit(IndexDesingator id) {
        // [index]
        Obj des = MyTab.find(id.getIdent(), SemanticAnalyzer.mainScope);
        Code.load(des); // [index, bazna adr]
        dup_x1();// [bazna adr, index, bazna adr]
        Code.put(Code.pop);// [bazna adr, index]
        if(id.getParent().getClass()==IncrementStatement.class || id.getParent().getClass()==DecrementStatement.class){
            Code.put(Code.dup2);
        }
    }
    public void visit(SIndexDesignator sid){
        //[ind]
        Code.store(SemanticAnalyzer.indObj);
        Obj des = MyTab.find(sid.getIdent(), SemanticAnalyzer.mainScope);
        if(des==Tab.noObj)System.out.println("index no obj");
        Code.load(des); // [bazna adr]
        Code.put(Code.arraylength);//[len]
        Code.put(Code.dup);//[len,len]
        Code.store(SemanticAnalyzer.exprObj);//exprObj<-len;, 
        Code.load(SemanticAnalyzer.indObj);
        Code.put(Code.sub);//[ len-ind];
        Code.store(SemanticAnalyzer.indObj);
        Code.put(Code.const_n);
        Code.store(SemanticAnalyzer.adrObj);//sum

        //
        int loop=Code.pc;
        Code.load(SemanticAnalyzer.indObj);
        Code.load(SemanticAnalyzer.exprObj);
        int end=Code.pc+1;
        Code.putFalseJump(Code.lt, 0);
        //telo
        Code.load(des);
        Code.load(SemanticAnalyzer.indObj);
        Code.put(Code.aload+((des.getType().getElemType()==Tab.charType)?2:0));
        Code.load(SemanticAnalyzer.adrObj);
        Code.put(Code.add);
        Code.store(SemanticAnalyzer.adrObj);

        Code.load(SemanticAnalyzer.indObj);
        Code.put(Code.const_1);
        Code.put(Code.add);
        Code.store(SemanticAnalyzer.indObj);

        
        Code.putJump(loop);
        Code.fixup(end);

        Code.load(SemanticAnalyzer.adrObj);


    }

    // Factor
    public void visit(NumFact nf) {
        Code.loadConst(nf.getNumConst().obj.getAdr());
    }

    public void visit(CharFact cf) {
        Code.loadConst(cf.getCharConst().obj.getAdr());
    }

    public void visit(BoolFact bf) {
        Code.loadConst(bf.getBoolConst().obj.getAdr());
    }

    public void visit(DesignatorFact designatorFact) {
        Designator d = designatorFact.getDesignator();
        if (d instanceof LengthDesignator || d instanceof SIndexDesignator) {
            return;
        }
        Code.load(d.obj);
    }

    

    public void visit(CreationFactor creationFactor) {
        // [n]
        Code.put(Code.newarray);
        Code.loadConst((creationFactor.getType().struct.getKind() == Struct.Char) ? 0 : 1);
        // [adr niza]
    }

    // Term
    
    public void visit(FactorsTerm ft) {
        // [termexpr, factorexpr]
        Mulop mulop = ft.getMulop();
        if (mulop instanceof StarOper)
            Code.put(Code.mul);
        else if (mulop instanceof DivOper)
            Code.put(Code.div);
        else if (mulop instanceof ModOper)
            Code.put(Code.rem);
        // [termexpr]
    }

    // TermExpr
    public void visit(MinusTermExpr mte) {
        Code.put(Code.neg);
    }

    public void visit(TermsExpr te) {
        Addop addop = te.getAddop();
        if (addop instanceof PlusOper)
            Code.put(Code.add);
        else if (addop instanceof MinusOper)
            Code.put(Code.sub);
    }

    // CondFact
    public void visit(CondFactRel node) {

        Relop relop = node.getRelop();
        int op;
        if (relop instanceof Equal)
            op = Code.eq;
        else if (relop instanceof NotEqual)
            op = Code.ne;
        else if (relop instanceof Greater)
            op = Code.gt;
        else if (relop instanceof GreaterEq)
            op = Code.ge;
        else if (relop instanceof Less)
            op = Code.lt;
        else if (relop instanceof LessEqual)
            op = Code.le;
        else
            op = -1;

        // Generiši uslovni skok
        int patchFalse = Code.pc+1;
        Code.putFalseJump(op, 0); // skok ako je false (adresa će se popuniti)

        // true, preskoci uslovnu granu
        Code.put(Code.const_1);
        int patchEnd = Code.pc+1;
        Code.putJump(0); 

        // === label_false ===
        Code.fixup(patchFalse); 
        Code.put(Code.const_n + 0); // stavi 0
        
        Code.fixup(patchEnd);
    }
    //Expr
    public void visit(CondExpr ce){
        //[cond,exp1,exp2]
        dup_x2();
        Code.put(Code.pop);
        dup_x2();
        Code.put(Code.pop);
        //[exp1,exp2,cond]

        Code.put(Code.const_1);
        int patch=Code.pc+1;
        Code.putFalseJump(Code.eq, 0);
        //[exp1,exp2]
        //true
        Code.put(Code.pop);
        int patch2=Code.pc+1;
        Code.putJump(0);
        Code.fixup(patch);
        //false
        dup_x1();//[exp2,exp1,exp2]
        Code.put(Code.pop);
        Code.put(Code.pop);//[exp2]
        Code.fixup(patch2);
    }
    public void visit(ExprQuestion eq){
       // [expr1, expr2]
       Code.store(SemanticAnalyzer.val2Obj);
       Code.put(Code.dup);
       Code.put(Code.const_n);
       int adr=Code.pc+1;
       Code.putFalseJump(Code.eq, 0);
       Code.put(Code.pop);
       Code.load(SemanticAnalyzer.val2Obj);
       Code.fixup(adr);
    }
    //DesignatorStatement
    public void visit(QuestionStatement qs){
        //[expr]
        Code.load(qs.getDesignator().obj);
        Code.put(Code.const_n);
        int adr=Code.pc+1;
        Code.putFalseJump(Code.ne, 0);
        Code.put(Code.pop);
        int adr1=Code.pc+1;
        Code.putJump(0);
        Code.fixup(adr);
        Code.store(qs.getDesignator().obj);
        Code.fixup(adr1);

    }
    public void visit(AssignmentStatement as){
        Code.store(as.getDesignator().obj);
    }
    public void visit(IncrementStatement is){
       Code.load(is.getDesignator().obj);
       Code.put(Code.const_1);
       Code.put(Code.add);
       Code.store(is.getDesignator().obj);
    }
    public void visit(DecrementStatement is){
       Code.load(is.getDesignator().obj);
       Code.put(Code.const_1);
       Code.put(Code.sub);
       Code.store(is.getDesignator().obj);
    }
    private void dup_x1(){
        //[val2, val1]
        Code.store(SemanticAnalyzer.val1Obj);
        Code.store(SemanticAnalyzer.val2Obj);

        Code.load(SemanticAnalyzer.val1Obj);
        Code.load(SemanticAnalyzer.val2Obj);
        Code.load(SemanticAnalyzer.val1Obj);
    }
    private void dup_x2(){
        //[val1,val2,val3]
        Code.store(SemanticAnalyzer.val3Obj);
        Code.store(SemanticAnalyzer.val2Obj);
        Code.store(SemanticAnalyzer.val1Obj);

        Code.load(SemanticAnalyzer.val3Obj);
        Code.load(SemanticAnalyzer.val1Obj);
        Code.load(SemanticAnalyzer.val2Obj);
        Code.load(SemanticAnalyzer.val3Obj);



    }

}