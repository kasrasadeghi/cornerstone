;========== Texp ==================================================================================

; sizeof(Texp) = 16 + 8 + 8 + 8
;              = 40
(struct %struct.Texp
  (%value %struct.String)
  (%children %struct.Texp*)
  (%length u64)
  (%capacity u64)
)

; consumes ownership of %value's memory allocation
; FIXME or does it?
; TODO rename to setFromString$ptr
(def @Texp$ptr.setFromString (params (%this %struct.Texp*) (%value %struct.String*)) void (do
  (store (load %value) (index %this 0))
  (store (cast %struct.Texp* (0 u64)) (index %this 1))
  (store 0 (index %this 2))
  (store 0 (index %this 3))
  (return-void)
))

(def @Texp.makeFromi8$ptr (params (%value-cstr i8*)) %struct.Texp (do
  (auto %result %struct.Texp)
  (store (call @String.makeFromi8$ptr (args %value-cstr)) (index %result 0))
  (store (cast %struct.Texp* (0 u64)) (index %result 1))
  (store 0 (index %result 2))
  (store 0 (index %result 3))
  (return (load %result))
))

(def @Texp.makeFromStringView (params (%value-view %struct.StringView*)) %struct.Texp (do
  (auto %result %struct.Texp)
  (store (call @String.makeFromStringView (args %value-view)) (index %result 0))
  (store (cast %struct.Texp* (0 u64)) (index %result 1))
  (store 0 (index %result 2))
  (store 0 (index %result 3))
  (return (load %result))
))

; copies %item into a conditionally resized child array in %this
; takes ownership of %item and %item's string
(def @Texp$ptr.push$ptr (params (%this %struct.Texp*) (%item %struct.Texp*)) void (do
  (let %children-ref (index %this 1))
  (let %length-ref (index %this 2))
  (let %cap-ref (index %this 3))

  (if (== (load %length-ref) (load %cap-ref)) (do

    (let %old-capacity (load %cap-ref))
    (if (== 0 %old-capacity) (do
      (store 1 %cap-ref)
    ))
    (if (!= 0 %old-capacity) (do
      (store (* 2 %old-capacity) %cap-ref)
    ))
    (let %new-capacity (load %cap-ref))
    
    (let %old-children (load %children-ref))
    (let %new-children (cast %struct.Texp* (call @realloc (args 
      (cast i8* %old-children) 
      (* 40 %new-capacity)))))
    (store %new-children (index %this 1))
  ))

  (let %children-base (cast u64 (load %children-ref)))
  (let %new-child-loc (cast %struct.Texp* 
    (+ (* 40 (cast u64 (load %length-ref))) %children-base)
  ))
  (store (load %item) %new-child-loc)

  (store (+ 1 (load %length-ref)) %length-ref)

  (return-void)
))

(def @Texp$ptr.push (params (%this %struct.Texp*) (%item %struct.Texp)) void (do
  (auto %local-item %struct.Texp)
  (store %item %local-item)
  (call @Texp$ptr.push$ptr (args %this %local-item))
  (return-void)
))

(def @Texp$ptr.parenPrint$lambda.child-iter (params (%this %struct.Texp*) (%child-index u64)) void (do
  (let %children (load (index %this 1)))
  (let %length (load (index %this 2)))

  (if (== %child-index %length) (do
    (return-void)
  ))

  (let %curr (cast %struct.Texp*  
    (+ (* 40 %child-index) (cast u64 %children))
  ))

  (if (!= 0 %child-index) (do
    (let %SPACE  (+ 32 (0 i8)))
    (call @i8.print (args %SPACE))
  ))

  (call @Texp$ptr.parenPrint (args %curr))
  (call-tail @Texp$ptr.parenPrint$lambda.child-iter (args %this (+ 1 %child-index)))
  (return-void)
))

(def @Texp$ptr.parenPrint (params (%this %struct.Texp*)) void (do
  (let %value-ref (index %this 0))
  (let %length (load (index %this 2)))

  (if (== 0 %length) (do
    (call @String$ptr.print (args %value-ref))
    (return-void)
  ))
  
  (let %LPAREN (+ 40 (0 i8)))
  (let %RPAREN (+ 41 (0 i8)))
  (let %SPACE  (+ 32 (0 i8)))

  (call @i8.print (args %LPAREN))
  (call @String$ptr.print (args %value-ref))
  (call @i8.print (args %SPACE))
  (call @Texp$ptr.parenPrint$lambda.child-iter (args %this 0))
  (call @i8.print (args %RPAREN))

  (return-void)
))

(def @Texp$ptr.free$lambda.child-iter (params (%this %struct.Texp*) (%child-index u64)) void (do
  (let %children (load (index %this 1)))
  (let %length (load (index %this 2)))

  (if (== %child-index %length) (do
    (return-void)
  ))

  (let %curr (cast %struct.Texp*  
    (+ (* 40 %child-index) (cast u64 %children))
  ))

  (call-tail @Texp$ptr.free$lambda.child-iter (args %this (+ 1 %child-index)))
  (return-void)
))

(def @Texp$ptr.last (params (%this %struct.Texp*)) %struct.Texp* (do
  (let %len (load (index %this 2)))
  (let %first-child (load (index %this 1)))
  (let %last (cast %struct.Texp*
    (+ (cast u64 %first-child)
       (* 40 (- %len 1)))
  ))
  (return %last)
))

(def @Texp$ptr.child (params (%this %struct.Texp*) (%i u64)) %struct.Texp* (do
; TODO consider bounds checking 
  (let %first-child (load (index %this 1)))
  (let %child (cast %struct.Texp*
    (+ (cast u64 %first-child)
       (* 40 %i))
  ))
  (return %child)
))

(def @Texp$ptr.free (params (%this %struct.Texp*)) void (do
  (call @String$ptr.free (args (index %this 0)))
  (call @free (args (cast i8* (load (index %this 1)))))
  (call @Texp$ptr.free$lambda.child-iter (args %this 0))
  (return-void)
))

(def @Texp$ptr.find_ (params (%this %struct.Texp*) (%last %struct.Texp*) (%key %struct.StringView*)) %struct.Texp* (do
  (let %view (call @String$ptr.view (args (index %this 0))))
  (if (call @StringView.eq (args %view (load %key))) (do
    (return %this)
  ))
  (if (== %this %last) (do
    (return (cast %struct.Texp* (0 u64)))
  ))
  (let %next (cast %struct.Texp* (+ 40 (cast u64 %this))))
  (return (call @Texp$ptr.find_ (args %next %last %key)))
))

(def @Texp$ptr.find (params (%this %struct.Texp*) (%key %struct.StringView*)) %struct.Texp* (do
  (let %first (load (index %this 1)))
  (let %last (call @Texp$ptr.last (args %this)))
  (return (call @Texp$ptr.find_ (args %first %last %key)))
))

; pushes curr onto result until curr == last
(def @Texp$ptr.clone_ (params (%acc %struct.Texp*) (%curr %struct.Texp*) (%last %struct.Texp*)) void (do
  (auto %curr-clone %struct.Texp)
  (store (call @Texp$ptr.clone (args %curr)) %curr-clone)
  (call @Texp$ptr.push$ptr (args %acc %curr-clone))

  (if (== %last %curr) (return-void))

  (let %next (cast %struct.Texp* (+ 40 (cast u64 %curr))))
  (call @Texp$ptr.clone_ (args %acc %next %last))
  (return-void)
))

(def @Texp$ptr.clone (params (%this %struct.Texp*)) %struct.Texp (do
  (auto %texp %struct.Texp)
  (store (call @String$ptr.copyalloc (args (index %this 0))) (index %texp 0))

  (call @Texp$ptr.clone_ (args %texp (load (index %texp 1)) (call @Texp$ptr.last (args %texp))))

  (return (load %texp))
))

(def @Texp$ptr.value-view (params (%this %struct.Texp*)) %struct.StringView* (do
  (return (cast %struct.StringView* (index %this 0)))
))

(def @Texp$ptr.value-check (params (%this %struct.Texp*) (%check i8*)) i1 (do
  (let %check-view (call @StringView.makeFromi8$ptr (args %check)))
  (let %value-view (call @String$ptr.view (args (index %this 0))))
  (return (call @StringView.eq (args %check-view %value-view)))
))

(def @Texp$ptr.value-get (params (%this %struct.Texp*) (%i u64)) i8 (do
  (let %value (load (index (index %this 0) 0)))
  (let %cptr (cast i8* (+ %i (cast u64 %value))))
  (return (load %cptr))
))

;========== Texp tests ============================================================================

(def @test.Texp-basic$lamdba.dump (params (%texp %struct.Texp*)) void (do
  (call @println args)
  (call @u64.print (args (load (index %texp 2))))

  (call @Texp$ptr.parenPrint (args %texp))
  (call @println args)
  (return-void)
))

(def @test.Texp-basic params void (do
  (auto %hello-string %struct.String)
  (call @String$ptr.set (args %hello-string "hello\00"))

  (auto %child0-string %struct.String)
  (call @String$ptr.set (args %child0-string "child-0\00"))

  (auto %child1-string %struct.String)
  (call @String$ptr.set (args %child1-string "child-1\00"))

  (auto %child2-string %struct.String)
  (call @String$ptr.set (args %child2-string "child-2\00"))

  (auto %texp %struct.Texp)
  (call @Texp$ptr.setFromString (args %texp %hello-string))

  (call @test.Texp-basic$lamdba.dump (args %texp))

; allocate children
  (auto %texp-child %struct.Texp)

; child 1
  (call @Texp$ptr.setFromString (args %texp-child %child0-string))
  (call @Texp$ptr.push$ptr (args %texp %texp-child))

  (call @test.Texp-basic$lamdba.dump (args %texp))

; child 2
  (call @Texp$ptr.setFromString (args %texp-child %child1-string))
  (call @Texp$ptr.push$ptr (args %texp %texp-child))

  (call @test.Texp-basic$lamdba.dump (args %texp))

; child 3
  (call @Texp$ptr.setFromString (args %texp-child %child2-string))
  (call @Texp$ptr.push$ptr (args %texp %texp-child))

  (call @test.Texp-basic$lamdba.dump (args %texp))

  (call @Texp$ptr.free (args %texp))
; FIXME still leaking some strings I think, 24 bytes on valgrind

  (return-void)
))

(def @test.Texp-clone params void (do
  (auto %hello-string %struct.String)
  (call @String$ptr.set (args %hello-string "hello\00"))

  (auto %child0-string %struct.String)
  (call @String$ptr.set (args %child0-string "child-0\00"))

  (auto %child1-string %struct.String)
  (call @String$ptr.set (args %child1-string "child-1\00"))

  (auto %child2-string %struct.String)
  (call @String$ptr.set (args %child2-string "child-2\00"))

  (auto %texp %struct.Texp)
  (call @Texp$ptr.setFromString (args %texp %hello-string))

  (call @test.Texp-basic$lamdba.dump (args %texp))

; allocate children
  (auto %texp-child %struct.Texp)

; child 1
  (call @Texp$ptr.setFromString (args %texp-child %child0-string))
  (call @Texp$ptr.push$ptr (args %texp %texp-child))

  (call @test.Texp-basic$lamdba.dump (args %texp))

; child 2
  (call @Texp$ptr.setFromString (args %texp-child %child1-string))
  (call @Texp$ptr.push$ptr (args %texp %texp-child))

  (call @test.Texp-basic$lamdba.dump (args %texp))

; child 3
  (call @Texp$ptr.setFromString (args %texp-child %child2-string))
  (call @Texp$ptr.push$ptr (args %texp %texp-child))

  (call @test.Texp-basic$lamdba.dump (args %texp))

  (call @Texp$ptr.free (args %texp))
; FIXME still leaking some strings I think, 24 bytes on valgrind

  (return-void)
))

(def @test.Texp-value-get params void (do
  (auto %hello-string %struct.String)
  (call @String$ptr.set (args %hello-string "hello\00"))

  (auto %texp %struct.Texp)
  (call @Texp$ptr.setFromString (args %texp %hello-string))

  (let %E_CHAR (+ 101 (0 i8)))
  
  (let %success (== %E_CHAR (call @Texp$ptr.value-get (args %texp 1))))
  (if %success (do
    (call @puts (args "pass\00"))
  ))
  (if (- 1 %success) (do
    (call @puts (args "fail\00"))    
  ))

  (call @Texp$ptr.free (args %texp))

  (return-void)
))
