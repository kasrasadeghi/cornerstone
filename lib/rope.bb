;========== Rope ===================================================================================
;
; This file has everything to do with navigating a StringView using lines and columns.
; It has the following fundamental structures:
; - TextCoord
; - Rope
;
; TODO:
; - Rope
; - TextCoord
;
; DONE:
; - @TextCoord.vector-print

(def @TextCoord.vector-print (params (%L-line u64) (%L-col u64) (%R-line u64) (%R-col u64)) void (do
  (call @u64.print (args %L-line))
  (call @i8$ptr.unsafe-print (args ",\00"))
  (call @u64.print (args %L-col))

  (call @i8$ptr.unsafe-print (args " -> \00"))

  (call @u64.print (args %R-line))
  (call @i8$ptr.unsafe-print (args ",\00"))
  (call @u64.print (args %R-col))
  (return-void)
))

(def @TextCoord.vector-println (params (%L-line u64) (%L-col u64) (%R-line u64) (%R-col u64)) void (do
  (call @TextCoord.vector-print (args %L-line %L-col %R-line %R-col))
  (call @println args)
  (return-void)
))

(def @TextCoord.lexically-compare (params (%L-line u64) (%L-col u64) (%R-line u64) (%R-col u64)) i1 (do
  (if (< %L-line %R-line) (do
    (return true)
  ))
  (if (> %L-line %R-line) (do
    (return false)
  ))

; TODO correctness-assert (== %L-line %R-line)

  (if (< %L-col %R-col) (do
    (return true)
  ))
  (if (> %L-col %R-col) (do
    (return false)
  ))

; TODO correctness-assert (== %L-col %R-col)

  (return false)
))