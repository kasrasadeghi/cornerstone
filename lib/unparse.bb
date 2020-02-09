;========== Unparser ==============================================================================
; TODO
; - unparse comments
; - cache value starting position
; - navigate to next printed element

; DONE
; - branch from Texp$ptr.parenPrint
; - print texp while keeping track out writing position

(struct %struct.Unparser
  (%line u64)
  (%col u64)
  (%parser-readref %struct.Parser*)
  (%parser-i u64)
)

(def @Unparser.make (params (%parser %struct.Parser*)) %struct.Unparser (do
  (auto %unparser %struct.Unparser)
  (store 0 (index %unparser 0))
  (store 0 (index %unparser 1))
  (store %parser (index %unparser 2))
  (store 0 (index %unparser 3))
  (return (load %unparser))
))

(def @Unparser$ptr.increment-col (params (%unparser %struct.Unparser*) (%col-delta u64)) void (do
  (store (+ %col-delta (load (index %unparser 1))) (index %unparser 1))
  (return-void)
))

; filling in necessary WS
(def @Unparser$ptr.navigate (params (%unparser %struct.Unparser*) (%line u64) (%col u64)) void (do
  (let %SPACE   (+ 32 (0 i8)))
  (let %NEWLINE (+ 10 (0 i8)))

  (if (== %line (load (index %unparser 0))) (do
    (if (== %col (load (index %unparser 1))) (do
      (return-void)
    ))
    (call @i8.print (args %SPACE))
    (call @Unparser$ptr.increment-col (args %unparser 1))

    (call @Unparser$ptr.navigate (args %unparser %line %col))
    (return-void)
  ))

  (call @i8.print (args %NEWLINE))
  (store (+ 1 (load (index %unparser 0))) (index %unparser 0))
  (store 0 (index %unparser 1))

  (call @Unparser$ptr.navigate (args %unparser %line %col))
  (return-void)
))

(def @unparse-children (params (%unparser %struct.Unparser*) (%texp %struct.Texp*) (%child-index u64)) void (do
  (let %SPACE  (+ 32 (0 i8)))
  (let %SIZEOF-Texp (+ 40 (0 u64)))

  (let %length (load (index %texp 2)))
  (if (== %child-index %length) (do
    (return-void)
  ))

  (let %children (load (index %texp 1)))
  (let %curr (cast %struct.Texp* (+ (* %SIZEOF-Texp %child-index) (cast u64 %children))))

  (call @unparse-texp (args %unparser %curr))
  (call-tail @unparse-children (args %unparser %texp (+ 1 %child-index)))
  (return-void)
))

(def @unparse-texp (params (%unparser %struct.Unparser*) (%texp %struct.Texp*)) void (do
  (if (== 0 (cast u64 %texp)) (do
    (call @i8$ptr.unsafe-println (args "cannot unparse null-texp\00"))
    (call @exit (args 1))
  ))

  (let %value-ref (index %texp 0))
  (let %value-length (load (index %value-ref 1)))
  (let %length (load (index %texp 2)))

; get opening coordinate (line, col) and closing/ending coordinate (eline, ecol)
  (let %parser (load (index %unparser 2)))
  (let %parser-i (index %unparser 3))
  (let %curr-parser-i (load %parser-i))
  (store (+ 1 %curr-parser-i) %parser-i)

  (let %line  (call @u64-vector$ptr.unsafe-get (args (index %parser 2) %curr-parser-i)))
  (let %col   (call @u64-vector$ptr.unsafe-get (args (index %parser 3) %curr-parser-i)))
  (let %eline (call @u64-vector$ptr.unsafe-get (args (index %parser 4) %curr-parser-i)))
  (let %ecol  (call @u64-vector$ptr.unsafe-get (args (index %parser 5) %curr-parser-i)))

  (call @Unparser$ptr.navigate (args %unparser %line %col))

  (if (== 0 %eline) (do
; TODO assert (== %ecol 0)
; WARN "non-normalized texp" if (!= %length 0)
    (call @String$ptr.print (args %value-ref))
    (call @Unparser$ptr.increment-col (args %unparser %value-length))
    (return-void)
  ))

  (let %LPAREN (+ 40 (0 i8)))
  (let %RPAREN (+ 41 (0 i8)))
  (let %SPACE  (+ 32 (0 i8)))

  (call @i8.print (args %LPAREN))
; TODO navigate-to %value-coordinate
  (call @String$ptr.print (args %value-ref))

  (call @Unparser$ptr.increment-col (args %unparser (+ 1 %value-length)))

  (call @unparse-children (args %unparser %texp 0))

  (call @Unparser$ptr.navigate (args %unparser %eline %ecol))
  (call @i8.print (args %RPAREN))
  (call @Unparser$ptr.increment-col (args %unparser 1))

  (return-void)
))

(def @unparse (params (%parser %struct.Parser*) (%texp %struct.Texp*)) void (do
  (auto %unparser %struct.Unparser)
  (store (call @Unparser.make (args %parser)) %unparser)

; use unparse-children so it does attempt to navigate to the program
  (call @unparse-children (args %unparser %texp 0))
  (return-void)
))
