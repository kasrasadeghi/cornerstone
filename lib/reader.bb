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
  (store (load (index %string-view 0)) (index %this 1))
  (store 0 (index %this 2))
  (store 0 (index %this 3))
  (store 0 (index %this 4))
  (return-void)
))

(def @Reader$ptr.peek (params (%this %struct.Reader*)) i8 (do
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