;========== Matcher ===============================================================================

(struct %struct.Matcher
  (%grammar %struct.Grammar))

(def @Matcher$ptr.is (params (%this %struct.Matcher*) (%texp %struct.Texp*) (%type-name %struct.StringView*)) %struct.Texp (do
; debug
  (call @i8$ptr.unsafe-print (args " [.is           ]  \00"))
  (call @Texp$ptr.parenPrint (args %texp))
  (call @i8$ptr.unsafe-print (args " -> \00"))
  (call @StringView$ptr.print (args %type-name))

  (let %grammar (index %this 0))
  (let %prod (call @Grammar$ptr.getProduction (args %grammar %type-name)))

; debug
  (call @i8$ptr.unsafe-print (args " @ \00"))
  (call @Texp$ptr.parenPrint (args %prod))
  (call @println args)

  (auto %result %struct.Texp)
  (store (call @Matcher$ptr.match (args %this %texp %prod)) %result)

  (if (call @Texp$ptr.value-check (args %result "success\00")) (do
    (let %child (load (index %result 1)))
    (let %proof-value (index %child 0))

    (auto %new-proof-value %struct.String)
    (store (call @String.makeFromStringView (args %type-name)) %new-proof-value)
    (let %FORWARD_SLASH (+ 47 (0 i8)))
    (call @String$ptr.pushChar (args %new-proof-value %FORWARD_SLASH))
    (call @String$ptr.append (args %new-proof-value %proof-value))

    (call @String$ptr.free (args %proof-value))
    (store (load %new-proof-value) %proof-value)
  ))
  (return (load %result))
))



(def @Matcher$ptr.atom (params (%this %struct.Matcher*) (%texp %struct.Texp*) (%prod %struct.Texp*)) %struct.Texp (do
  (call @i8$ptr.unsafe-println (args " [.atom         ]\00"))

  (if (!= 0 (load (index %texp 2))) (do
    (auto %error-result %struct.Texp)
;   hex '22' is a quote
    (store (call @Result.error-from-i8$ptr (args "\22texp is not an atom\22\00")) %error-result)
    (call @Texp$ptr.push (args %error-result (call @Texp$ptr.clone (args %prod))))
    (return (load %error-result))
  ))
  (return (call @Result.success-from-i8$ptr (args "atom\00")))
))



(def @Matcher$ptr.match (params (%this %struct.Matcher*) (%texp %struct.Texp*) (%prod %struct.Texp*)) %struct.Texp (do

  (let %rule (load (index %prod 1)))
  (let %rule-length (load (index %rule 2)))

; debug
  (call @i8$ptr.unsafe-print (args " [.match        ]  -> \00"))
  (call @Texp$ptr.parenPrint (args %rule))
  
  (if (call @Texp$ptr.value-check (args %rule "|\00")) (do

; debug
    (call @println args)
    
    (return (call @Matcher$ptr.choice (args %this %texp %prod)))
  ))

; debug
  (call @i8$ptr.unsafe-print (args ", rule-length: \00"))
  (call @u64.print (args %rule-length))
  (call @println args)

; (grammar (* (production (rule)))) <- the only child of a production is a (rewrite) rule 

  (auto %value-result %struct.Texp)
  (store (call @Matcher$ptr.value (args %this %texp %prod)) %value-result)
; check for error
  (if (call @Texp$ptr.value-check (args %value-result "error\00")) (do
    (return (load %value-result))
  ))

; TODO use value-result later

  (if (!= %rule-length 0) (do
    (let %last (call @Texp$ptr.last (args %rule)))
    (if (call @Texp$ptr.value-check (args %last "*\00")) (do
      (return (call @Matcher$ptr.kleene (args %this %texp %prod)))
    ))
    (return (call @Matcher$ptr.exact (args %this %texp %prod)))
  ))

  (return (call @Matcher$ptr.atom (args %this %texp %prod)))
))



(def @Matcher$ptr.kleene-seq (params
      (%this %struct.Matcher*) (%texp %struct.Texp*) (%prod %struct.Texp*) (%acc %struct.Texp*)
      (%curr-index u64) (%last-index u64))
    void (do

; debug
  (call @i8$ptr.unsafe-print (args " [.kleene-seq   ]  i: \00"))
  (call @u64.print (args %curr-index))

  (let %rule (load (index %prod 1)))
  (let %last (call @Texp$ptr.last (args %rule)))

  (let %texp-child (call @Texp$ptr.child (args %texp %curr-index)))
  (let %rule-child (call @Texp$ptr.child (args %rule %curr-index)))

; debug
  (call @i8$ptr.unsafe-print (args ", \00"))
  (call @Texp$ptr.parenPrint (args %texp-child))
  (call @i8$ptr.unsafe-print (args " -> :\00"))
  (call @Texp$ptr.parenPrint (args %rule-child))
  (call @println args)

  (auto %result %struct.Texp)
  (store (call @Matcher$ptr.is (args %this %texp-child (call @Texp$ptr.value-view (args %rule-child)))) %result)

; if the check is not successful, clear the accumulator and set the error code.
; CONSIDER caching the accumulator and rearranging the result to keep the current continuation of proof instead of freeing it
  (if (call @Texp$ptr.value-check (args %result "error\00")) (do
    (call @Texp$ptr.free (args %acc))
    (store (load %result) %acc)
    (return-void)
  ))

  (call @Texp$ptr.demote-free (args %result))
  (call @Texp$ptr.push$ptr (args %acc %result))
  (if (== %curr-index %last-index) (do (return-void)))
  (let %next-index (+ 1 %curr-index))
  (call @Matcher$ptr.kleene-seq (args %this %texp %prod %acc %next-index %last-index))
  (return-void)
))



(def @Matcher$ptr.kleene-many (params
      (%this %struct.Matcher*) (%texp %struct.Texp*) (%prod %struct.Texp*) (%acc %struct.Texp*)
      (%curr-index u64) (%last-index u64))
    void (do

; debug
  (call @i8$ptr.unsafe-print (args " [.kleene-many  ]  i: \00"))
  (call @u64.print (args %curr-index))

  (let %rule (load (index %prod 1)))
  (let %last (call @Texp$ptr.last (args %rule)))

  (let %texp-child (call @Texp$ptr.child (args %texp %curr-index)))
  (let %rule-child (call @Texp$ptr.last (args %last)))

; debug
  (call @i8$ptr.unsafe-print (args ", \00"))
  (call @Texp$ptr.parenPrint (args %texp-child))
  (call @i8$ptr.unsafe-print (args " -> :\00"))
  (call @Texp$ptr.parenPrint (args %rule-child))
  (call @println args)
  
  (auto %result %struct.Texp)
  (store (call @Matcher$ptr.is (args %this %texp-child (call @Texp$ptr.value-view (args %rule-child)))) %result)

; if the check is not successful, clear the accumulator and set the error code.
; CONSIDER see kleene-many
  (if (call @Texp$ptr.value-check (args %result "error\00")) (do
    (call @Texp$ptr.free (args %acc))
    (store (load %result) %acc)
    (return-void)
  ))

  (call @Texp$ptr.demote-free (args %result))
  (call @Texp$ptr.push$ptr (args %acc %result))

  (if (== %curr-index %last-index) (do (return-void)))

  (let %next-index (+ 1 %curr-index))
  (call @Matcher$ptr.kleene-many (args %this %texp %prod %acc %next-index %last-index))
  (return-void)
))



(def @Matcher$ptr.kleene (params (%this %struct.Matcher*) (%texp %struct.Texp*) (%prod %struct.Texp*)) %struct.Texp (do

  (let %rule (load (index %prod 1)))

; debug
  (call @i8$ptr.unsafe-print (args " [.kleene       ]  texp: \00"))
  (call @Texp$ptr.parenPrint (args %texp))
  (call @i8$ptr.unsafe-print (args ", rule: \00"))
  (call @Texp$ptr.parenPrint (args %rule))
  (call @println args)
  
  (let %last (call @Texp$ptr.last (args %rule)))

; TODO
; ASSERT grammar is well-formed
; - every kleene's last child has exact one child

  (let %kleene-prod-view (call @String$ptr.view (args (index %last 0))))

; (grammar (* (production (rule))))

  (let %rule-length (load (index %rule 2)))
  (let %texp-length (load (index %texp 2)))

  (auto %proof %struct.Texp)
  (store (call @Texp.makeFromi8$ptr (args "kleene\00")) %proof)

; debug
  (call @i8$ptr.unsafe-print (args " [.kleene       ]  rule-length: \00"))
  (call @u64.print (args %rule-length))
  (call @i8$ptr.unsafe-print (args ", texp-length: \00"))
  (call @u64.print (args %texp-length))
  (call @println args)

  (let %seq-length  (- %rule-length 1))
  (let %last-texp-i (- %texp-length 1))

; debug
  (call @i8$ptr.unsafe-print (args " [.kleene       ]  seq-length: \00"))
  (call @u64.print (args %seq-length))
  (call @i8$ptr.unsafe-print (args ", last-texp-i: \00"))
  (call @u64.print (args %last-texp-i))
  (call @println args)

  (if (< %texp-length %seq-length) (do
    (auto %failure-result %struct.Texp)
    (store (call @Texp.makeFromi8$ptr (args "error\00")) %failure-result)

    (call @Texp$ptr.push (args %failure-result
      (call @Texp.makeFromi8$ptr (args "texp length not less than for rule.len - 1\00"))))
    (call @Texp$ptr.push (args %failure-result (call @Texp$ptr.clone (args %rule))))
    (call @Texp$ptr.push (args %failure-result (call @Texp$ptr.clone (args %texp))))
    (return (load %failure-result))
  ))


  (if (!= 0 %seq-length) (do
    (call @Matcher$ptr.kleene-seq (args %this %texp %prod %proof 0 (- %seq-length 1)))
    (if (call @Texp$ptr.value-check (args %proof "error\00")) (do
      (return (load %proof))
    ))
  ))

; if (!= %seq-length %texp-length) then there is nothing to match with the kleene
  (if (!= %seq-length %texp-length) (do
    (call @Matcher$ptr.kleene-many (args %this %texp %prod %proof %seq-length (- %texp-length 1)))
    (if (call @Texp$ptr.value-check (args %proof "error\00")) (do
      (return (load %proof))
    ))
  ))

  (return (call @Result.success (args %proof)))
))



(def @Matcher$ptr.exact_ (params
      (%this %struct.Matcher*) (%texp %struct.Texp*) (%prod %struct.Texp*) (%acc %struct.Texp*)
      (%curr-index u64) (%last-index u64))
    void (do
  (let %rule (load (index %prod 1)))
  (let %texp-child (call @Texp$ptr.child (args %texp %curr-index)))
  (let %rule-child (call @Texp$ptr.child (args %rule %curr-index)))

; debug
  (call @i8$ptr.unsafe-print (args " [.exact_       ]  \00"))
  (call @Texp$ptr.parenPrint (args %texp-child))
  (call @i8$ptr.unsafe-print (args " -> \00"))
  (call @Texp$ptr.parenPrint (args %rule-child))
  (call @println args)

  (auto %result %struct.Texp)
  (store (call @Matcher$ptr.is (args %this %texp-child (call @Texp$ptr.value-view (args %rule-child)))) %result)

; if the check is not successful, clear the accumulator and set the error code.
; CONSIDER caching the accumulator and rearranging the result to keep the current continuation of proof instead of freeing it
  (if (call @Texp$ptr.value-check (args %result "error\00")) (do
    (call @Texp$ptr.free (args %acc))
    (store (load %result) %acc)
    (return-void)
  ))

  (call @Texp$ptr.demote-free (args %result))
  (call @Texp$ptr.push$ptr (args %acc %result))
  (if (== %curr-index %last-index) (do (return-void)))
  (let %next-index (+ 1 %curr-index))
  (call @Matcher$ptr.exact_ (args %this %texp %prod %acc %next-index %last-index))
  (return-void)
))



(def @Matcher$ptr.exact (params (%this %struct.Matcher*) (%texp %struct.Texp*) (%prod %struct.Texp*)) %struct.Texp (do

  (let %rule (load (index %prod 1)))
  (if (!= (load (index %texp 2)) (load (index %rule 2))) (do
    (auto %len-result %struct.Texp)
;   hex \22 is the double quote character
    (store (call @Result.error-from-i8$ptr (args "\22texp has incorrect length for exact sequence\22\00")) %len-result)
    (call @Texp$ptr.push (args %len-result (call @Texp$ptr.clone (args %texp))))
    (call @Texp$ptr.push (args %len-result (call @Texp$ptr.clone (args %rule))))
    (return (load %len-result))
  ))

; debug
  (call @i8$ptr.unsafe-print (args " [.exact        ]  \00"))
  (call @Texp$ptr.parenPrint (args %texp))
  (call @i8$ptr.unsafe-print (args " -> \00"))
  (call @Texp$ptr.parenPrint (args %rule))
  (call @println args)


  (auto %proof %struct.Texp)
  (store (call @Texp.makeFromi8$ptr (args "exact\00")) %proof)

  (let %last (- (load (index %texp 2)) 1))
  (call @Matcher$ptr.exact_ (args %this %texp %prod %proof 0 %last))

  (auto %proof-success-wrapper %struct.Texp)
  (store (call @Texp.makeFromi8$ptr (args "success\00")) %proof-success-wrapper)
  (call @Texp$ptr.push$ptr (args %proof-success-wrapper %proof))
  (return (load %proof-success-wrapper))
))



(def @Matcher$ptr.choice_ (params
         (%this %struct.Matcher*) (%texp %struct.Texp*) (%prod %struct.Texp*)
         (%i u64) (%attempts %struct.Texp*)) %struct.Texp (do

  (let %rule (load (index %prod 1)))
  (let %rule-child (call @Texp$ptr.child (args %rule %i)))

; debug
  (call @i8$ptr.unsafe-print (args " [.choice_      ]  i: \00"))
  (call @u64.print (args %i))
  (call @i8$ptr.unsafe-print (args ", \00"))
  (call @Texp$ptr.parenPrint (args %texp))
  (call @i8$ptr.unsafe-print (args " -> :\00"))
  (call @Texp$ptr.parenPrint (args %rule-child))
  (call @println args)

; TODO
; ASSERT grammar is well-formed
; - choices have nonzero numbers of children
; - each child of a choice is empty

  (let %rule-child-view (call @Texp$ptr.value-view (args %rule-child)))

  (auto %is-result %struct.Texp)
  (store (call @Matcher$ptr.is (args %this %texp %rule-child-view)) %is-result)

  (if (call @Texp$ptr.value-check (args %is-result "success\00")) (do
    (let %proof-value-ref (index (load (index %is-result 1)) 0))
    (auto %choice-marker %struct.String)
    (store (call @String.makeFromi8$ptr (args "choice->\00")) %choice-marker)
    (call @String$ptr.prepend (args %proof-value-ref %choice-marker))
    
    (return (load %is-result))
  ))

  (auto %keyword-result %struct.Texp)
  (store (call @Grammar$ptr.get-keyword (args (index %this 0) %rule-child-view)) %keyword-result)
  (if (call @StringView$ptr.eq (args
        (call @Texp$ptr.value-view (args %texp))
        (call @Texp$ptr.value-view (args %keyword-result))
      )) (do
    (auto %keyword-error-result %struct.Texp)
    (store (call @Result.error-from-i8$ptr (args "keyword-choice-match\00")) %keyword-error-result)
    (call @Texp$ptr.push (args %keyword-error-result (call @Texp$ptr.clone (args %prod))))
    (call @Texp$ptr.push (args %keyword-error-result (call @Texp$ptr.clone (args %texp))))
    (call @Texp$ptr.push$ptr (args %keyword-error-result %is-result))
    (return (load %keyword-error-result))
  ))


  (let %last-rule-index (- (load (index %rule 2)) 1))
  (if (== %i %last-rule-index) (do
    (auto %choice-match-error-result %struct.Texp)
    (store (call @Result.error-from-i8$ptr (args "choice-match\00")) %choice-match-error-result)
    (call @Texp$ptr.push (args %choice-match-error-result (call @Texp$ptr.clone (args %prod))))
    (call @Texp$ptr.push (args %choice-match-error-result (call @Texp$ptr.clone (args %texp))))
    (call @Texp$ptr.push$ptr (args %choice-match-error-result %attempts))
    (return (load %choice-match-error-result))
  ))

  (call @Texp$ptr.push$ptr (args %attempts %is-result))
  
  (return (call @Matcher$ptr.choice_ (args %this %texp %prod (+ 1 %i) %attempts)))
))



(def @Matcher$ptr.choice (params (%this %struct.Matcher*) (%texp %struct.Texp*) (%prod %struct.Texp*)) %struct.Texp (do

; debug
  (call @i8$ptr.unsafe-print (args " [.choice       ]  -> \00"))
  (call @Texp$ptr.parenPrint (args (call @Texp$ptr.last (args %prod))))
  (call @println args)

  (auto %proof %struct.Texp)
  (store (call @Texp.makeFromi8$ptr (args "choice\00")) %proof)

  (auto %attempts %struct.Texp)
  (store (call @Texp.makeFromi8$ptr (args "choice-attempts\00")) %attempts)

  (return (call @Matcher$ptr.choice_ (args %this %texp %prod 0 %attempts)))
))


; TODO consider returning results from regexes
(def @Matcher.regexInt_ (params (%curr i8*) (%len u64)) i1 (do
  (if (== 0 %len) (do (return true)))

  (let %ZERO (+ 48 (0 i8)))
  (let %offset (- (load %curr) %ZERO))

; debug
; (call @u64.print (args %len))
; (call @i8$ptr.unsafe-print (args " \00"))
; (call @u64.print (args (cast u64 (cast u8 (load %curr)))))
; (call @i8$ptr.unsafe-print (args " \00"))
; (call @u64.println (args (cast u64 (cast u8 %offset))))

  (if (< %offset 0)   (do (return false)))
  (if (>= %offset 10) (do (return false)))

  (return (call @Matcher.regexInt_ (args (cast i8* (+ 1 (cast u64 %curr))) (- %len 1))))
))



(def @Matcher.regexInt (params (%texp %struct.Texp*)) i1 (do
  (let %view (call @Texp$ptr.value-view (args %texp)))

; debug
; (call @u64.println (args (cast u64 (load (index %view 1)))))

  (return (call @Matcher.regexInt_ (args (load (index %view 0)) (load (index %view 1)))))
))



; TODO
(def @Matcher.regexString_ (params (%curr i8*) (%len u64)) i1 (do
  (return true)
))



(def @Matcher.regexString (params (%texp %struct.Texp*)) i1 (do
  (let %view (call @Texp$ptr.value-view (args %texp)))
  (let %curr (load (index %view 0)))
  (let %len  (load (index %view 1)))

  (if (< %len 2) (do (return false)))
  (let %last (cast i8* (+ (- %len 1) (cast u64 %curr))))

  (let %QUOTE (+ 34 (0 i8)))
  (if (!= %QUOTE (load %curr)) (do (return false)))
  (if (!= %QUOTE (load %last)) (do (return false)))

  (let %next (cast i8* (+ 1 (cast u64 %curr))))

  (return (call @Matcher.regexString_ (args %next (- %len 2))))
))



(def @Matcher.regexBool (params (%texp %struct.Texp*)) i1 (do
  (if (call @Texp$ptr.value-check (args %texp "true\00"))  (do (return true)))
  (if (call @Texp$ptr.value-check (args %texp "false\00")) (do (return true)))
  (return false)
))



(def @Matcher$ptr.value (params (%this %struct.Matcher*) (%texp %struct.Texp*) (%prod %struct.Texp*)) %struct.Texp (do

  (let %rule (load (index %prod 1)))

  (let %texp-value-view-ref (call @Texp$ptr.value-view (args %texp)))
  (let %rule-value-view-ref (call @Texp$ptr.value-view (args %rule)))

; debug
  (call @i8$ptr.unsafe-print (args " [.value        ]  \00"))
  (call @StringView$ptr.print (args %texp-value-view-ref))
  (call @i8$ptr.unsafe-print (args " -> \00"))
  (call @StringView$ptr.print (args %rule-value-view-ref))
  (call @println args)

  (auto %rule-value-texp %struct.Texp)
  (store (call @Texp.makeFromStringView (args %rule-value-view-ref)) %rule-value-texp)
  (auto %texp-value-texp %struct.Texp)
  (store (call @Texp.makeFromStringView (args %texp-value-view-ref)) %texp-value-texp)

  (let %default-success (call @Result.success (args %rule-value-texp)))

; hash character, '#'
  (let %HASH (+ 35 (0 i8)))
  (let %cond (== %HASH (call @Texp$ptr.value-get (args %rule 0))))

  (auto %error-result %struct.Texp)
  (store (call @Texp.makeEmpty args) %error-result)

  (if %cond (do

    (if (call @Texp$ptr.value-check (args %rule "#int\00")) (do
      (if (call @Matcher.regexInt (args %texp)) (do
        (return %default-success)
      ))
      (store (call @Result.error-from-i8$ptr (args "failed to match #int\22\00")) %error-result)
    ))

    (if (call @Texp$ptr.value-check (args %rule "#string\00")) (do
      (if (call @Matcher.regexString (args %texp)) (do
        (return %default-success)
      ))
      (store (call @Result.error-from-i8$ptr (args "\22failed to match #string\22\00")) %error-result)
    ))

    (if (call @Texp$ptr.value-check (args %rule "#bool\00")) (do
      (if (call @Matcher.regexBool (args %texp)) (do
        (return %default-success)
      ))
      (store (call @Result.error-from-i8$ptr (args "\22failed to match #bool\22\00")) %error-result)
    ))

; TODO
    (if (call @Texp$ptr.value-check (args %rule "#type\00")) (do
      (return %default-success)
    ))

; TODO
    (if (call @Texp$ptr.value-check (args %rule "#name\00")) (do
      (return %default-success)
    ))

;   check for errors
    (if (call @Texp$ptr.value-check (args %error-result "error\00")) (do
      (call @Texp$ptr.push$ptr (args %error-result %texp))
      (return (load %error-result))
    ))

;   error out if no regex class matches and no error
    (call @i8$ptr.unsafe-print (args "unmatched regex check for rule value: \00"))
    (call @StringView$ptr.print (args %rule-value-view-ref))
    (call @i8$ptr.unsafe-print (args ", rule: \00"))
    (call @Texp$ptr.parenPrint (args %rule))
    (call @i8$ptr.unsafe-print (args ", \00"))
    (call @Texp$ptr.parenPrint (args %error-result))
    (call @println args)
    (call @exit (args 1))
  ))

; rule.value doesn't start with '#'
  (if (call @StringView$ptr.eq (args %rule-value-view-ref %texp-value-view-ref)) (do
    (return %default-success)
  ))

  (auto %keyword-match-error %struct.Texp)
  (store (call @Texp.makeFromi8$ptr (args "error\00")) %keyword-match-error)
  (call @Texp$ptr.push (args %keyword-match-error (call @Texp.makeFromi8$ptr (args "keyword-match\00"))))
  (call @Texp$ptr.push (args %keyword-match-error (call @Texp$ptr.clone (args %rule-value-texp))))
  (call @Texp$ptr.push (args %keyword-match-error (call @Texp$ptr.clone (args %texp-value-texp))))
  (return (load %keyword-match-error))
; ))
; end else branch
))

;========== Matcher tests ==========================================================================

(def @test.matcher-simple params void (do

  (auto %prog %struct.Texp)
  (store (call @Parser.parse-file-i8$ptr (args "/home/kasra/projects/backbone-test/matcher/program.texp\00")) %prog)

  (auto %matcher %struct.Matcher)
  (store (call @Grammar.make (args (call @Parser.parse-file-i8$ptr (args "/home/kasra/projects/backbone-test/matcher/program.grammar\00")))) (index %matcher 0))

  (auto %start-production %struct.StringView)
  (store (call @StringView.makeFromi8$ptr (args "Program\00")) %start-production)

  (auto %result %struct.Texp)
  (store (call @Matcher$ptr.is (args %matcher %prog %start-production)) %result)
  (call @Texp$ptr.parenPrint (args %result))
  (call @println args)

  (return-void)
))

(def @test.matcher-choice params void (do

  (auto %prog %struct.Texp)
  (store (call @Parser.parse-file-i8$ptr (args "/home/kasra/projects/backbone-test/matcher/choice.texp\00")) %prog)

  (auto %matcher %struct.Matcher)
  (store (call @Grammar.make (args (call @Parser.parse-file-i8$ptr (args "/home/kasra/projects/backbone-test/matcher/choice.grammar\00")))) (index %matcher 0))

  (auto %start-production %struct.StringView)
  (store (call @StringView.makeFromi8$ptr (args "Program\00")) %start-production)

  (auto %result %struct.Texp)
  (store (call @Matcher$ptr.is (args %matcher %prog %start-production)) %result)
  (call @Texp$ptr.parenPrint (args %result))
  (call @println args)

  (return-void)
))

(def @test.matcher-kleene-seq params void (do

  (auto %prog %struct.Texp)
  (store (call @Parser.parse-file-i8$ptr (args "/home/kasra/projects/backbone-test/matcher/seq-kleene.texp\00")) %prog)

  (auto %matcher %struct.Matcher)
  (store (call @Grammar.make (args (call @Parser.parse-file-i8$ptr (args "/home/kasra/projects/backbone-test/matcher/seq-kleene.grammar\00")))) (index %matcher 0))

  (auto %start-production %struct.StringView)
  (store (call @StringView.makeFromi8$ptr (args "Program\00")) %start-production)

  (auto %result %struct.Texp)
  (store (call @Matcher$ptr.is (args %matcher %prog %start-production)) %result)
  (call @Texp$ptr.parenPrint (args %result))
  (call @println args)

  (return-void)
))

(def @test.matcher-exact params void (do

  (auto %prog %struct.Texp)
  (store (call @Parser.parse-file-i8$ptr (args "/home/kasra/projects/backbone-test/matcher/exact.texp\00")) %prog)

  (auto %matcher %struct.Matcher)
  (store (call @Grammar.make (args (call @Parser.parse-file-i8$ptr (args "/home/kasra/projects/backbone-test/matcher/exact.grammar\00")))) (index %matcher 0))

  (auto %start-production %struct.StringView)
  (store (call @StringView.makeFromi8$ptr (args "Program\00")) %start-production)

  (auto %result %struct.Texp)
  (store (call @Matcher$ptr.is (args %matcher %prog %start-production)) %result)
  (call @Texp$ptr.parenPrint (args %result))
  (call @println args)

  (return-void)
))

(def @test.matcher-value params void (do
  (auto %prog %struct.Texp)
  (store (call @Parser.parse-file-i8$ptr (args "/home/kasra/projects/backbone-test/matcher/value.texp\00")) %prog)

  (auto %matcher %struct.Matcher)
  (store (call @Grammar.make (args (call @Parser.parse-file-i8$ptr (args "/home/kasra/projects/backbone-test/matcher/value.grammar\00")))) (index %matcher 0))

  (auto %start-production %struct.StringView)
  (store (call @StringView.makeFromi8$ptr (args "Program\00")) %start-production)

  (auto %result %struct.Texp)
  (store (call @Matcher$ptr.is (args %matcher %prog %start-production)) %result)
  (call @Texp$ptr.parenPrint (args %result))
  (call @println args)

  (return-void)
))

(def @test.matcher-empty-kleene params void (do
  (auto %prog %struct.Texp)
  (store (call @Parser.parse-file-i8$ptr (args "/home/kasra/projects/backbone-test/matcher/empty-kleene.texp\00")) %prog)

  (auto %matcher %struct.Matcher)
  (store (call @Grammar.make (args (call @Parser.parse-file-i8$ptr (args "/home/kasra/projects/backbone-test/matcher/empty-kleene.grammar\00")))) (index %matcher 0))

  (auto %start-production %struct.StringView)
  (store (call @StringView.makeFromi8$ptr (args "Program\00")) %start-production)

  (auto %result %struct.Texp)
  (store (call @Matcher$ptr.is (args %matcher %prog %start-production)) %result)
  (call @Texp$ptr.parenPrint (args %result))
  (call @println args)

  (return-void)
))

(def @test.matcher-self params void (do
  (auto %filename %struct.StringView)
	(call @StringView$ptr.set (args %filename "lib2/matcher.bb\00"))

  (auto %prog %struct.Texp)
  (store (call @Parser.parse-file (args %filename)) %prog)

  (auto %matcher %struct.Matcher)
  (store (call @Grammar.make (args (call @Parser.parse-file-i8$ptr (args "docs/bb-type-tall-str-include-grammar.texp\00")))) (index %matcher 0))

  (auto %start-production %struct.StringView)
  (store (call @StringView.makeFromi8$ptr (args "Program\00")) %start-production)

  (auto %result %struct.Texp)
  (store (call @Matcher$ptr.is (args %matcher %prog %start-production)) %result)
  (call @Texp$ptr.parenPrint (args %result))
  (call @println args)

  (return-void)
))

(def @test.matcher-regexString params void (do
; hex 22 is a quote
; hex 5c is a backslash
  (auto %string %struct.Texp)
  (store (call @Texp.makeFromi8$ptr (args "\22hello, world\22\00")) %string)

  (if (call @Matcher.regexString (args %string)) (do
    (call @i8$ptr.unsafe-println (args "PASSED\00"))
    (return-void)
  ))
  (call @i8$ptr.unsafe-println (args "FAILED\00"))
  (return-void)
))

(def @test.matcher-regexInt params void (do
; hex 22 is a quote
; hex 5c is a backslash
  (auto %string %struct.Texp)
  (let %actual (+ 1234567890 (0 u64)))
  (store (call @Texp.makeFromi8$ptr (args "0123456789\00")) %string)

  (if (call @Matcher.regexInt (args %string)) (do
    (call @i8$ptr.unsafe-println (args "PASSED\00"))
    (return-void)
  ))
  (call @i8$ptr.unsafe-println (args "FAILED\00"))
  (return-void)
))