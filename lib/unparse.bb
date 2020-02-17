;========== Unparser ==============================================================================
; TODO
; - unparse comments
; - cache value starting position
; - navigate to next printed element

; DONE
; - branch from Texp$ptr.parenPrint
; - print texp while keeping track out writing position

(struct %struct.Unparser

; 0
  (%line u64)
  (%col u64)
  (%parser-readref %struct.Parser*)

; 3
  (%syntax-i u64)
  (%file %struct.File)
  (%reader %struct.Reader)

; 6
  (%comment-i u64)
  (%comment-count u64)
)

(def @Unparser.make.count-comments (params (%unparser %struct.Unparser*) (%curr-i u64)) u64 (do
  (let %parser (load (index %unparser 2)))
  (let %type (call @u64-vector$ptr.unsafe-get (args (index %parser 3) %curr-i)))

; assert (!= %type 3)

  (if (!= %type 3) (do (return %curr-i)))

  (return (call @Unparser.make.count-comments (args %unparser (+ 1 %curr-i))))
))

(def @Unparser.make (params (%parser %struct.Parser*)) %struct.Unparser (do
  (auto %unparser %struct.Unparser)
  (store 0 (index %unparser 0))
  (store 0 (index %unparser 1))
  (store %parser (index %unparser 2))

; indices
  (store 0 (index %unparser 6))
  (let %comment-count (call @Unparser.make.count-comments (args %unparser 0)))

; cache %comment-count in %unparser for iterator.done checks later in lazy merge
  (store %comment-count (index %unparser 7))

; start parser-i@3 at the first non-comment
  (store %comment-count (index %unparser 3))

  (let %filename (index %parser 4))
  (auto %file %struct.File)
  (store (call @File.open (args %filename)) %file)

  (auto %content %struct.StringView)
  (store (call @File$ptr.read (args %file)) %content)
  (call @Reader$ptr.set (args (index %unparser 5) %content))

  (return (load %unparser))
))

(def @Unparser$ptr.increment-col (params (%unparser %struct.Unparser*) (%col-delta u64)) void (do
; debug
  (call @i8$ptr.unsafe-print (args "[ inc-col ] +\00"))
  (call @u64.println (args %col-delta))

  (store (+ %col-delta (load (index %unparser 1))) (index %unparser 1))
  (return-void)
))

(def @Unparser$ptr.print-comment (params (%unparser %struct.Unparser*)) void (do
  (let %NEWLINE (+ 10 (0 i8)))
  (let %reader (index %unparser 5))

  (let %save-line (load (index %reader 3)))
  (let %save-col  (load (index %reader 4)))

  (call @Reader$ptr.find-next (args %reader %NEWLINE))

  (let %end-line (load (index %reader 3)))
  (let %end-col  (load (index %reader 4)))

; TODO correctness-assert (== end-line save-line)
; debug
  (call @i8$ptr.unsafe-print (args "[ print-comment ] ==? \00"))
  (call @TextCoord.vector-println (args %save-line %save-col %end-line %end-col))

; you can't just set the column, you have to seek to the correct place
  (store %save-col (index %reader 4))
  (let %comment-begin (load (index %reader 1)))
  (let %comment-length (- %end-col %save-col))
  (call @i8$ptr.printn (args %comment-begin %comment-length))
  (call @Unparser$ptr.increment-col (args %unparser %comment-length))
  (return-void)
))

; filling in necessary WS
(def @Unparser$ptr.navigate (params (%unparser %struct.Unparser*) (%line u64) (%col u64)) void (do
  (let %SPACE   (+ 32 (0 i8)))
  (let %NEWLINE (+ 10 (0 i8)))

  (let %line-ref (index %unparser 0))
  (let %col-ref  (index %unparser 1))

; if we are navigating to something before us, fail
  (if (call @TextCoord.lexically-compare (args %line %col (load %line-ref) (load %col-ref))) (do
    (call @i8$ptr.unsafe-print (args "error: cannot navigate backwards from the cursor: "))
    (call @TextCoord.vector-println (args (load %line-ref) (load %col-ref) %line %col))
    (call @exit (args 1))
  ))

; debug
  (call @TextCoord.vector-println (args (load %line-ref) (load %col-ref) %line %col))

  (if (< %line (load %line-ref)) (do
    (call @TextCoord.vector-println (args (load %line-ref) (load %col-ref) %line %col))
  ))

  (if (== %line (load %line-ref)) (do
    (if (== %col (load %col-ref)) (do
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

; returns true if L < R within lexical ordering

; gets next value from lazy merge of %comment-i and %parser-i
; - sets %error to true on error
(def @Unparser$ptr.pop (params (%unparser %struct.Unparser*) (%index-out u64*) (%error i1*)) void (do

; SOON actually increment iterators

  (store false %error)

  (store 0 %index-out)
  (let %parser (load (index %unparser 2)))

  (let %syntax-i (index %unparser 3))
  (let %comment-i (index %unparser 6))

  (let %lines (index %parser 1))
  (let %cols  (index %parser 2))
  (let %types (index %parser 3))

  (let %comment-count (load (index %unparser 7)))
  (let %syntax-count (load (index %lines 2)))

; debug
  (call @i8$ptr.unsafe-print (args " [ pop ] %syntax-i=\00"))
  (call @u64.print (args (load %syntax-i)))
  (call @i8$ptr.unsafe-print (args "/\00"))
  (call @u64.print (args %syntax-count))

  (call @i8$ptr.unsafe-print (args ", %comment-i=\00"))
  (call @u64.print (args (load %comment-i)))

  (call @i8$ptr.unsafe-print (args "/\00"))
  (call @u64.println (args %comment-count))

  (let %comment-exhausted (== %comment-count (load %comment-i)))
  (let %syntax-exhausted (== %syntax-count (load %syntax-i)))

  (if %syntax-exhausted (do
    (if %comment-exhausted (do
      (store true %error)
      (return-void)
    ))
  ))

  (if %syntax-exhausted (do
    (if (== false %comment-exhausted) (do
      (store (load %comment-i) %index-out)
      (return-void)
    ))
  ))

  (if %comment-exhausted (do
    (if (== false %syntax-exhausted) (do
      (store (load %syntax-i) %index-out)
      (return-void)
    ))
  ))

  (call @i8$ptr.unsafe-println (args " [ pop ] neither exhausted\00"))  

; neither the syntax tokens nor the comment tokens are exhausted
; -> lexically compare coordinates to find which one is next

  (let %s-line (call @u64-vector$ptr.unsafe-get (args %lines (load %syntax-i))))
  (let %s-col  (call @u64-vector$ptr.unsafe-get (args %cols  (load %syntax-i))))

  (let %c-line (call @u64-vector$ptr.unsafe-get (args %lines (load %comment-i))))
  (let %c-col  (call @u64-vector$ptr.unsafe-get (args %cols  (load %comment-i))))

; TODO parser-correctness assert (not (and (== s-line c-line) (== s-col c-col)))

  (let %syntax-is-less-than (call @TextCoord.lexically-compare (args %s-line %s-col %c-line %c-col)))

  (if %syntax-is-less-than (do
    (store (load %syntax-i) %index-out)
  ))

  (if (== false %syntax-is-less-than) (do
    (store (load %comment-i) %index-out)
  ))

  (call @i8$ptr.unsafe-print (args " [ pop ] result: \00"))
  (call @u64.println (args (load %index-out)))

  (return-void)
))



(def @Unparser$ptr.pop-to-value (params (%unparser %struct.Unparser*)) void (do

  (let %parser (load (index %unparser 2)))

  (auto %current-index u64)
  (auto %error i1)
  (call @Unparser$ptr.pop (args %unparser %current-index %error))

  (if (load %error) (do
    (call @i8$ptr.unsafe-println (args "error: pop-to-value: iterators exhausted\00"))
    (call @exit (args 1))
  ))

  (let %line (call @u64-vector$ptr.unsafe-get (args (index %parser 1) (load %current-index))))
  (let %col  (call @u64-vector$ptr.unsafe-get (args (index %parser 2) (load %current-index))))
  (let %type (call @u64-vector$ptr.unsafe-get (args (index %parser 3) (load %current-index))))

  (call @Unparser$ptr.navigate (args %unparser %line %col))

; source: top of parser.bb
; 0 '('
; 1 ')'
; 2 value
; 3 comment

  (let %LPAREN (+ 40 (0 i8)))
  (let %RPAREN (+ 41 (0 i8)))
  (let %SPACE  (+ 32 (0 i8)))

  (if (== %type 3) (do
    (call @Reader$ptr.reset (args (index %unparser 5)))
    (call @Reader$ptr.seek-forward (args (index %unparser 5) %line %col))
    (call @Unparser$ptr.print-comment (args %unparser))
    (call @Unparser$ptr.pop-to-value (args %unparser))
    (return-void)
  ))

  (if (== %type 0) (do
    (call @i8.print (args %RPAREN))
    (call @Unparser$ptr.increment-col (args %unparser 1))
    (call @Unparser$ptr.pop-to-value (args %unparser))
    (return-void)
  ))

  (if (== %type 1) (do
    (call @i8.print (args %LPAREN))
    (call @Unparser$ptr.increment-col (args %unparser 1))
    (call @Unparser$ptr.pop-to-value (args %unparser))
    (return-void)
  ))

; TODO correctness-assert (== 2 %type)
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

; unfolds values while filter-zipping with the lexical coordinates
; define filter-zip: in this context, only zip with coordinates of type texp
; TODO extract zipping into a separate @unparse_ driver
(def @unparse-texp (params (%unparser %struct.Unparser*) (%texp %struct.Texp*)) void (do

  (if (== 0 (cast u64 %texp)) (do
    (call @i8$ptr.unsafe-println (args "cannot unparse null-texp\00"))
    (call @exit (args 1))
  ))

  (let %value-ref (index %texp 0))
  (let %value-length (load (index %value-ref 1)))
  (let %length (load (index %texp 2)))

  (call @Unparser$ptr.pop-to-value (args %unparser))

  (call @String$ptr.print (args %value-ref))
  (call @Unparser$ptr.increment-col (args %unparser %value-length))

; debug
  (call @i8$ptr.unsafe-print (args "[ unparse-texp ] incremented by \00"))
  (call @u64.println (args %value-length))


  (call @unparse-children (args %unparser %texp 0))
  (return-void)
))

; unparse using lexical information from the %parser and the %texp it parsed
(def @unparse (params (%parser %struct.Parser*) (%texp %struct.Texp*)) void (do
  (auto %unparser %struct.Unparser)
  (store (call @Unparser.make (args %parser)) %unparser)

; consuming lexical coordinates with a lazy merge of comment and syntactic coordinates
; note: use unparse-children so it doesn't attempt to navigate to the program
  (call @unparse-children (args %unparser %texp 0))

; exhaust any tokens that are left after unparsing %texp's values
  (return-void)
))
