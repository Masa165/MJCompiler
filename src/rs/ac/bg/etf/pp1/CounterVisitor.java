package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.ArrFormPar;
import rs.ac.bg.etf.pp1.ast.ArrFormPars;
import rs.ac.bg.etf.pp1.ast.ArrayVar;
import rs.ac.bg.etf.pp1.ast.ArrayVarDecls;
import rs.ac.bg.etf.pp1.ast.SimpleFormPar;
import rs.ac.bg.etf.pp1.ast.SimpleFormPars;
import rs.ac.bg.etf.pp1.ast.SimpleVar;
import rs.ac.bg.etf.pp1.ast.SimpleVarDecls;
import rs.ac.bg.etf.pp1.ast.VisitorAdaptor;

public class CounterVisitor extends VisitorAdaptor {

	protected int count;
	
	public int getCount(){
		return count;
	}
	
	public static class FormParamCounter extends CounterVisitor{
	
		public void visit(SimpleFormPar formParamDecl){
			count++;
		}
        public void visit(ArrFormPar formParamDecl){
			count++;
		}
        public void visit(SimpleFormPars formParamDecl){
			count++;
		}
        public void visit(ArrFormPars formParamDecl){
			count++;
		}
	}
	
	public static class VarCounter extends CounterVisitor{
		
		public void visit(SimpleVar varDecl){
			count++;
		}
        public void visit(ArrayVar varDecl){
			count++;
		}
        public void visit(SimpleVarDecls varDecl){
			count++;
		}
        public void visit(ArrayVarDecls varDecl){
			count++;
		}
	}
}
