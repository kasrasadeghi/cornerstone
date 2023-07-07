(include "../lib/core.bb")

(include "../backbone/passconfig.bb")
(include "../backbone/includer.bb")

;========== main program ===========================================================================

; (def @print-each-toplevel (params () (%i u64)) void (do
; ))

(def @main (params (%argc i32) (%argv i8**)) i32 (do

  (if (!= 4 %argc) (do
    (call @i8$ptr.unsafe-print (args "USAGE: runner <file> [--single|--until] <pass> for a pass in {\00"))
    (call @StringView.print (args (call @Backbone.get-passlist args)))
    (call @i8$ptr.unsafe-println (args "}\00"))
    (call @exit (args 1))
  ))

  (let %arg-filename     (load (cast i8** (+ 8  (cast u64 %argv)))))
  (let %arg-single-until (load (cast i8** (+ 16 (cast u64 %argv)))))
  (let %arg-passname     (load (cast i8** (+ 24 (cast u64 %argv)))))

  (auto %program %struct.Texp)
  (store (call @Parser.parse-file-i8$ptr (args %arg-filename)) %program)

  (store (call @Includer.on-Program (args %program)) %program)
  (call @Texp$ptr.pretty-print (args %program))

  (return 0)
))
