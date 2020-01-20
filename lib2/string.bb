;========== StringView ============================================================================

; NOTE: does not necessarily own trailing zero, but would be included in length otherwise
(struct %struct.StringView
  (%ptr i8*)
  (%size u64))

(def @StringView.makeEmpty params %struct.StringView (do
  (auto %result %struct.StringView)
  (store (cast i8* (0 i64)) (index %result 0))
  (store 0 (index %result 1))
  (return (load %result))
))

(def @StringView$ptr.set (params (%this %struct.StringView*) (%charptr i8*)) void (do
  (store %charptr (index %this 0))
  (store (call @i8$ptr.length (args %charptr)) (index %this 1))
  (return-void)
))

(def @StringView.make (params (%charptr i8*) (%size u64)) %struct.StringView (do
  (auto %result %struct.StringView)
  (store %charptr (index %result 0))
  (store %size (index %result 1))
  (return (load %result))
))

(def @StringView.makeFromi8$ptr (params (%charptr i8*)) %struct.StringView (do
  (auto %result %struct.StringView)
  (store %charptr (index %result 0))
  (store (call @i8$ptr.length (args %charptr)) (index %result 1))
  (return (load %result))
))

(def @StringView$ptr.print (params (%this %struct.StringView*)) void (do
  (call @i8$ptr.printn (args (load (index %this 0)) (load (index %this 1))))
  (return-void)
))

(def @StringView$ptr.println (params (%this %struct.StringView*)) void (do
  (call @StringView$ptr.print (args %this))
  (call @println args)
  (return-void)
))

(def @StringView.print (params (%this %struct.StringView)) void (do
  (auto %local %struct.StringView)
  (store %this %local)
  (call @i8$ptr.printn (args (load (index %local 0)) (load (index %local 1))))
  (return-void)
))

(def @StringView.println (params (%this %struct.StringView)) void (do
  (call @StringView.print (args %this))
  (call @println args)
  (return-void)
))

(def @StringView$ptr.eq (params (%this %struct.StringView*) (%other %struct.StringView*)) i1 (do
  (let %len (load (index %this 1)))
  (let %olen (load (index %other 1)))
  (if (!= %len %olen) (do
    (return false)
  ))
  (return (call @i8$ptr.eqn (args (load (index %this 0)) (load (index %other 0)) %len)))
))

(def @StringView.eq (params (%this-value %struct.StringView) (%other-value %struct.StringView)) i1 (do
  (auto %this %struct.StringView)
  (store %this-value %this)
  (auto %other %struct.StringView)
  (store %other-value %other)
  (return (call @StringView$ptr.eq (args %this %other)))
))

;========== String ================================================================================

; sizeof(String) == 8 + 8 == 16
; NOTE: does not include trailing zero in size, but %ptr owns trailing zero in memory allocation
(struct %struct.String
  (%ptr i8*)
  (%size u64)
)

(def @String.makeEmpty params %struct.String (do
  (auto %result %struct.String)
  (store (cast i8* (0 i64)) (index %result 0))
  (store 0 (index %result 1))
  (return (load %result))
))

; allocates memory using @i8$ptr.copyalloc
; TODO investigate introducing some kind of notation for ownership of result
(def @String$ptr.set (params (%this %struct.String*) (%charptr i8*)) void (do
  (store (call @i8$ptr.copyalloc (args %charptr)) (index %this 0))
  (store (- (call @i8$ptr.length (args %charptr)) 1) (index %this 1))
  (return-void)
))

; i8$ptr.copyalloc allocates one extra space for zero terminated character
; TODO TEST: i8$ptr.length should not include the zero character in length
(def @String.makeFromi8$ptr (params (%charptr i8*)) %struct.String (do
  (auto %this %struct.String)
  (store (call @i8$ptr.copyalloc (args %charptr)) (index %this 0))
  (store (call @i8$ptr.length (args %charptr)) (index %this 1))
  (return (load %this))
))

(def @String$ptr.copyalloc (params (%this %struct.String*)) %struct.String (do
  (auto %result %struct.String)
  (store (call @i8$ptr.copyalloc (args (load (index %this 0)))) (index %result 0))
  (store (load (index %this 1)) (index %result 1))
  (return (load %result))
))

; TODO TEST
(def @String.makeFromStringView (params (%other %struct.StringView*)) %struct.String (do
  (let %len (load (index %other 1)))

  (auto %result %struct.String)
  (store (call @malloc (args (+ 1 %len))) (index %result 0))
  (call @memcpy (args (load (index %result 0)) (load (index %other 0)) %len))
  (store 0 (cast i8* (+ %len (cast u64 (load (index %result 0))))))
  (store %len (index %result 1))
  (return (load %result))
))

(def @String$ptr.is-empty (params (%this %struct.String*)) i1 (do
  (return (== 0 (load (index %this 1))))
))

(def @String$ptr.view (params (%this %struct.String*)) %struct.StringView (do
  (return (load (cast %struct.StringView* %this)))
))

(def @String$ptr.free (params (%this %struct.String*)) void (do
  (call @free (args (load (index %this 0))))
  (return-void)
))

(def @String$ptr.setFromChar (params (%this %struct.String*) (%c i8)) void (do
  (let %ptr-ref (index %this 0))
  (let %size-ref (index %this 1))

  (let %ptr (call @malloc (args 2)))
  (store %c %ptr)
  (store 0 (cast i8* (+ 1 (cast u64 %ptr))))
  (store %ptr %ptr-ref)
  (store 1 %size-ref)
  (return-void)
))

; maintains ownership of %this but does not consume ownership of %other
(def @String$ptr.append (params 
    (%this %struct.String*) 
    (%other %struct.String*)) void (do

; TODO use more advanced String$ptr.eq

  (let %same-string (== %this %other))
  (if %same-string (do
    (auto %temp-copy %struct.String)
    (store (call @String$ptr.copyalloc (args %other)) %temp-copy)

    (call @String$ptr.append (args %this %temp-copy))
    (call @free (args (load (index %temp-copy 0))))
    (return-void)
  ))

  (let %old-length (load (index %this 1)))
  (let %new-length (+ %old-length (load (index %other 1))))
  (store 
    (call @realloc (args (load (index %this 0)) (+ 1 %new-length)))
    (index %this 0))

; get end of string with old size
  (let %end-of-this-string (call @String$ptr.end (args %this)))

; NOTE: make sure to store new length **after** getting end of string in reallocated chunk
  (store %new-length (index %this 1))

  (call @memcpy (args
    %end-of-this-string 
    (load (index %other 0)) 
    (load (index %other 1))))
  
  (return-void)
))

;; prepends %other before %this
; maintains ownership of %this, does not consume ownership of %other
(def @String$ptr.prepend (params (%this %struct.String*) (%other %struct.String*)) void (do
  (let %same-string (== %this %other))
  (if %same-string (do
    (auto %temp-copy %struct.String)
    (store (call @String$ptr.copyalloc (args %other)) %temp-copy)

    (call @String$ptr.append (args %this %temp-copy))
    (call @free (args (load (index %temp-copy 0))))
    (return-void)
  ))

  (let %old-length   (load (index %this 1)))
  (let %other-length (load (index %other 1)))
  (let %new-length   (+ %old-length %other-length))
  (store %new-length (index %this 1))
  (store 
    (call @realloc (args (load (index %this 0)) (+ 1 %new-length)))
    (index %this 0))

;               /- midpoint
;               v
; [ <- other -> | <- this -> ]

  (let %new-start (load (index %this 0)))
  (let %other-start (load (index %other 0)))
  (let %midpoint (cast i8* (+ %other-length (cast u64 %new-start))))
  (call @memmove (args %midpoint  %new-start   %old-length))
  (call @memcpy  (args %new-start %other-start %other-length))
  (return-void)
))

; takes ownership of neither and creates a new string
(def @String.add (params (%left %struct.String*) (%right %struct.String*)) %struct.String (do
  (auto %result %struct.String)
  (store (call @String$ptr.copyalloc (args %left)) %result)
  (call @String$ptr.append (args %result %right))
  (return (load %result))
))

(def @String$ptr.end (params (%this %struct.String*)) i8* (do
  (let %begin (load (index %this 0)))
  (let %length (load (index %this 1)))
  (let %one-past-last (cast i8* (+ (cast u64 %begin) %length)))
  (return %one-past-last)
))

(def @String$ptr.pushChar (params (%this %struct.String*) (%c i8)) void (do
  (let %ptr-ref (index %this 0))
  (let %size-ref (index %this 1))

  (if (== 0 (cast u64 (load %ptr-ref))) (do
;   TODO assert size is also zero
    (call-tail @String$ptr.setFromChar (args %this %c))
    (return-void)
  ))

  (let %old-size (load %size-ref))
; the size does not include the terminating byte, but it is guaranteed to be there
; add 1 for the null character and 1 for the pushed character, 2 total
  (store (call @realloc (args (load %ptr-ref) (+ 2 %old-size))) %ptr-ref)
  (store (+ 1 %old-size) %size-ref)

  (let %new-char-loc (cast i8* (+ %old-size (cast u64 (load (%ptr-ref))))))
  (store %c %new-char-loc)
  (return-void)
))

(def @reverse-pair (params (%begin i8*) (%end i8*)) void (do
  (if (>= (cast u64 %begin) (cast u64 %end)) (do
    (return-void)
  ))
  (call @i8$ptr.swap (args %begin %end))
  (let %next-begin (cast i8* (+ (cast u64 %begin) 1)))
  (let %next-end   (cast i8* (- (cast u64 %end) 1)))
  (call-tail @reverse-pair (args %next-begin %next-end))
  (return-void)
))

(def @String$ptr.reverse-in-place (params (%this %struct.String*)) void (do
  (let %begin (load (index %this 0)))
  (let %size (load (index %this 1)))
  (if (== 0 %size) (do (return-void)))
; begin + size -> null char, so begin + size - 1 -> last char
  (let %end (cast i8* (+ (- %size 1) (cast u64 %begin))))
  (call-tail @reverse-pair (args %begin %end))
  (return-void)
))

(def @String$ptr.char-at-unsafe (params (%this %struct.String*) (%i u64)) i8 (do
  (let %begin (load (index %this 0)))
  (return (load (cast i8* (+ %i (cast u64 %begin)))))
))

; even though Strings own an extra byte at the end, we don't have to print it because it's definitionally null char
(def @String$ptr.print (params (%this %struct.String*)) void (do
  (call @i8$ptr.printn (args (load (index %this 0)) (load (index %this 1))))
  (return-void)
))

(def @String$ptr.println (params (%this %struct.String*)) void (do
  (call @i8$ptr.printn (args (load (index %this 0)) (load (index %this 1))))
  (call @println args)
  (return-void)
))

;========== string tests ==========================================================================

(def @test.strlen params void (do
  (let %str-example "global string example\00")
  (call-vargs @printf (args 
    "'%s' has length %lu.\0A\00" "global string example\00" (call @i8$ptr.length (args "global string example\00"))))  
  (return-void)
))

(def @test.strview params void (do
  (auto %string-view %struct.StringView)
  (store (call @StringView.makeEmpty args) %string-view)
  (call @StringView$ptr.set (args %string-view "global string example\00"))
  (call-vargs @printf (args 
    "'%s' has length %lu.\0A\00" (load (index %string-view 0)) (load (index %string-view 1))))
  (return-void)
))

(def @test.basic-string params void (do
  (auto %string %struct.String)
  (call @String$ptr.set (args %string "basic-string test\00"))
  (call-vargs @printf (args
    "'%s' has length %lu.\0A\00" (load (index %string 0)) (load (index %string 1))))
  (return-void)
))

(def @test.string-self-append params void (do
  (auto %string %struct.String)
  (call @String$ptr.set (args %string "string-self-append test\00"))
  (call @String$ptr.append (args %string %string))
  (call @puts (args (load (index %string 0))))
  (return-void)
))

(def @test.string-append-helloworld params void (do
  (auto %hello %struct.String)
  (call @String$ptr.set (args %hello "hello, \00"))

  (auto %world %struct.String)
  (call @String$ptr.set (args %world "world\00"))

  (call @String$ptr.append (args %hello %world))
  (call @puts (args (load (index %hello 0))))

  (return-void)
))

(def @test.string-pushchar params void (do
  (auto %acc %struct.String)
  (store (call @String.makeEmpty args) %acc)

  (let %A (+ 65 (0 i8)))

  (call @String$ptr.pushChar (args %acc %A))
  (call @puts (args (load (index %acc 0))))

  (call @String$ptr.pushChar (args %acc %A))
  (call @puts (args (load (index %acc 0))))
  
  (call @String$ptr.pushChar (args %acc %A))
  (call @puts (args (load (index %acc 0))))

  (return-void)
))

(def @test.string-reverse-in-place params void (do
  (auto %acc %struct.String)
  (store (call @String.makeEmpty args) %acc)

  (let %A (+ 65 (0 i8)))

  (call @String$ptr.pushChar (args %acc %A))
  (call @puts (args (load (index %acc 0))))

  (call @String$ptr.pushChar (args %acc (+ 1 %A)))
  (call @puts (args (load (index %acc 0))))
  
  (call @String$ptr.pushChar (args %acc (+ 2 %A)))
  (call @puts (args (load (index %acc 0))))

  (let %begin (load (index %acc 0)))
  (let %size (load (index %acc 1)))
  (let %end (cast i8* (+ (- %size 1) (cast u64 %begin))))

  (call @u64.print (args %size))
  (call @i8.print (args (load %begin)))
  (call @i8.print (args (load %end)))
  (call @println args)

  (call @i8$ptr.swap (args %begin %end))
  (call @puts (args (load (index %acc 0))))

  (call @String$ptr.reverse-in-place (args %acc))
  (call @puts (args (load (index %acc 0))))

  (return-void)
))

(def @test.stringview-nonpointer-eq params void (do
  (let %passed (call @StringView.eq (args
    (call @StringView.makeFromi8$ptr (args "this is a comparison string\00"))
    (call @StringView.makeFromi8$ptr (args "this is a comparison string\00"))
  )))
  (if %passed (do
    (call @puts (args "PASSED\00"))
    (return-void)
  ))
  (call @puts (args "FAILED\00"))
  (return-void)
))

(def @test.string-prepend-helloworld params void (do
  (auto %hello %struct.String)
  (call @String$ptr.set (args %hello "hello, \00"))

  (auto %world %struct.String)
  (call @String$ptr.set (args %world "world\00"))

  (call @String$ptr.prepend (args %world %hello))
  (call @String$ptr.println (args %world))

  (call @String$ptr.free (args %world))
  (call @String$ptr.free (args %hello))
  
  (return-void)
))