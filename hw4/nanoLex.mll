{
  open Nano        (* nano.ml *)
  open NanoParse   (* nanoParse.ml from nanoParse.mly *)
}

rule token = parse
  | eof                                 { EOF }

  | [' ' '\t' '\r' '\n']                { token lexbuf }
  | "true"                              { TRUE }
  | "false"                             { FALSE }
  | "let"	                              { LET }
  | "rec"	                              { REC }
  | '='                                 { EQ }
  | "in"                                { IN }
  | "fun"                               { FUN }
  | "->"                                { ARROW }
  | "if"                                { IF }
  | "then"                              { THEN }
  | "else"                              { ELSE }

  | '+'                                 { PLUS }
  | '-'                                 { MINUS }
  | '*'                                 { MUL }
  | '/'                                 { DIV }
  | '<'                                 { LT }
  | "<=" 	                              { LE }
  | "!=" 	                              { NE }
  | "&&" 	                              { AND }
  | "||" 	                              { OR }

  | '('                                 { LPAREN }
  | ')'                                 { RPAREN }

  | ['0'-'9']+ as l                     { Num (int_of_string l) }
  | ['A'-'z']['A'-'z' '0'-'9']* as l    { Id l }
  | _                                   { raise (MLFailure ("Illegal Character '"^(Lexing.lexeme lexbuf)^"'")) }
