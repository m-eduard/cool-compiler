parser grammar CoolParser;

options {
    tokenVocab = CoolLexer;
}

@header{
    package cool.parser;
}

program
    :   (classes+=class SEMI)* EOF
    ;

class :
    CLASS name=TYPE LBRACE (features+=feature SEMI)* RBRACE
    | CLASS name=TYPE INHERITS base+=(TYPE | ID) LBRACE (features+=feature SEMI)* RBRACE;

feature:
    name=ID LPAREN (formals+=formal (COMMA formals+=formal)*)* RPAREN
        COLON type=(TYPE | ID) LBRACE body=expr RBRACE          #methodDef
    | lval=formal (ASSIGN init=expr)?                           #memberDef;

formal:
    name=ID COLON type=TYPE;

localVar:
    lval=formal (ASSIGN e=expr)?;

expr:
    // these 2 distinct rules for dispatch are needed to achieve direct left recursion (otherwise,
    // for (obj=expr)? at the beginning of the rule, ANTLR would generate an error because it cannot
    // handle indirect left recursion)
    obj=expr (AT t=TYPE)? POINT name=ID LPAREN
        (args+=expr (COMMA args+=expr)*)? RPAREN                # explicitDispatch
    | name=ID LPAREN (args+=expr (COMMA args+=expr)*)? RPAREN   # implicitDispatch
    | UMINUS e=expr                                             # uMinus
    | NEW t=TYPE                                                # new
    | ISVOID e=expr                                             # isVoid
    | left=expr op=(MULT | DIV) right=expr                      # multDiv
    | left=expr op=(PLUS | MINUS) right=expr                    # plusMinus
    | left=expr op=(LT | LE | EQUAL) right=expr                 # relational    // relation is more prioritary than not
    | NOT e=expr                                                # not
    | LPAREN nested=expr RPAREN                                 # nested
    | LET localVars+=localVar (COMMA localVars+=localVar)*
        IN body=expr                                            # let
    | name=ID ASSIGN e=expr                                     # assign
    | CASE e=expr OF (lval+=formal ARROW rval+=expr SEMI)+ ESAC # case
    | IF cond=expr THEN thenBranch=expr ELSE elseBranch=expr FI # if
    | WHILE cond=expr LOOP body=expr POOL                       # while
    | LBRACE (expressions+=expr SEMI)+ RBRACE                   # block
    | ID                                                        # id
    | INT                                                       # int
    | STRING                                                    # string
    | BOOL                                                      # bool;
