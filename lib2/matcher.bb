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
  (call @u64.println (args %curr-index))

  (let %rule (load (index %prod 1)))
  (let %last (call @Texp$ptr.last (args %rule)))

  (let %texp-child (call @Texp$ptr.child (args %texp %curr-index)))
  (let %rule-child (call @Texp$ptr.child (args %rule %curr-index)))

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
  
  (let %last-length (load (index %last 2)))
  (if (!= 1 %last-length) (do
    (call @i8$ptr.unsafe-println (args "a kleene's last element should only have one child\00"))
    (call @i8$ptr.unsafe-print (args "rule: \00"))
    (call @Texp$ptr.parenPrint (args %rule))
    (call @println args)
    (call @i8$ptr.unsafe-print (args "last: \00"))
    (call @Texp$ptr.parenPrint (args %last))
    (call @println args)
    (call @exit (args 1))
  ))
  (let %kleene-prod-view (call @String$ptr.view (args (index %last 0))))

; (grammar (* (production (rule))))

  (let %rule-length (load (index %rule 2)))
  (let %texp-length (load (index %texp 2)))

  (if (< %texp-length (- %rule-length 1)) (do
    (auto %failure-result %struct.Texp)
    (store (call @Texp.makeFromi8$ptr (args "error\00")) %failure-result)

    (call @Texp$ptr.push (args %failure-result 
      (call @Texp.makeFromi8$ptr (args "texp length not less than for rule.len - 1\00"))))
    (call @Texp$ptr.push (args %failure-result (call @Texp$ptr.clone (args %rule))))
    (call @Texp$ptr.push (args %failure-result (call @Texp$ptr.clone (args %texp))))
    (return (load %failure-result))
  ))

  (auto %proof %struct.Texp)
  (store (call @Texp.makeFromi8$ptr (args "kleene\00")) %proof)

  (if (!= 1 %rule-length) (do
    (call @Matcher$ptr.kleene-seq (args %this %texp %prod %proof 0 (- %rule-length 2)))
    (if (call @Texp$ptr.value-check (args %proof "error\00")) (do
      (return (load %proof))
    ))
  ))

;                                                                                  |
; TODO                                                                 why is this v not (- %texp-length 1)
  (call @Matcher$ptr.kleene-many (args %this %texp %prod %proof (- %rule-length 1) (- %texp-length 1)))
  (if (call @Texp$ptr.value-check (args %proof "error\00")) (do
    (return (load %proof))
  ))

; debug
  (call @i8$ptr.unsafe-print (args " [.kleene       ]  success: \00"))
  (call @Texp$ptr.parenPrint (args %proof))
  (call @println args)

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
; TODO assert (rule.size == texp.size)

; debug
  (call @i8$ptr.unsafe-println (args " [.exact        ]\00"))

  (auto %proof %struct.Texp)
  (store (call @Texp.makeFromi8$ptr (args "exact\00")) %proof)

; TODO why does exact_ get the value?

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
; TODO assert curr.rule length == 0

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

  (if (== %i (- (load (index %prod 2)) 1)) (do
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
  (call @i8$ptr.unsafe-print (args " [.choice       ]  \00"))
  (call @Texp$ptr.parenPrint (args %texp))
  (call @println args)

  (auto %proof %struct.Texp)
  (store (call @Texp.makeFromi8$ptr (args "choice\00")) %proof)

  (auto %attempts %struct.Texp)
  (store (call @Texp.makeFromi8$ptr (args "choice-attempts\00")) %attempts)

  (return (call @Matcher$ptr.choice_ (args %this %texp %prod 0 %attempts)))
))

; TODO regexInt
(def @Matcher.regexInt (params (%texp %struct.Texp*)) i1 (do
  (return true)
))



(def @Matcher$ptr.value (params (%this %struct.Matcher*) (%texp %struct.Texp*) (%prod %struct.Texp*)) %struct.Texp (do
; debug
  (call @i8$ptr.unsafe-print (args " [.value        ]  \00"))
  (call @Texp$ptr.parenPrint (args %texp))

  (let %rule (load (index %prod 1)))

; debug
  (call @i8$ptr.unsafe-print (args " -> \00"))
  (call @Texp$ptr.parenPrint (args %prod))
  (call @println args)

  (let %texp-value-view-ref (call @Texp$ptr.value-view (args %texp)))
  (let %rule-value-view-ref (call @Texp$ptr.value-view (args %rule)))

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
      (store (call @Result.error-from-i8$ptr (args "failed to match #int\00")) %error-result)
    ))

    (if (call @Texp$ptr.value-check (args %rule "#string\00")) (do
;                   TODO regexString    
      (if (call @Matcher.regexInt (args %texp)) (do
        (return %default-success)
      ))
      (store (call @Result.error-from-i8$ptr (args "failed to match #string\00")) %error-result)
    ))

    (if (call @Texp$ptr.value-check (args %rule "#bool\00")) (do
      (if (call @Matcher.regexInt (args %texp)) (do
        (return %default-success)
      ))
      (store (call @Result.error-from-i8$ptr (args "failed to match #bool\00")) %error-result)
    ))

    (if (call @Texp$ptr.value-check (args %rule "#type\00")) (do
;     (if (call @Matcher.regexInt (args %texp)) (do
        (return %default-success)
;     ))
;     (store (call @Result.error-from-i8$ptr (args "failed to match #type\00")) %error-result)
    ))

    (if (call @Texp$ptr.value-check (args %rule "#name\00")) (do

; debug
;     (call @i8$ptr.unsafe-println (args " [.value        ]  in #name\00"))

;     (if (call @Matcher.regexInt (args %texp)) (do
        (return %default-success)
;     ))
;     (store (call @Result.error-from-i8$ptr (args "failed to match #name\00")) %error-result)
    ))

;   ; check for errors
    (if (call @Texp$ptr.value-check (args %error-result "error")) (do
      (call @Texp$ptr.push$ptr (args %error-result %texp))
      (return (load %error-result))
    ))

;   ; error out, "unmatched regex check for rule value: '" + rule.value + "'"
    (call-vargs @printf (args "unmatched regex check for rule value: '\00"))
    (call @StringView$ptr.print (args %rule-value-view-ref))
    (call @puts (args "'\00"))
    (call @exit (args 1))
  ))

; else branch, if rule.value doesn't start with '#'
; (if (- 1 %cond) (do
;   return texp.value == rule.value ? texp("success", rule.value)
;                                   : texp("error", "keyword-match", texp.value, rule.value);
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

(def @test.matcher-self params void (do
  (auto %filename %struct.StringView)
	(call @StringView$ptr.set (args %filename "lib2/core.bb\00"))

  (auto %prog %struct.Texp)
  (store (call @Parser.parse-file (args %filename)) %prog)

  (auto %matcher %struct.Matcher)
  (store (call @Grammar.make (args (call @Parser.parse-file-i8$ptr (args "docs/bb-type-tall-str-include-grammar.texp\00")))) (index %matcher 0))

  (auto %start-production %struct.StringView)
  (store (call @StringView.makeFromi8$ptr (args "Program\00")) %start-production)

  (auto %result %struct.Texp)
  (store (call @Matcher$ptr.is (args %matcher %prog %start-production)) %result)

  (return-void)
))