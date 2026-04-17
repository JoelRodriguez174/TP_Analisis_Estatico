import java_cup.runtime.*;
%%
%class AnalizadorLexicoCUP
%public
%line
%column
%cup
%{
    private Symbol symbol(int type) {
        return new Symbol(type, yyline, yycolumn);
    }
    
    private Symbol symbol(int type, Object value) {
        return new Symbol(type, yyline, yycolumn, value);
    }
%}
%%
[ \t\r\n]+                     { /* ignorar espacios */ }
"int"                          { return symbol(sym.INT, yytext()); }
"bool"                         { return symbol(sym.BOOL, yytext()); }
"void"                         { return symbol(sym.VOID, yytext()); }
"main"                         { return symbol(sym.MAIN, yytext()); }
"return"                       { return symbol(sym.RETURN, yytext()); }
"if"                           { return symbol(sym.IF, yytext()); }
"else"                         { return symbol(sym.ELSE, yytext()); }
"while"                        { return symbol(sym.WHILE, yytext()); }
"true"                         { return symbol(sym.TRUE, yytext()); }
"false"                        { return symbol(sym.FALSE, yytext()); }
"="                            { return symbol(sym.OPERADOR_ASIGNACION, yytext()); }
"+"                            { return symbol(sym.OPERADOR_SUMA, yytext()); }
"*"                            { return symbol(sym.OPERADOR_MULTIPLICACION, yytext()); }
"("                            { return symbol(sym.PARENTESIS_ABRE, yytext()); }
")"                            { return symbol(sym.PARENTESIS_CIERRA, yytext()); }
"{"                            { return symbol(sym.LLAVE_ABRE, yytext()); }
"}"                            { return symbol(sym.LLAVE_CIERRA, yytext()); }
";"                            { return symbol(sym.PUNTO_COMA, yytext()); }
[0-9]+("."[0-9]+)?             { return symbol(sym.NUMERO, yytext()); }
[A-Za-z][A-Za-z0-9]* { return symbol(sym.IDENTIFICADOR, yytext()); }
.                              { System.err.println("Carácter ilegal: " + yytext()); }
<<EOF>>                        { return symbol(sym.EOF); }