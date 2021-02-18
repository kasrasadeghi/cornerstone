;================ Texp Path ======================================

(struct %struct.Coord
  (%line u64)
  (%column u64)
)

(def @texp-path (params (%parser %struct.Parser*) (%line u64) (%col u64)) %struct.Coord (do
  (auto %result %struct.Coord)
  (return (load %result))
))

(def @search-coords-backward (params (%parser %struct.Parser*) (%line u64) (%col u64)) u64 (do
  (let %comment-count (call @Lexer.count-comments (args %parser)))

  (let %lines (index %parser 1))
  (let %lines-length (load (index %lines 1)))
; assert lines-length > 0
  (if (== %lines-length 0) (do
    (call @i8$ptr.unsafe-println (args "ERROR: lexer-parser info is empty?"))
    (call @exit (args 0))
  ))

  (return (call-tail @search-coords-backward_ 
    (args %parser %line %col (- %lines-length 1) %comment-count)))
))

; returns length of coord vector on failure
; NOTE: does not return -1, because it returns an unsigned value
; NOTE: %comment-count serves as a left-side terminating case as we're iterating over syntax coords
(def @search-coords-backward_ (params (%parser %struct.Parser*)
                                      (%line u64) (%col u64) (%i u64) (%comment-count u64)) u64 (do
  (let %lines (index %parser 1))
  (let %columns (index %parser 2))

  (let %i-line (call @u64-vector$ptr.unsafe-get (args %lines %i)))
  (let %i-col  (call @u64-vector$ptr.unsafe-get (args %columns %i)))

; NOTE: the comparison is backwards compared to the forward search
  (let %compare (call @TextCoord.lexically-compare (args %i-line %i-col %line %col)))
  (if %compare (do
    (return %i)
  ))

  (if (== %i %comment-count) (do
    (let %lines-length (load (index %lines 1)))
    (return %lines-length)
  ))

  (return (call-tail @search-coords-backward_ (args %parser %line %col (- %i 1) %comment-count)))
))

; return index of coordinate in %parser
; if index == length of vectors in %parser, then result was not found
(def @search-coords-forward (params (%parser %struct.Parser*) (%line u64) (%col u64)) u64 (do
  (let %comment-count (call @Lexer.count-comments (args %parser)))
  (return (call-tail @search-coords-forward_ (args %parser %line %col %comment-count)))
))

; returns length of coord vector on failure
(def @search-coords-forward_ (params (%parser %struct.Parser*) (%line u64) (%col u64) (%i u64)) u64 (do
  (let %lines (index %parser 1))
  (let %columns (index %parser 2))

; if i == length
  (let %lines-length (load (index %lines 1)))
  (if (== %i %lines-length) (do (return %i)))

  (let %i-line (call @u64-vector$ptr.unsafe-get (args %lines %i)))
  (let %i-col  (call @u64-vector$ptr.unsafe-get (args %columns %i)))

  (let %compare (call @TextCoord.lexically-compare (args %line %col %i-line %i-col)))
  (if %compare (do
    (return %i)
  ))

  (return (call-tail @search-coords-forward_ (args %parser %line %col (+ %i 1))))
))

(def @Lexer.count-comments (params (%parser %struct.Parser*)) u64 (do
  (let %lines (index %parser 1))
; length is index 1 of a vector
  (let %lines-count (load (index %lines 1)))
  (return (call-tail @Lexer.count-comments_ (args %parser 0 %lines-count)))
))

(def @Lexer.count-comments_ (params (%parser %struct.Parser*) (%curr-i u64) (%total-length u64)) u64 (do

  (if (== %curr-i %total-length) (do (return %curr-i)))

  (let %type-vec (index %parser 3))
  (let %type (call @u64-vector$ptr.unsafe-get (args %type-vec %curr-i)))
  (if (!= %type 3) (do (return %curr-i)))

  (return (call-tail @Lexer.count-comments_ (args %parser (+ 1 %curr-i) %total-length)))
))

