(include "core.bb")
(include "rope.bb")
(include "unparse.bb")
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

  (let %line (+ 0 (98 u64)))
  (let %column (+ 0 (37 u64)))

  (let %i (call @search-coords-backward (args %parser %line %column)))
  (let %lines (index %parser 1))
  (let %columns (index %parser 2))

  (call @u64.print (args (call @u64-vector$ptr.unsafe-get (args %lines %i))))
  (call @i8$ptr.unsafe-print (args ", \00"))
  (call @u64.print (args (call @u64-vector$ptr.unsafe-get (args %columns %i))))
  (call @println args)

; END new section

  (call @File.unread (args %content))
  (call @File$ptr.close (args %file))

  (return 0)
))
