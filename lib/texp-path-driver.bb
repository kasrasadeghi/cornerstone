(include "core.bb")
(include "rope.bb")
(include "texp-path.bb")

;========== main program ===========================================================================

(def @dump-parser_ (params (%parser %struct.Parser*) (%row u64)) void (do
  (let %texp-lines  (index %parser 2))

  (if (== %row (load (index %texp-lines 1))) (do
    (return-void)
  ))

  (let %lines (index %parser 1))
  (let %cols  (index %parser 2))
  (let %types (index %parser 3))

  (call @u64.print (args %row))
  (call @i8$ptr.unsafe-print (args ": \00"))

  (call @i8$ptr.unsafe-print (args "(\00"))
  (call @u64.print (args (call @u64-vector$ptr.unsafe-get (args %lines %row))))
  (call @i8$ptr.unsafe-print (args ", \00"))
  (call @u64.print (args (call @u64-vector$ptr.unsafe-get (args %cols %row))))
  (call @i8$ptr.unsafe-print (args ")  \00"))

  (let %type (call @u64-vector$ptr.unsafe-get (args %types %row)))
  (call @u64.print (args %type))

  (call @i8$ptr.unsafe-print (args " \00"))

  (if (== %type 0) (do
    (call @i8$ptr.unsafe-print (args "'('\00"))
  ))

  (if (== %type 1) (do
    (call @i8$ptr.unsafe-print (args "')'\00"))
  ))

  (if (== %type 2) (do
; TODO actually put texp values here? seems hard
    (call @i8$ptr.unsafe-print (args "value\00"))
  ))

  (call @println args)
  (call @dump-parser_ (args %parser (+ 1 %row)))

  (return-void)
))

(def @dump-parser (params (%parser %struct.Parser*)) void (do
  (let %lines  (index %parser 1))
  (let %cols   (index %parser 2))
  (let %types  (index %parser 3))

  (call @i8$ptr.unsafe-println (args "lengths:\00"))
  (call @u64.print (args (load (index %lines 1))))
  (call @i8$ptr.unsafe-print (args " \00"))
  (call @u64.print (args (load (index %cols 1))))
  (call @i8$ptr.unsafe-print (args " \00"))
  (call @u64.print (args (load (index %types 1))))
  (call @println args)

  (call @i8$ptr.unsafe-println (args "---------------------------------\00"))

  (call @dump-parser_ (args %parser 0))
  (return-void)
))

(def @main (params (%argc i32) (%argv i8**)) i32 (do

; find    v
  (let    %example   (+    0    (1 u64)))
;  ^      ^ 72, 10   ^
;  ^ 72, 3           ^ 72, 21
;    backward          forward
; ^ unmatched backward                  ^ unmatched forward

  (auto %filename %struct.StringView)
  (store (call @StringView.makeFromi8$ptr (args "lib/texp-path-driver.bb\00")) %filename)

; stolen from @Parser.parse-file
  (auto %file %struct.File)
  (store (call @File.openrw (args %filename)) %file)

  (auto %content %struct.StringView)
  (store (call @File$ptr.readwrite (args %file)) %content)

  (auto %parser %struct.Parser)
  (store (call @Parser.make (args %content)) %parser)
  (store (load %filename) (index %parser 4))

  (call @Parser$ptr.remove-comments (args %parser))

  (auto %prog %struct.Texp)
  (auto %filename-string %struct.String)
  (store (call @String.makeFromStringView (args %filename)) %filename-string)
  (call @Texp$ptr.setFromString (args %prog %filename-string))

  (call @Parser$ptr.collect (args %parser %prog))

; new section

  (call @dump-parser (args %parser))
  (call @println args)

  (let %line (+ 0 (72 u64)))
  (let %column (+ 0 (39 u64)))

  (call @u64.print (args %line))
  (call @i8$ptr.unsafe-print (args ", \00"))
  (call @u64.print (args %column))
  (call @println args)
  (call @i8$ptr.unsafe-print (args "  ->\00"))
  (call @println args)

  (let %info-count (call @LexerInfo.length (args %parser)))

  (let %lines (index %parser 1))
  (let %columns (index %parser 2))

  (let %i-sf (call @search-coords-forward (args %parser %line %column)))

  (if (!= %i-sf %info-count) (do
    (call @u64.print (args (call @u64-vector$ptr.unsafe-get (args %lines %i-sf))))
    (call @i8$ptr.unsafe-print (args ", \00"))
    (call @u64.print (args (call @u64-vector$ptr.unsafe-get (args %columns %i-sf))))
    (call @println args)
  ))

  (let %i-sb (call @search-coords-backward (args %parser %line %column)))

  (if (!= %i-sb %info-count) (do
    (call @u64.print (args (call @u64-vector$ptr.unsafe-get (args %lines %i-sb))))
    (call @i8$ptr.unsafe-print (args ", \00"))
    (call @u64.print (args (call @u64-vector$ptr.unsafe-get (args %columns %i-sb))))
    (call @println args)
  ))

  (let %i-uf (call @unmatched-forward (args %parser %line %column)))

  (if (!= %i-uf %info-count) (do
    (call @u64.print (args (call @u64-vector$ptr.unsafe-get (args %lines %i-uf))))
    (call @i8$ptr.unsafe-print (args ", \00"))
    (call @u64.print (args (call @u64-vector$ptr.unsafe-get (args %columns %i-uf))))
    (call @println args)
  ))

  (let %i-ub (call @unmatched-backward (args %parser %line %column)))

  (if (!= %i-ub %info-count) (do
    (call @u64.print (args (call @u64-vector$ptr.unsafe-get (args %lines %i-ub))))
    (call @i8$ptr.unsafe-print (args ", \00"))
    (call @u64.print (args (call @u64-vector$ptr.unsafe-get (args %columns %i-ub))))
    (call @println args)
  ))

; END new section

  (call @File.unread (args %content))
  (call @File$ptr.close (args %file))

  (return 0)
))
