package rs.ac.bg.etf.pp1;

import rs.etf.pp1.symboltable.concepts.Struct;
import rs.etf.pp1.symboltable.visitors.DumpSymbolTableVisitor;

public class MyDumpVisitor extends DumpSymbolTableVisitor {
    @Override
    public void visitStructNode(Struct struct) {
        if(struct.getKind()==MyTab.BoolKind) {
		    output.append("bool");
            return;
        }
        if(struct.getKind()==Struct.Array && struct.getElemType().getKind()==MyTab.BoolKind){
            output.append("Arr of bool");
            return;
        }
        super.visitStructNode(struct);
    }
}