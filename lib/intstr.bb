
;========== int string conv =======================================================================

(def @u64.string_ (params (%this u64) (%acc %struct.String*)) void (do
  (if (== 0 %this) (do
    (return-void)
  ))

  (let %ZERO (+ 48 (0 i8)))
  (let %top  (% %this 10))
  (let %c (+ %ZERO (cast i8 %top)))
  (call @String$ptr.pushChar (args %acc %c))

  (let %rest (/ %this 10))
  (call-tail @u64.string_ (args %rest %acc))
  (return-void)
))

(def @u64.string (params (%this u64)) %struct.String (do
  (auto %acc %struct.String)
  (store (call @String.makeEmpty args) %acc)

  (let %ZERO (+ 48 (0 i8)))

  (if (== 0 %this) (do
    (call @String$ptr.pushChar (args %acc %ZERO))
    (return (load %acc))
  ))

  (call @u64.string_ (args %this %acc))
  (call @String$ptr.reverse-in-place (args %acc))

  (return (load %acc))
))

(def @u64.print (params (%this u64)) void (do
  (auto %string %struct.String)
  (store (call @u64.string (args %this)) %string)
  (call @String$ptr.print (args %string))
  (return-void)
))

(def @u64.println (params (%this u64)) void (do
  (call @u64.print (args %this))
  (call @println args)
  (return-void)
))

;========== int string conv tests =================================================================

(def @test.u64-print params void (do
  (call @u64.print (args 12408124))
  (call @println args)
  (return-void)
))
