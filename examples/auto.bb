(include "core.bb")

;========== main program ==========================================================================

(def @main params i32 (do

; int a, b;
  (auto %a u64)
  (auto %b u64)

; a = 5; b = 6;
  (store 5 %a)
  (store 6 %b)

; print 4
  (call @u64.println (args 4))

; print a and b as local variables
  (call @u64.println (args (load %a)))
  (call @u64.println (args (load %b)))

; values: similar to constexpr or const
; note: no lexical scope
; int c = 5 + 6;
; print c
  (let %c (+ (5 u64) 6))
  (call @u64.println (args %c))

; a = b;
  (store (load %b) %a)

; print again after modifications
  (call @println args)
  (call @u64.println (args (load %a)))
  (call @u64.println (args (load %b)))
  
  (return 0)
))
