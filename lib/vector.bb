(struct %struct.u64-vector
  (%data u64*)
  (%len u64)
  (%cap u64))

(def @u64-vector.make params %struct.u64-vector (do
  (auto %result %struct.u64-vector)
  (store (cast u64* (0 u64)) (index %result 0))
  (store 0 (index %result 1))
  (store 0 (index %result 2))
  (return (load %result))
))

(def @u64-vector$ptr.push (params (%this %struct.u64-vector*) (%item u64)) void (do
  (let %SIZEOF-u64 (+ 8 (0 u64)))

  (let %data-ref (index %this 0))
  (let %length-ref (index %this 1))
  (let %cap-ref (index %this 2))

  (if (== (load %length-ref) (load %cap-ref)) (do

    (let %old-capacity (load %cap-ref))
    (if (== 0 %old-capacity) (do
      (store 1 %cap-ref)
    ))
    (if (!= 0 %old-capacity) (do
      (store (* 2 %old-capacity) %cap-ref)
    ))
    (let %new-capacity (load %cap-ref))

    (let %old-data (load %data-ref))
    (let %new-data (cast u64* (call @realloc (args 
      (cast i8* %old-data) 
      (* %SIZEOF-u64 %new-capacity)))))
    (store %new-data (index %this 0))
  ))

  (let %data-base (cast u64 (load %data-ref)))
  (let %new-child-loc (cast u64*
    (+ %data-base (* %SIZEOF-u64 (cast u64 (load %length-ref))))
  ))
  (store %item %new-child-loc)

  (store (+ 1 (load %length-ref)) %length-ref)
  (return-void)
))

(def @u64-vector$ptr.print_ (params (%this %struct.u64-vector*) (%i u64)) void (do
  (if (== (load (index %this 1)) %i) (do
    (return-void)
  ))

  (let %COMMA (+ 44 (0 i8)))
  (let %SPACE (+ 32 (0 i8)))

  (call @i8.print (args %COMMA))
  (call @i8.print (args %SPACE))

  (let %curr (call @u64-vector$ptr.unsafe-get (args %this %i)))
  (call @u64.print (args %curr))

  (call-tail @u64-vector$ptr.print_ (args %this (+ 1 %i)))
  (return-void)
))

(def @u64-vector$ptr.print (params (%this %struct.u64-vector*)) void (do
  (let %LBRACKET (+ 91 (0 i8)))
  (let %RBRACKET (+ 93 (0 i8)))
  (let %COMMA    (+ 44 (0 i8)))
  (let %SPACE    (+ 32 (0 i8)))

  (call @i8.print (args %LBRACKET))
  (if (!= 0 (load (index %this 1))) (do
    (call @u64.print (args (load (load (index %this 0)))))
  ))

  (if (!= 0 (load (index %this 1))) (do
    (call @u64-vector$ptr.print_ (args %this 1))
  ))

  (call @i8.print (args %RBRACKET))
  (return-void)
))

(def @u64-vector$ptr.println (params (%this %struct.u64-vector*)) void (do
  (call @u64-vector$ptr.print (args %this))
  (call @println args)
  (return-void)
))

; unsafe = no bounds checking
(def @u64-vector$ptr.unsafe-get (params (%this %struct.u64-vector*) (%i u64)) u64 (do
  (let %SIZEOF-u64 (+ 8 (0 u64)))
  (return (load (cast u64* (+ (* %SIZEOF-u64 %i) (cast u64 (load (index %this 0)))))))
))

; put a %value in the %i index of a vector
(def @u64-vector$ptr.unsafe-put (params (%this %struct.u64-vector*) (%i u64) (%value u64)) void (do
  (let %SIZEOF-u64 (+ 8 (0 u64)))
  (store %value (cast u64* (+ (* %SIZEOF-u64 %i) (cast u64 (load (index %this 0))))))
  (return-void)
))

;========== u64-vector tests =======================================================================

(def @test.u64-vector-basic params void (do
  (auto %vec %struct.u64-vector)
  (store (call @u64-vector.make args) %vec)

  (call @u64-vector$ptr.push (args %vec 0))
  (call @u64-vector$ptr.push (args %vec 1))
  (call @u64-vector$ptr.push (args %vec 2))
  (call @u64-vector$ptr.push (args %vec 3))
  (call @u64-vector$ptr.push (args %vec 4))
  (call @u64-vector$ptr.push (args %vec 5))
  (call @u64-vector$ptr.push (args %vec 6))

  (call @u64-vector$ptr.println (args %vec))
  (return-void)
))

(def @test.u64-vector-one params void (do
  (auto %vec %struct.u64-vector)
  (store (call @u64-vector.make args) %vec)

  (call @u64-vector$ptr.push (args %vec 0))

  (call @u64-vector$ptr.println (args %vec))
  (return-void)
))

(def @test.u64-vector-empty params void (do
  (auto %vec %struct.u64-vector)
  (store (call @u64-vector.make args) %vec)

  (call @u64-vector$ptr.println (args %vec))
  (return-void)
))

(def @test.u64-vector-put params void (do
  (auto %vec %struct.u64-vector)
  (store (call @u64-vector.make args) %vec)

  (call @u64-vector$ptr.push (args %vec 0))
  (call @u64-vector$ptr.push (args %vec 1))
  (call @u64-vector$ptr.push (args %vec 2))
  (call @u64-vector$ptr.push (args %vec 3))
  (call @u64-vector$ptr.push (args %vec 4))
  (call @u64-vector$ptr.push (args %vec 5))
  (call @u64-vector$ptr.push (args %vec 6))

  (call @u64-vector$ptr.unsafe-put (args %vec 3 10))

  (call @u64-vector$ptr.println (args %vec))
  (return-void)
))