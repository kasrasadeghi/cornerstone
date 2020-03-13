;========== Pretty Printer ========================================================================

; FIXME: not sure why this is here
(def @Texp$ptr.pretty-print$lambda.do (params (%this %struct.Texp*)) void (do
; TODO consider adding stmt level pretty printing with indentation
  (return-void)
))

(def @Texp$ptr.pretty-print$lambda.toplevel (params (%this %struct.Texp*) (%last %struct.Texp*)) void (do
  (call @Texp$ptr.parenPrint (args %this))
  (call @println args)
  (if (!= %this %last) (do
    (let %next (cast %struct.Texp* (+ 40 (cast u64 %this))))
    (call @Texp$ptr.pretty-print$lambda.toplevel (args %next %last))
  ))
  (return-void)
))

(def @Texp$ptr.pretty-print (params (%this %struct.Texp*)) void (do
  (let %LPAREN (+ 40 (0 i8)))
  (let %RPAREN (+ 41 (0 i8)))

  (call @i8.print (args %LPAREN))
  (call @String$ptr.println (args (index %this 0)))

  (let %last (call @Texp$ptr.last (args %this)))
  (let %first-child (load (index %this 1)))
  (call @Texp$ptr.pretty-print$lambda.toplevel (args %first-child %last))

  (call @i8.print (args %RPAREN))
  (call @println args)
  (return-void)
))

;========== Pretty Printer tests ===================================================================

(def @test.texp-pretty-print params void (do
  (auto %filename %struct.StringView)
  (call @StringView$ptr.set (args %filename "lib/pprint.bb\00"))

  (auto %prog %struct.Texp)
  (store (call @Parser.parse-file (args %filename)) %prog)

  (call @Texp$ptr.pretty-print (args %prog))
  (return-void)
))
