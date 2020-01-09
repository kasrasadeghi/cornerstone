;========== Parser ================================================================================

(def @i8.isspace (params (%this i8)) i1 (do

; ' ', space 
  (if (== %this 32) (do (return true)))

; '\f', form feed
  (if (== %this 12) (do (return true)))

; '\n', new line
  (if (== %this 10) (do (return true)))
  
; '\r', carriage return
  (if (== %this 13) (do (return true)))

; '\t', horizontal tab
  (if (== %this 9) (do (return true)))

; '\v', vertical tab
  (if (== %this 11) (do (return true)))

  (return false)
))

(struct %struct.Parser
  (%reader %struct.Reader))

(def @Parser$ptr.whitespace (params (%this %struct.Parser*)) void (do
  (if (call @i8.isspace (args (call @Reader$ptr.peek (args (index %this 0))))) (do
    (call @Reader$ptr.get (args (index %this 0)))
    (call-tail @Parser$ptr.whitespace (args %this))
    (return-void)
  ))
  (return-void)
))

(def @Parser$ptr.word_ (params (%this %struct.Parser*) (%acc %struct.String*)) void (do
  (let %reader (index %this 0))
  (if (call @Reader$ptr.done (args %reader)) (do
    (return-void)
  ))

  (let %LPAREN (+ 40 (0 i8)))
  (let %RPAREN (+ 41 (0 i8)))

  (let %c (call @Reader$ptr.peek (args %reader)))
  (if (== %LPAREN %c) (do (return-void)))
  (if (== %RPAREN %c) (do (return-void)))
  (if (call @i8.isspace (args %c)) (do (return-void)))

  (call @Reader$ptr.get (args %reader))
  (call @String$ptr.pushChar (args %acc %c))
  (call-tail @Parser$ptr.word_ (args %this %acc))
  (return-void)
))

(def @Parser$ptr.word (params (%this %struct.Parser*)) %struct.String (do
  (call @Parser$ptr.whitespace (args %this))
; TODO CHECK not r.done OTHERWISE "reached end of file while parsing word"

  (auto %acc %struct.String)
  (store (call @String.makeEmpty args) %acc)
  (call @Parser$ptr.word_ (args %this %acc))
  (return (load %acc))
))

; parses string contents until the string is closed
(def @Parser$ptr.string_ (params (%this %struct.Parser*) (%acc %struct.String*)) void (do
  (let %QUOTE (+ 34 (0 i8)))
  (let %BACKSLASH (+ 92 (0 i8)))

  (if (== %QUOTE (call @Reader$ptr.peek (args (index %this 0)))) (do
    (let %prev (load (index (index %this 0) 2)))
    (if (!= %BACKSLASH %prev) (do
      (return-void)
    ))
  ))

  (let %c (call @Reader$ptr.get (args (index %this 0))))
  (call @String$ptr.pushChar (args %acc %c))
  (call-tail @Parser$ptr.string_ (args %this %acc))
  (return-void)
))

(def @Parser$ptr.string (params (%this %struct.Parser*)) %struct.Texp (do
  (auto %acc %struct.String)
  (store (call @String.makeEmpty args) %acc)

; TODO assert r.peek == '\"'
  (call @String$ptr.pushChar (args %acc (call @Reader$ptr.get (args (index %this 0)))))
  
  (call @Parser$ptr.string_ (args %this %acc))

; TODO assert r.peek == '\"'
  (call @String$ptr.pushChar (args %acc (call @Reader$ptr.get (args (index %this 0)))))
  
  (auto %texp %struct.Texp)
  (call @Texp$ptr.setFromString (args %texp %acc))
  (return (load %texp))
))

(def @Parser$ptr.atom (params (%this %struct.Parser*)) %struct.Texp (do
; TODO assert r.peek != ')'

; string
  (let %QUOTE (+ 34 (0 i8)))
  (if (== %QUOTE (call @Reader$ptr.peek (args (index %this 0)))) (do
    (return (call @Parser$ptr.string (args %this)))
  ))

; TODO char

; atom
  (auto %texp %struct.Texp)
  (auto %word %struct.String)
  (store (call @Parser$ptr.word (args %this)) %word)
  (call @Texp$ptr.setFromString (args %texp %word))
  (return (load %texp))
))

(def @Parser$ptr.list_ (params (%this %struct.Parser*) (%acc %struct.Texp*)) void (do
  (let %RPAREN (+ 41 (0 i8)))

  (if (!= %RPAREN (call @Reader$ptr.peek (args (index %this 0)))) (do
    (auto %texp %struct.Texp)
    (store (call @Parser$ptr.texp (args %this)) %texp)
    (call @Texp$ptr.push$ptr (args %acc %texp))
    (call @Parser$ptr.whitespace (args %this))
    (call @Parser$ptr.list_ (args %this %acc))
  ))
  (return-void)
))

(def @Parser$ptr.list (params (%this %struct.Parser*)) %struct.Texp (do
; TODO assert r.get == '('
  (call @Reader$ptr.get (args (index %this 0)))

  (auto %curr %struct.Texp)
  (auto %word %struct.String)
  (store (call @Parser$ptr.word (args %this)) %word)
  (call @Texp$ptr.setFromString (args %curr %word))

  (call @Parser$ptr.whitespace (args %this))

  (call @Parser$ptr.list_ (args %this %curr))
  
; TODO assert r.get == ')'
  (call @Reader$ptr.get (args (index %this 0)))
  
  (return (load %curr))
))

(def @Parser$ptr.texp (params (%this %struct.Parser*)) %struct.Texp (do
; if r.peek = '(' list otherwise atom
  (let %LPAREN (+ 40 (0 i8)))

  (call @Parser$ptr.whitespace (args %this))
  
  (if (== %LPAREN (call @Reader$ptr.peek (args (index %this 0)))) (do
    (return (call @Parser$ptr.list (args %this)))
  ))

  (return (call @Parser$ptr.atom (args %this)))
))

(def @Parser$ptr.collect (params (%this %struct.Parser*) (%parent %struct.Texp*)) void (do
  (if (call @Reader$ptr.done (args (index %this 0))) (do (return-void)))

  (auto %child %struct.Texp)
  (store (call @Parser$ptr.texp (args %this)) %child)
  (call @Texp$ptr.push$ptr (args %parent %child))

  (call @Parser$ptr.whitespace (args %this))

  (call @Parser$ptr.collect (args %this %parent))

  (return-void)
))

(def @Parser$ptr.remove-comments (params (%this %struct.Parser*) (%state i8)) void (do 
  (let %NEWLINE       (+ 10 (0 i8)))
  (let %SPACE         (+ 32 (0 i8)))
  (let %QUOTE         (+ 34 (0 i8)))
  (let %SEMICOLON     (+ 59 (0 i8)))
  (let %BACKSLASH     (+ 92 (0 i8)))

  
  (let %COMMENT_STATE (- (0 i8) 1))
  (let %START_STATE   (+ 0 (0 i8)))
  (let %STRING_STATE  (+ 1 (0 i8)))
  (let %CHAR_STATE    (+ 2 (0 i8)))

  (let %reader (index %this 0))

  (let %done (call @Reader$ptr.done (args %reader)))
  (if %done (do
    (call @Reader$ptr.reset (args %reader))
    (return-void)
  ))

  (let %c (call @Reader$ptr.get (args %reader)))

  (if (== %COMMENT_STATE %state) (do
    (if (== %NEWLINE %c) (do
      (call-tail @Parser$ptr.remove-comments (args %this %START_STATE))
      (return-void)
    ))
; TODO assert %prev != (0 i8)
    (store %SPACE (cast i8* (- (cast u64 (load (index %reader 1))) 1)))
    (call-tail @Parser$ptr.remove-comments (args %this %state))
    (return-void)
  ))

  (if (== %START_STATE %state) (do
    (if (== %QUOTE %c) (do
      (call-tail @Parser$ptr.remove-comments (args %this %STRING_STATE))
      (return-void)
    ))

; TODO APOSTROPHE comparison for CHAR_STATE
    
    (if (== %SEMICOLON %c) (do
; TODO assert %prev != (0 i8)
      (store %SPACE (cast i8* (- (cast u64 (load (index %reader 1))) 1)))
      (call-tail @Parser$ptr.remove-comments (args %this %COMMENT_STATE))
      (return-void)
    ))

    (call-tail @Parser$ptr.remove-comments (args %this %state))
    (return-void)
  ))

  (if (== %STRING_STATE %state) (do
; consider using @Parser$ptr.string_ for this    
    (if (== %QUOTE %c) (do
      (let %prev (load (index %reader 2)))
      (if (!= %BACKSLASH %prev) (do
        (call-tail @Parser$ptr.remove-comments (args %this %START_STATE))
        (return-void)
      ))
    ))

    (call-tail @Parser$ptr.remove-comments (args %this %state))
    (return-void)
  ))

; TODO handle CHAR_STATE

  (return-void)
))

(def @Parser.parse-file (params (%filename %struct.StringView*)) %struct.Texp (do
  (auto %file %struct.File)
  (store (call @File.openrw (args %filename)) %file)

  (auto %content %struct.StringView)
  (store (call @File$ptr.readwrite (args %file)) %content)

  (auto %parser %struct.Parser)
  (call @Reader$ptr.set (args (index %parser 0) %content))

  (call @Parser$ptr.remove-comments (args %parser 0))

  (auto %prog %struct.Texp) 
  (auto %filename-string %struct.String)
  (store (call @String.makeFromStringView (args %filename)) %filename-string)
  (call @Texp$ptr.setFromString (args %prog %filename-string))

  (call @Parser$ptr.collect (args %parser %prog))

  (call @File.unread (args %content))
  (call @File$ptr.close (args %file))
  (return (load %prog))
))

(def @Parser.parse-file-i8$ptr (params (%filename i8*)) %struct.Texp (do
  (auto %fn-view %struct.StringView)
  (store (call @StringView.makeFromi8$ptr (args %filename)) %fn-view)
  (return (call @Parser.parse-file (args %fn-view)))
))

;========== Parser tests ==========================================================================

(def @test.parser-whitespace params void (do
  (auto %filename %struct.StringView)
  (call @StringView$ptr.set (args %filename ""))
  
  (auto %file %struct.File)
  (store (call @File.open (args %filename)) %file)
  
  (auto %content %struct.StringView)
  (store (call @File$ptr.read (args %file)) %content)

  (auto %parser %struct.Parser)
  (call @Reader$ptr.set (args (index %parser 0) %content))

  (call @puts (args (load (index (index %parser 0) 1))))

  (call @Parser$ptr.whitespace (args %parser))
  (call @puts (args (load (index (index %parser 0) 1))))

  (call @Parser$ptr.whitespace (args %parser))
  (call @puts (args (load (index (index %parser 0) 1))))

  (call @Reader$ptr.get (args (index %parser 0)))
  (call @puts (args (load (index (index %parser 0) 1))))

  (call @Parser$ptr.whitespace (args %parser))
  (call @puts (args (load (index (index %parser 0) 1))))

  (call @File.unread (args %content))
  (call @File$ptr.close (args %file))
  (return-void)
))

(def @test.parser-atom params void (do
  (auto %filename %struct.StringView)
  (call @StringView$ptr.set (args %filename "huh\00"))
  
  (auto %file %struct.File)
  (store (call @File.open (args %filename)) %file)
  
  (auto %content %struct.StringView)
  (store (call @File$ptr.read (args %file)) %content)

  (auto %parser %struct.Parser)
  (call @Reader$ptr.set (args (index %parser 0) %content))

  (call @puts (args (load (index (index %parser 0) 1))))

  (auto %texp %struct.Texp)
  (store (call @Parser$ptr.atom (args %parser)) %texp)
  (call @puts (args (load (index (index %parser 0) 1))))

  (call @Texp$ptr.parenPrint (args %texp))
  (call @println args)

  (auto %texp2 %struct.Texp)
  (store (call @Parser$ptr.atom (args %parser)) %texp2)
  (call @puts (args (load (index (index %parser 0) 1))))

  (call @Texp$ptr.parenPrint (args %texp2))
  (call @println args)

  (call @File.unread (args %content))
  (call @File$ptr.close (args %file))
  (return-void)
))

(def @test.parser-texp params void (do
  (auto %filename %struct.StringView)
  (call @StringView$ptr.set (args %filename "lib2/core.bb.type.tall\00"))
  
  (auto %file %struct.File)
  (store (call @File.open (args %filename)) %file)
  
  (auto %content %struct.StringView)
  (store (call @File$ptr.read (args %file)) %content)

  (auto %parser %struct.Parser)
  (call @Reader$ptr.set (args (index %parser 0) %content))

  (call @puts (args (load (index (index %parser 0) 1))))

  (auto %texp %struct.Texp)
  (store (call @Parser$ptr.texp (args %parser)) %texp)
  (call @puts (args (load (index (index %parser 0) 1))))

  (call @Texp$ptr.parenPrint (args %texp))
  (call @println args)

  (call @File.unread (args %content))
  (call @File$ptr.close (args %file))
  (return-void)
))

(def @test.parser-string params void (do
  (auto %filename %struct.StringView)
  (call @StringView$ptr.set (args %filename "../backbone-test/texp-parser/string.texp\00"))
  
  (auto %file %struct.File)
  (store (call @File.open (args %filename)) %file)
  
  (auto %content %struct.StringView)
  (store (call @File$ptr.read (args %file)) %content)

  (auto %parser %struct.Parser)
  (call @Reader$ptr.set (args (index %parser 0) %content))

  (call @puts (args (load (index (index %parser 0) 1))))

  (auto %texp %struct.Texp)
  (store (call @Parser$ptr.texp (args %parser)) %texp)
  (call @puts (args (load (index (index %parser 0) 1))))

  (call @Texp$ptr.parenPrint (args %texp))
  (call @println args)
  (call @u64.print (args (load (index %texp 2))))
  (call @println args)

  (call @File.unread (args %content))
  (call @File$ptr.close (args %file))
  (return-void)
))

(def @test.parser-comments params void (do
  (auto %filename %struct.StringView)
	(call @StringView$ptr.set (args %filename "lib2/core.bb.type.tall\00"))
  
  (auto %file %struct.File)
  (store (call @File.openrw (args %filename)) %file)

  (auto %content %struct.StringView)
  (store (call @File$ptr.readwrite (args %file)) %content)

  (auto %parser %struct.Parser)
  (call @Reader$ptr.set (args (index %parser 0) %content))

  (call @Parser$ptr.remove-comments (args %parser 0))

  (call @puts (args (load (index (index %parser 0) 1))))

  (auto %texp %struct.Texp)
  (store (call @Parser$ptr.texp (args %parser)) %texp)
  
  (call @Texp$ptr.parenPrint (args %texp))
  (call @println args)

  (call @File.unread (args %content))
  (call @File$ptr.close (args %file))

  (return-void)
))

(def @test.parser-file params void (do
  (auto %filename %struct.StringView)
	(call @StringView$ptr.set (args %filename "lib2/core.bb.type.tall\00"))
  
  (auto %file %struct.File)
  (store (call @File.openrw (args %filename)) %file)

  (auto %content %struct.StringView)
  (store (call @File$ptr.readwrite (args %file)) %content)

  (auto %parser %struct.Parser)
  (call @Reader$ptr.set (args (index %parser 0) %content))

  (call @Parser$ptr.remove-comments (args %parser 0))

  (auto %prog %struct.Texp) 
  (auto %filename-string %struct.String)
  (store (call @String.makeFromStringView (args %filename)) %filename-string)
  (call @Texp$ptr.setFromString (args %prog %filename-string))

  (call @Parser$ptr.collect (args %parser %prog))

  (call @Texp$ptr.parenPrint (args %prog))

  (call @File.unread (args %content))
  (call @File$ptr.close (args %file))
  (return-void)
))
