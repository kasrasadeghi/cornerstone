;========== Reader ================================================================================

(struct %struct.Reader

; 0
  (%content %struct.StringView)

; 1
  (%iter i8*)

; 2
  (%prev i8)

; 3
  (%line u64)

; 4
  (%col u64)
)

(def @Reader$ptr.set (params (%this %struct.Reader*) (%string-view %struct.StringView*)) void (do
  (store (load %string-view) (index %this 0))

  (let %content (index %this 0))

; debug
; (call @i8$ptr.unsafe-println (args "reader:\00"))
; (call @u64.println (args (load (cast u64* %content))))
; (call @u64.println (args (load (cast u64* (index %content 1)))))

  (store (load (index %string-view 0)) (index %this 1))
  (store 0 (index %this 2))
  (store 0 (index %this 3))
  (store 0 (index %this 4))
  (return-void)
))

(def @Reader$ptr.peek (params (%this %struct.Reader*)) i8 (do

; debug
; (call @u64.println (args (cast u64 (load (index %this 1)))))

  (return (load (load (index %this 1))))
))

(def @Reader$ptr.get (params (%this %struct.Reader*)) i8 (do
  (let %iter-ref (index %this 1))
  (let %char (load (load %iter-ref)))
  (store %char (index %this 2))
  (store (cast i8* (+ 1 (cast u64 (load %iter-ref)))) %iter-ref)

; if newline, increment the line counter and set column counter to 0
  (let %NEWLINE (+ 10 (0 i8)))
  (if (== %char %NEWLINE) (do
    (store (+ 1 (load (index %this 3))) (index %this 3))
    (store 0 (index %this 4))
    (return %char)
  ))

; otherwise, increment the column counter
  (store (+ 1 (load (index %this 4))) (index %this 4))
  (return %char)
))

(def @Reader$ptr.seek-backwards-on-line (params (%this %struct.Reader*) (%line u64) (%col u64)) void (do
; assert (== %line (load (index %this 3)))

  (let %col-ref (index %this 4))
; assert (< %col (load %col-ref))

;      col ----- -offset ----> curr-col
; curr-col <----  offset ----- col
  (let %curr-col (load (index %this 4)))
  (let %anti-offset (- %curr-col %col))

; iter - -offset = iter + offset
  (store (cast i8* (- (cast u64 (load (index %this 1))) %anti-offset)) (index %this 1))
  (store %col (index %this 4))

  (return-void)
))

(def @Reader$ptr.seek-forwards.fail (params (%this %struct.Reader*) (%line u64) (%col u64) (%msg i8*)) void (do
  (call @i8$ptr.unsafe-print (args %msg))
  (call @i8$ptr.unsafe-print (args " \00"))

  (call @u64.print (args (load (index %this 3))))
  (call @i8$ptr.unsafe-print (args ",\00"))
  (call @u64.print (args (load (index %this 4))))

  (call @i8$ptr.unsafe-print (args " -> \00"))

  (call @u64.print (args %line))
  (call @i8$ptr.unsafe-print (args ",\00"))
  (call @u64.print (args %col))

  (call @println args)
  (call @exit (args 1))

  (return-void)
))

(def @Reader$ptr.seek-forwards (params (%this %struct.Reader*) (%line u64) (%col u64)) void (do
  (let %curr-line (index %this 3))
  (let %curr-col  (index %this 4))

  (if (== %line (load %curr-line)) (do
    (if (== %col (load %curr-col)) (do
      (return-void)
    ))
    (if (< %col (load %curr-col)) (do
      (call @Reader$ptr.seek-forwards.fail (args %this %line %col "Error: Seeking before cursor column\00"))
    ))
    (if (> %col (load %curr-col)) (do
      (call @Reader$ptr.get (args %this))
      (if (< %line (load %curr-line)) (do
        (call @Reader$ptr.seek-forwards.fail (args %this %line %col "Error: Seeking past end of column\00"))
      ))
      (call @Reader$ptr.seek-forwards (args %this %line %col))
      (return-void)
    ))
  ))

  (if (call @Reader$ptr.done (args %this)) (do
    (call @Reader$ptr.seek-forwards.fail (args %this %line %col "Error: Seeking past end of file\00"))
  ))

  (if (< %line (load %curr-line)) (do
    (call @Reader$ptr.seek-forwards.fail (args %this %line %col "Error: Seeking before cursor line\00"))
  ))

; TODO correctness assert (> %line (load %curr-line)
  (call @Reader$ptr.get (args %this))
  (call @Reader$ptr.seek-forwards (args %this %line %col))
  (return-void)
))

(def @Reader$ptr.find-next (params (%this %struct.Reader*) (%char i8)) void (do
  (if (call @Reader$ptr.done (args %this)) (do
    (call @i8$ptr.unsafe-println (args "Error: Finding character past end of file"))
    (call @exit (args 1))
  ))

  (let %peeked (call @Reader$ptr.peek (args %this)))

; debug
; (call @i8.print (args %peeked))
; (call @println args)

  (if (== %char %peeked) (do
    (return-void)
  ))
  (call @Reader$ptr.get (args %this))
  (call @Reader$ptr.find-next (args %this %char))
  (return-void)
))

(def @Reader$ptr.pos (params (%this %struct.Reader*)) u64 (do
  (let %iter (load (index %this 1)))
  (let %start (load (index (index %this 0) 0)))
  (let %result (- (cast u64 %iter) (cast u64 %start)))
  (return %result)
))

; a Reader is done when %iter points to the end of %content
(def @Reader$ptr.done (params (%this %struct.Reader*)) i1 (do
  (let %content (index %this 0))
  (let %content-end (cast i8* (+
    (cast u64 (load (index %content 0)))
    (load (index %content 1))
  )))
  (let %iter (load (index %this 1)))
  (return (== %iter %content-end))
))

(def @Reader$ptr.reset (params (%this %struct.Reader*)) void (do
  (let %string-view (index %this 0))
  (store (load (index %string-view 0)) (index %this 1))
  (store 0 (index %this 2))
  (store 0 (index %this 3))
  (store 0 (index %this 4))
  (return-void)
))

;========== Reader tests ==========================================================================

(def @test.Reader-get$lambda0 (params (%reader %struct.Reader*) (%i i32)) void (do
  (if (== %i 0) (do
    (return-void)
  ))
  (call @i8.print (args (call @Reader$ptr.get (args %reader))))
  (call-tail @test.Reader-get$lambda0 (args %reader (- %i 1)))
  (return-void)
))

(def @test.Reader-get params void (do
  (auto %filename %struct.StringView)
  (call @StringView$ptr.set (args %filename "lib2/core.bb.type.tall\00"))

  (auto %file %struct.File)
  (store (call @File.open (args %filename)) %file)

; print file name
  (call @String$ptr.println (args (index %file 0)))

  (auto %content %struct.StringView)
  (store (call @File$ptr.read (args %file)) %content)

; print content
; (call @StringView$ptr.print (args %content))

  (auto %reader %struct.Reader)
  (call @Reader$ptr.set (args %reader %content))

  (call @test.Reader-get$lambda0 (args %reader 50))

  (call @println args)

  (call @File.unread (args %content))
  (call @File$ptr.close (args %file))
  (return-void)
))

(def @test.Reader-done$lambda0 (params (%reader %struct.Reader*)) void (do
  (call @i8.print (args (call @Reader$ptr.get (args %reader))))
  (if (- 1 (call @Reader$ptr.done (args %reader))) (do
    (call-tail @test.Reader-done$lambda0 (args %reader))
  ))
  (return-void)
))

(def @test.Reader-done params void (do
  (auto %filename %struct.StringView)
  (call @StringView$ptr.set (args %filename "todo.json\00"))

  (auto %file %struct.File)
  (store (call @File.open (args %filename)) %file)

; print file name
  (call @String$ptr.println (args (index %file 0)))

  (auto %content %struct.StringView)
  (store (call @File$ptr.read (args %file)) %content)

; print content
; (call @StringView$ptr.print (args %content))

  (auto %reader %struct.Reader)
  (call @Reader$ptr.set (args %reader %content))

  (call @test.Reader-done$lambda0 (args %reader))

  (call @println args)

  (call @File.unread (args %content))
  (call @File$ptr.close (args %file))
  (return-void)
))
