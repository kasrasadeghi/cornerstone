(include "core.bb")
(include "unparse.bb")

;========== main program ===========================================================================

(def @main (params (%argc i32) (%argv i8**)) i32 (do

  (if (!= 2 %argc) (do
    (call @i8$ptr.unsafe-println (args
      "usage: matcher <test-case> from <test-case> in ../backbone-test/matcher/*\00"))
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

  (call @Parser$ptr.remove-comments (args %parser))

  (auto %prog %struct.Texp)
  (auto %filename-string %struct.String)
  (store (call @String.makeFromStringView (args %filename)) %filename-string)
  (call @Texp$ptr.setFromString (args %prog %filename-string))

  (call @Parser$ptr.collect (args %parser %prog))

; new section
  (call @unparse (args %parser %prog))
; END new section

  (call @File.unread (args %content))
  (call @File$ptr.close (args %file))

  (return 0)
))
