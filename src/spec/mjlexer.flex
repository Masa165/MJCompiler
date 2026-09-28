
package rs.ac.bg.etf.pp1;

import java_cup.runtime.Symbol;

%%

%{

	// ukljucivanje informacije o poziciji tokena
	private Symbol new_symbol(int type) {
		return new Symbol(type, yyline+1, yycolumn);
	}
	
	// ukljucivanje informacije o poziciji tokena
	private Symbol new_symbol(int type, Object value) {
		return new Symbol(type, yyline+1, yycolumn, value);
	}

%}

//%cup
%implements	java_cup.runtime.Scanner
%function	next_token
%type		java_cup.runtime.Symbol
%yylexthrow java.io.IOException
%eofval{
	return new_symbol(sym.EOF);
%eofval}

%line
%column
%xstate COMMENT

%%

" " 	{ }
"\b" 	{ }
"\t" 	{ }
"\r\n" 	{ }
"\f" 	{ }


// ---------- Keywords ----------
"program"   { return new_symbol(sym.PROG, yytext()); }    
//"break"     { return new_symbol(sym.BREAK, yytext()); }
//"enum"      { return new_symbol(sym.ENUM, yytext()); }
//"class"     { return new_symbol(sym.CLASS, yytext()); }
//"abstract"  { return new_symbol(sym.ABSTRACT, yytext()); }
//"else"      { return new_symbol(sym.ELSE, yytext()); }    
"const"     { return new_symbol(sym.CONST, yytext()); }
//"if"        { return new_symbol(sym.IF, yytext()); }      
"new"       { return new_symbol(sym.NEW, yytext()); }
"print"     { return new_symbol(sym.PRINT, yytext()); }    
"read"      { return new_symbol(sym.READ, yytext()); }
//"return"    { return new_symbol(sym.RETURN, yytext()); }  
"void"      { return new_symbol(sym.VOID, yytext()); }     
//"extends"   { return new_symbol(sym.EXTENDS, yytext()); }
//"continue"  { return new_symbol(sym.CONTINUE, yytext()); }
//"for"       { return new_symbol(sym.FOR, yytext()); }
"length"    { return new_symbol(sym.LENGTH, yytext()); }
//"switch"    { return new_symbol(sym.SWITCH, yytext()); }
//"case"      { return new_symbol(sym.CASE, yytext()); }
"findAny"      { return new_symbol(sym.FINDANY, yytext()); }


// ---------- Multi‑character operators ----------
"=="        { return new_symbol(sym.EQ, yytext()); }
"!="        { return new_symbol(sym.NE, yytext()); }
">="        { return new_symbol(sym.GE, yytext()); }
"<="        { return new_symbol(sym.LE, yytext()); }
//"&&"        { return new_symbol(sym.AND, yytext()); }
//"||"        { return new_symbol(sym.OR, yytext()); }
"++"        { return new_symbol(sym.INC, yytext()); }
"--"        { return new_symbol(sym.DEC, yytext()); }
"??"        {return new_symbol(sym.QUESTIONQUESTION, yytext());}
"?="        {return new_symbol(sym.QUESTIONEQUAL, yytext());}
"[^"         { return new_symbol(sym.LBRACKETCAP, yytext());}


// ---------- Single‑character operators ----------
"+"         { return new_symbol(sym.PLUS, yytext()); }
"-"         { return new_symbol(sym.MINUS, yytext()); }
"*"         { return new_symbol(sym.STAR, yytext()); }
"/"         { return new_symbol(sym.DIV, yytext()); }
"%"         { return new_symbol(sym.MOD, yytext()); }
"="         { return new_symbol(sym.EQUAL, yytext()); }
">"         { return new_symbol(sym.GT, yytext()); }
"<"         { return new_symbol(sym.LT, yytext()); }
";"         { return new_symbol(sym.SEMI, yytext()); }
":"         { return new_symbol(sym.COLON, yytext()); }
","         { return new_symbol(sym.COMMA, yytext()); }
"."         { return new_symbol(sym.DOT, yytext()); }
"("         { return new_symbol(sym.LPAREN, yytext()); }
")"         { return new_symbol(sym.RPAREN, yytext()); }
"["         { return new_symbol(sym.LBRACKET, yytext()); }
"]"         { return new_symbol(sym.RBRACKET, yytext()); }
"{"         { return new_symbol(sym.LBRACE, yytext()); }
"}"         { return new_symbol(sym.RBRACE, yytext()); }
"?"         { return new_symbol(sym.QUESTION, yytext()); }



"//" {yybegin(COMMENT);}
<COMMENT> . {yybegin(COMMENT);}
<COMMENT> "\r\n" { yybegin(YYINITIAL); }


// ---------- Boolean constants ----------
"true"      { return new_symbol(sym.TRUE, 1); }
"false"     { return new_symbol(sym.FALSE, 0); }


// ---------- Numeric constant ----------
[0-9]+      { return new_symbol(sym.NUMBER, new Integer(yytext())); }


// ---------- Character constant ----------
// Karakterne konstante: 'a', '\n', '\t', ...
\'([^'\\]|\\.)\' {
    String s = yytext();
    char ch;
    if (s.length() == 3) {
        // Običan znak: 'a', 'Z', '5', ' ' (razmak)
        ch = s.charAt(1);
    } else {
        // Escape sekvenca: '\n', '\t', '\\', '\'', '\"'
        char esc = s.charAt(2);
        switch (esc) {
            case 'n': ch = '\n'; break;
            case 't': ch = '\t'; break;
            case 'r': ch = '\r'; break;
            case 'b': ch = '\b'; break;
            case 'f': ch = '\f'; break;
            case '0': ch = '\0'; break;
            case '\\': ch = '\\'; break;
            case '\'': ch = '\''; break;
            case '\"': ch = '\"'; break;
            default:
                // Nepoznata escape sekvenca – prijavi grešku
                System.out.println("Leksicka greska pri definiciji karaktera: '" + yytext() + "' u liniji " + (yyline+1) + ", kolona " + (yycolumn+1));
                return new_symbol(sym.error, yytext());
        }
    }
    return new_symbol(sym.CHARCONST, (int) ch);
}


// ---------- Identifier ----------
[a-zA-Z][a-zA-Z0-9_]* { return new_symbol(sym.IDENT, yytext()); }


. { System.out.println("Leksicka greska: '" + yytext() + "' u liniji " + (yyline+1) + ", kolona " + (yycolumn+1));
         return new_symbol(sym.error, yytext()); }










