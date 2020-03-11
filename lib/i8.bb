;========== i8* methods ===========================================================================

; NOTE: does not include trailing zero
(def @i8$ptr.length_ (params (%this i8*) (%acc u64)) u64 (do
  (if (== (load %this) 0) (do
    (return %acc)
  ))

  (return (call-tail @i8$ptr.length_ (args (cast i8* (+ 1 (cast u64 %this))) (+ %acc 1))))
))

(def @i8$ptr.length (params (%this i8*)) u64 (do
  (return (call-tail @i8$ptr.length_ (args %this 0)))
))

(def @i8$ptr.printn (params (%this i8*) (%n u64)) void (do
  (let %FD_STDOUT (+ 1 (0 i32)))
  (call @write (args %FD_STDOUT %this %n))
  (return-void)
))

; NOTE: unsafe!
(def @i8$ptr.unsafe-print (params (%this i8*)) void (do
  (let %length (call @i8$ptr.length (args %this)))
  (call @i8$ptr.printn (args %this %length))
  (return-void)
))

(def @i8$ptr.unsafe-println (params (%this i8*)) void (do
  (call @i8$ptr.unsafe-print (args %this))
  (call @println args)
  (return-void)
))

(def @i8$ptr.copyalloc (params (%this i8*)) i8* (do
  (let %length (call @i8$ptr.length (args %this)))
  (let %allocated (call @malloc (args (+ %length 1))))
  (store 0 (cast i8* (+ %length (cast u64 %allocated))))
  (call @memcpy (args %allocated %this %length))
  (return %allocated)
))

(def @i8.print (params (%this i8)) void (do
  (auto %c i8)
  (store %this %c)
  (let %FD_STDOUT (+ 1 (0 i32)))
  (call @write (args %FD_STDOUT %c 1))
  (return-void)
))

(def @i8$ptr.swap (params (%this i8*) (%other i8*)) void (do
  (let %this_value (load %this))
  (let %other_value (load %other))
  (store %this_value %other)
  (store %other_value %this)
  (return-void)
))

(def @i8$ptr.eqn (params (%this i8*) (%other i8*) (%len u64)) i1 (do
  (if (== 0 %len) (do
    (return true)
  ))

  (if (!= (load %this) (load %other)) (do
    (return false)
  ))

  (let %next-this  (cast i8* (+ 1 (cast u64 %this))))
  (let %next-other (cast i8* (+ 1 (cast u64 %other))))
  (return (call @i8$ptr.eqn (args %next-this %next-other (- %len 1))))
))

(def @println params void (do
  (let %NEWLINE (+ 10 (0 i8)))
  (call @i8.print (args %NEWLINE))
  (return-void)
))

;========== i8* tests =============================================================================

(def @test.i8$ptr-eqn params void (do
  (auto %a %struct.String)
  (store (call @String.makeEmpty args) %a)
  (auto %b %struct.String)
  (store (call @String.makeEmpty args) %b)

  (call @String$ptr.pushChar (args %a 65))
  (call @String$ptr.pushChar (args %a 66))
  (call @String$ptr.pushChar (args %a 67))
  (call @String$ptr.pushChar (args %a 68))

  (call @String$ptr.pushChar (args %b 65))
  (call @String$ptr.pushChar (args %b 66))
  (call @String$ptr.pushChar (args %b 67))
  (call @String$ptr.pushChar (args %b 68))

; FIXME suspicious
  (let %a-cstr (cast i8* %a))
  (let %b-cstr (cast i8* %b))

  (call @i8.print (args (+ 48 (cast i8 (call @i8$ptr.eqn (args %a-cstr %b-cstr 5))))))
  (return-void)
))
