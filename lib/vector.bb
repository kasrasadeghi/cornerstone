(struct %struct.u64-vector
  (%data u64*)
  (%len u64)
  (%cap u64))

(def @u64-vector.make params %struct.u64-vector (do
  (auto %result %struct.u64-vector)
  (store (cast i8* (0 u64)) (index %result 0))
  (store 0 (index %result 1))
  (store 0 (index %result 2))
  (return (load %result))
))

(def @u64-vector$ptr.push (params (%this %struct.u64-vector*) (%x u64)) void (do
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
    (let %new-data (cast %struct.Texp* (call @realloc (args 
      (cast i8* %old-data) 
      (* %SIZEOF-u64 %new-capacity)))))
    (store %new-data (index %this 0))
  ))

  (let %data-base (cast u64 (load %data-ref)))
  (let %new-child-loc (cast %struct.Texp*
    (+ (* %SIZEOF-u64 (cast u64 (load %length-ref))) %data-base)
  ))
  (store (load %item) %new-child-loc)

  (store (+ 1 (load %length-ref)) %length-ref)
  (return-void)
))

; unsafe = no bounds checking
(def @u64-vector$ptr.unsafe-get (params (%this %struct.u64*) (%i u64)) u64 (do
  (let %SIZE-u64 (+ 8 (0 u64)))
  (return (load (cast u64* (+ (* %SIZEOF-u64 %i) (cast u64 (load (index %this 0)))))))
))

;========== u64-vector tests =======================================================================

(def @test.u64-vector-basic params void (do
  (auto %vec %struct.u64-vector)
  (store (call @u64-vector.make args) %vec)
  (return-void)
))