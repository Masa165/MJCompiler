package rs.ac.bg.etf.pp1;



import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Scope;
import rs.etf.pp1.symboltable.concepts.Struct;

public class MyTab extends Tab{
     public static final int BoolKind = 5;
    public static final Struct boolType = new Struct(BoolKind);
    public static final Struct intArrType = new Struct(Struct.Array, intType);
    public static final Struct charArrType = new Struct(Struct.Array, charType);
    public static final Struct boolArrType = new Struct(Struct.Array, boolType);
    public static void init(){
        System.out.println("MyTab.init() called");
    Tab.init();
    System.out.println("After Tab.init(), currentScope = " + currentScope);
      
        currentScope.addToLocals(new Obj(Obj.Type, "bool", boolType));
        currentScope.addToLocals(new Obj(Obj.Con, "true", boolType, 1, 0));
        currentScope.addToLocals(new Obj(Obj.Con, "false", boolType, 0, 0));
    }
    public static Obj insert(int kind, String name, Struct type, int adr, int level) {
		// create a new Object node with kind, name, type
		Obj newObj = new Obj(kind, name, type, adr, level); 
		
		// append the node to the end of the symbol list
		if (!currentScope.addToLocals(newObj)) {
			Obj res = currentScope.findSymbol(name);
			return (res != null) ? res : noObj;
		}
		else 
			return newObj;
	}
    public static Obj find(String name, Scope scope) {
		Obj resultObj = null;
		for (Scope s = scope; s != null; s = s.getOuter()) {
			if (s.getLocals() != null) {
				resultObj = s.getLocals().searchKey(name);
				if (resultObj != null) break;
			}
		}
		return (resultObj != null) ? resultObj : noObj;
	}
}
