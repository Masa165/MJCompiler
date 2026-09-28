package rs.ac.bg.etf.pp1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.log4j.Logger;

import java_cup.runtime.Symbol;
import rs.ac.bg.etf.pp1.ast.Program;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;

public class Compiler {
	public static void tsdump(){
		Tab.dump(new MyDumpVisitor());
	}
    public static void main(String[] args) throws Exception {
        Logger log = Logger.getLogger(Compiler.class);
        if(args.length<=0){
            System.out.println("Nema prosledjenih argumenata.");
            return;
        }
		Reader br = null;
		try {
			File sourceCode = new File(args[0]);
		
			log.info("Compiling source file: " + sourceCode.getAbsolutePath());
			
			br = new BufferedReader(new FileReader(sourceCode));
			Yylex lexer = new Yylex(br);
			
			parser p = new parser(lexer);
	        Symbol s = p.parse();  //pocetak parsiranja
	        
	        Program prog = (Program)(s.value); 
			// ispis sintaksnog stabla
			MyTab.init();
			log.info(prog.toString(""));
			log.info("===================================");

			// ispis prepoznatih programskih konstrukcija
			
			SemanticAnalyzer v = new SemanticAnalyzer();
			prog.traverseBottomUp(v); 
			tsdump();
			if(v.passed()){
				System.out.println("Parsiranje uspesno zavrseno");
				File objFile=new File(args[1]);
				if(objFile.exists())objFile.delete();
				CodeGenerator generator=new CodeGenerator();
				prog.traverseBottomUp(generator);
				Code.dataSize=v.nVars;
				Code.mainPc=generator.getMainPc();
				Code.write(new FileOutputStream(objFile));

			}else{
				System.out.println("Parsiranje NIJE uspesno zavrseno");
			}	
	
		} 
		finally {
			if (br != null) try { br.close(); } catch (IOException e1) { log.error(e1.getMessage(), e1); }
		}
    }
}
