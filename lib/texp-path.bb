;================ Texp Path ======================================

(struct %struct.Coord
  (%line u64)
  (%column u64)
)

(def @texp-path (params (%parser %struct.Parser*) (%line u64) (%col u64)) %struct.Coord (do
  (auto %result %struct.Coord)
  (return (load %result))
))



; ==== unmatched ===============

(def @unmatched-backward (params (%parser %struct.Parser*) (%line u64) (%col u64)) u64 (do
  (auto %paren-counter u64)
  (store 1 %paren-counter)

  (let %info-count (call @LexerInfo.length (args %parser)))

  (let %backward-search (call @search-coords-backward (args %parser %line %col)))
  (if (== %backward-search %info-count) (do
    (return %info-count)
  ))

  (let %comment-count (call @LexerInfo.count-comments (args %parser)))
  (return (call @unmatched-backward_
    (args %parser %paren-counter %info-count %comment-count %backward-search)))
))

(def @unmatched-backward_ (params (%parser %struct.Parser*)
                                  (%paren-counter u64*) (%total-length u64) (%comment-length u64)
                                  (%curr-i u64)) u64 (do

  (if (== %curr-i %comment-length) (do
    (return %total-length)
  ))

  (let %types (index %parser 3))

; debug
; (call @u64.println (args %curr-i))

  (let %curr-type (call @u64-vector$ptr.unsafe-get (args %types %curr-i)))

; open paren
  (if (== %curr-type 0) (do
    (store (- (load %paren-counter) 1) %paren-counter)
  ))

; close paren
  (if (== %curr-type 1) (do
    (store (+ (load %paren-counter) 1) %paren-counter)
  ))

  (if (== 0 (load %paren-counter)) (do
    (return %curr-i)
  ))

  (return (call-tail @unmatched-backward_
    (args %parser %paren-counter %total-length %comment-length (- %curr-i 1))))
))


(def @unmatched-forward (params (%parser %struct.Parser*) (%line u64) (%col u64)) u64 (do

  (auto %paren-counter u64)
  (store 1 %paren-counter)

  (let %info-count (call @LexerInfo.length (args %parser)))

  (let %forward-search (call @search-coords-forward (args %parser %line %col)))
  (if (== %forward-search %info-count) (do
    (return %info-count)
  ))

  (return (call @unmatched-forward_ (args %parser %paren-counter %info-count %forward-search)))
))

; returns %total-length on failure
(def @unmatched-forward_ (params (%parser %struct.Parser*)
                                 (%paren-counter u64*) (%total-length u64) (%curr-i u64)) u64 (do

  (if (== %curr-i %total-length) (do
    (return %total-length)
  ))

  (let %types (index %parser 3))

; debug
; (call @u64.println (args %curr-i))

  (let %curr-type (call @u64-vector$ptr.unsafe-get (args %types %curr-i)))

; open paren
  (if (== %curr-type 0) (do
    (store (+ 1 (load %paren-counter)) %paren-counter)
  ))

; close paren
  (if (== %curr-type 1) (do
    (store (- (load %paren-counter) 1) %paren-counter)
  ))

  (if (== 0 (load %paren-counter)) (do
    (return %curr-i)
  ))

  (return (call-tail @unmatched-forward_ (args %parser %paren-counter %total-length (+ %curr-i 1))))
))



; ==== search for token ===============

(def @search-coords-backward (params (%parser %struct.Parser*) (%line u64) (%col u64)) u64 (do
  (let %comment-count (call @LexerInfo.count-comments (args %parser)))

  (let %info-count (call @LexerInfo.length (args %parser)))

; assert %info-count > 0
  (if (== 0 %info-count) (do
    (call @i8$ptr.unsafe-println (args "ERROR: lexer-parser info is empty?"))
    (call @exit (args 0))
  ))

  (return (call @search-coords-backward_
    (args %parser %line %col (- %info-count 1) %comment-count)))
))

; TODO: check that searching backwards doesn't give you an index to a comment
; - when syntax-info is empty, but there is comment-info, it'll start with the last comment
; - in that case, it'll still do a comparison in backward_
; - if the comment starts before the coordinate, then it'll return the comment before it returns %info-count/%total-length

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
  (let %comment-count (call @LexerInfo.count-comments (args %parser)))
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




; ==== LexerInfo ==========================

(def @LexerInfo.count-comments (params (%parser %struct.Parser*)) u64 (do
  (let %info-count (call @LexerInfo.length (args %parser)))
  (return (call-tail @LexerInfo.count-comments_ (args %parser 0 %info-count)))
))

(def @LexerInfo.count-comments_ (params (%parser %struct.Parser*) (%curr-i u64) (%total-length u64)) u64 (do

  (if (== %curr-i %total-length) (do (return %curr-i)))

  (let %type-vec (index %parser 3))
  (let %type (call @u64-vector$ptr.unsafe-get (args %type-vec %curr-i)))
  (if (!= %type 3) (do (return %curr-i)))

  (return (call-tail @LexerInfo.count-comments_ (args %parser (+ 1 %curr-i) %total-length)))
))

; NOTE: assume lines-count == columns-count == types-count
(def @LexerInfo.length (params (%parser %struct.Parser*)) u64 (do
  (let %lines (index %parser 1))
; length is index 1 of a vector
  (let %lines-count (load (index %lines 1)))
  (return %lines-count)
))
