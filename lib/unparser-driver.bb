(include "core.bb")
(include "rope.bb")
(include "unparse.bb")

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
  (call @u64.print (args (call @u64-vector$ptr.unsafe-get (args %types %row))))

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

  (if (!= 2 %argc) (do
    (call @i8$ptr.unsafe-println (args
      "usage: unparser <file.bb>\00"))
    (call @exit (args 1))
  ))

  (let %arg (cast i8** (+ 8 (cast u64 %argv))))

  (auto %filename %struct.StringView)
  (store (call @StringView.makeFromi8$ptr (args (load %arg))) %filename)

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

; debug
  (call @dump-parser (args %parser))

; new section

  (call @unparse (args %parser %prog))

; TODO remove
  (call @println args)

; END new section

  (call @File.unread (args %content))
  (call @File$ptr.close (args %file))

  (return 0)
))
