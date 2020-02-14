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
  (%parser-i u64)
  (%file %struct.File)
  (%reader %struct.Reader)

; 6
  (%parser-comment-i u64)
)

(def @Unparser.make (params (%parser %struct.Parser*)) %struct.Unparser (do
  (auto %unparser %struct.Unparser)
  (store 0 (index %unparser 0))
  (store 0 (index %unparser 1))
  (store %parser (index %unparser 2))

; indices
  (store 0 (index %unparser 3)))
  (store 0 (index %unparser 6)))

; SOON count parser-i@3 to where the first non-comment is

  (let %filename (index %parser 4))
  (auto %file %struct.File)
  (store (call @File.open (args %filename)) %file)

  (auto %content %struct.StringView)
  (store (call @File$ptr.read (args %file)) %content)
  (call @Reader$ptr.set (args (index %unparser 5) %content))

  (return (load %unparser))
))

(def @Unparser$ptr.increment-col (params (%unparser %struct.Unparser*) (%col-delta u64)) void (do
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

  (store %save-col (index %reader 3))
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

  (if (< %line (load (index %unparser 0)) (do
    (call @u64.print (args (load (index %this 3))))
    (call @i8$ptr.unsafe-print (args ",\00"))
    (call @u64.print (args (load (index %this 4))))

    (call @i8$ptr.unsafe-print (args " -> \00"))

    (call @u64.print (args %line))
    (call @i8$ptr.unsafe-print (args ",\00"))
    (call @u64.print (args %col))
  ))

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

; gets next value
(def @Unparser$ptr.pop (params (%unparser %struct.Unparser*) (%index-out u64*)) void (do

  (store 0 %index&)
  (let %parser (load (index %unparser 2)))

  (let %parser-i (index %unparser 3))
  (let %comment-i (index %unparser 6))

  (return-void)
))


(def @Unparser$ptr.pop-to-value (params (%unparser %struct.Unparser*)) void (do

  (let %parser (load (index %unparser 2)))

  (let %parser-i (index %unparser 3))
  (let %comment-i (index %unparser 6))

; increment parser index
  (store (+ 1 %curr-parser-i) %parser-i)

  (let %line (call @u64-vector$ptr.unsafe-get (args (index %parser 1) %curr-parser-i)))
  (let %col  (call @u64-vector$ptr.unsafe-get (args (index %parser 2) %curr-parser-i)))
  (let %type (call @u64-vector$ptr.unsafe-get (args (index %parser 3) %curr-parser-i)))

; SOON check for index exhaustion, both comment-i == initial parser-i, so (type of comment-i is no longer comment, ), and parser-i == length

  (call @Unparser$ptr.navigate (args %unparser %line %col))

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
    (call @Unparser$ptr.pop ())
    (return-void)
  ))

  (if (== %type 1) (do
    (call @i8.print (args %LPAREN))
    (call @Unparser$ptr.increment-col (args %unparser 1))
  ))
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

  (call @unparse-children (args %unparser %texp 0))
  (return-void)
))

; unparse using lexical information from the %parser and the %texp it parsed
(def @unparse (params (%parser %struct.Parser*) (%texp %struct.Texp*)) void (do
  (auto %unparser %struct.Unparser)
  (store (call @Unparser.make (args %parser)) %unparser)

;; consume lexical tokens in the coordinate array until there are no more tokens
; this should coordinate with the consumption of 
; tokens are consumed in a lazy merge of two different iterators

; use unparse-children so it doesn't attempt to navigate to the program
  (call @unparse-children (args %unparser %texp 0))
  (return-void)
))
