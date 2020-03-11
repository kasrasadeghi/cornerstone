; TODO verify this grammar with parser implementations

(Grammar
  (Texp (| List Atom))
  (List ('(' WS Word (* List-Child) ')'))
  (List-Child (WS Texp))
  (Atom (| String Char Word))
  (Word (| (* Non-Special)))
  (WS   (| ' ' '\f' '\n' '\r' '\t' '\v'))
  
; Non-Special is defined as any ascii character that is not in WS and
; also not left or right parenthesis
  (Non-Special (- All (U WS '(' ')')))
)
