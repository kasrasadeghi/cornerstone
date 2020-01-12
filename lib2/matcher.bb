;========== Matcher ===============================================================================

(struct %struct.Matcher
  (%grammar %struct.Grammar))

(def @Matcher$ptr.is (params (%this %struct.Matcher*) (%texp %struct.Texp*) (%type-name %struct.StringView*)) %struct.Texp (do
; debug
  (call @i8$ptr.unsafe-print (args " [Matcher.is    ]  texp: \00"))
  (call @Texp$ptr.parenPrint (args %texp))
  (call @println args)
  (call @i8$ptr.unsafe-print (args " [Matcher.is    ]  type: \00"))
  (call @StringView$ptr.print (args %type-name))

  (let %grammar (index %this 0))
  (let %rule (call @Grammar$ptr.getProduction (args %grammar %type-name)))

; debug
  (call @i8$ptr.unsafe-print (args ", rule: \00"))
  (call @Texp$ptr.parenPrint (args %rule))
  (call @println args)

  (auto %result %struct.Texp)
  (store (call @Matcher$ptr.match (args %this %texp %rule)) %result)

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

(def @Matcher$ptr.atom (params (%this %struct.Matcher*) (%texp %struct.Texp*) (%rule %struct.Texp*)) %struct.Texp (do
  (call @i8$ptr.unsafe-println (args " [Matcher.atom  ]  start\00"))

  (if (!= 0 (load (index %texp 2))) (do
    (auto %error-result %struct.Texp)
;   hex '22' is a quote
    (store (call @Result.error-from-i8$ptr (args "\22texp is not an atom\22\00")) %error-result)
    (call @Texp$ptr.push (args %error-result (call @Texp$ptr.clone (args %rule))))
    (return (load %error-result))
  ))
  (return (call @Result.success-from-i8$ptr (args "atom\00")))
))

(def @Matcher$ptr.match (params (%this %struct.Matcher*) (%texp %struct.Texp*) (%rule %struct.Texp*)) %struct.Texp (do
  (if (call @Texp$ptr.value-check (args %texp "|\00")) (do
    (return (call @Matcher$ptr.choice (args %this %texp %rule)))
  ))

  (let %value-result (call @Matcher$ptr.value (args %this %texp %rule)))
; check for error
  (if (call @Texp$ptr.value-check (args %texp "error\00")) (do
    (return %value-result)
  ))

; TODO use value-result later

; (grammar (* (rule (production)))) <- the only child of a rule is a production

  (let %production (load (index %rule 1)))
  (let %rule-length (load (index %production 2)))

; debug
  (call @i8$ptr.unsafe-print (args " [Matcher.match ]  length: \00"))
  (call @u64.println (args %rule-length))
  (call @i8$ptr.unsafe-print (args " [Matcher.match ]  rule: \00"))
  (call @Texp$ptr.parenPrint (args %rule))
  (call @println args)
  (call @i8$ptr.unsafe-print (args " [Matcher.match ]  texp: \00"))
  (call @Texp$ptr.parenPrint (args %texp))
  (call @println args)

  (if (!= %rule-length 0) (do
    (let %last (call @Texp$ptr.last (args %production)))
    (if (call @Texp$ptr.value-check (args %last "*\00")) (do
      (return (call @Matcher$ptr.kleene (args %this %texp %rule)))
    ))
    (return (call @Matcher$ptr.exact (args %this %texp %rule)))
  ))

  (return (call @Matcher$ptr.atom (args %this %texp %rule)))
))

(def @Matcher$ptr.kleene-many (params
      (%this %struct.Matcher*) (%texp %struct.Texp*) (%rule %struct.Texp*) (%acc %struct.Texp*)
      (%curr-index u64) (%last-index u64))
    void (do

; debug
  (call @i8$ptr.unsafe-println (args " [Matcher.kleene]  -many start\00"))

  (let %production (load (index %rule 1)))
  (let %last (call @Texp$ptr.last (args %production)))

  (let %texp-child (call @Texp$ptr.child (args %texp %curr-index)))
  (let %rule-child (call @Texp$ptr.last (args (load (index %production 1)))))

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
  (call @Matcher$ptr.kleene-many (args %this %texp %rule %acc %next-index %last-index))
  (return-void)
))

(def @Matcher$ptr.kleene-seq (params
      (%this %struct.Matcher*) (%texp %struct.Texp*) (%rule %struct.Texp*) (%acc %struct.Texp*)
      (%curr-index u64) (%last-index u64))
    void (do

; debug
  (call @i8$ptr.unsafe-println (args " [Matcher.kleene]  -seq start\00"))

  (let %production (load (index %rule 1)))
  (let %last (call @Texp$ptr.last (args %production)))

  (let %texp-child (call @Texp$ptr.child (args %texp %curr-index)))
  (let %prod-child (call @Texp$ptr.child (args %production %curr-index)))

  (auto %result %struct.Texp)
  (store (call @Matcher$ptr.is (args %this %texp-child (call @Texp$ptr.value-view (args %prod-child)))) %result)

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
  (call @Matcher$ptr.kleene-seq (args %this %texp %rule %acc %next-index %last-index))
  (return-void)
))

(def @Matcher$ptr.kleene (params (%this %struct.Matcher*) (%texp %struct.Texp*) (%rule %struct.Texp*)) %struct.Texp (do

; debug
  (call @i8$ptr.unsafe-println (args " [Matcher.kleene]  start\00"))

  (let %production (load (index %rule 1)))
  (let %last (call @Texp$ptr.last (args %production)))
  (let %last-length (load (index %last 2)))
  (if (!= 1 %last-length) (do
    (call @puts (args "error\00"))
    (call @exit (args 1))
  ))
  (let %kleene-prod-view (call @String$ptr.view (args (index %last 0))))

; (grammar (* (rule (production))))

  (let %production-length (load (index %production 2)))
  (let %texp-length (load (index %texp 2)))

  (if (< %texp-length (- %production-length 1)) (do
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

  (if (!= 1 %production-length) (do
    (call @Matcher$ptr.kleene-seq (args %this %texp %rule %proof 0 (- %production-length 1)))
    (if (call @Texp$ptr.value-check (args %proof "error\00")) (do
      (return (load %proof))
    ))
  ))

  (call @Matcher$ptr.kleene-many (args %this %texp %rule %proof %production-length (- %texp-length 1)))
  (if (call @Texp$ptr.value-check (args %proof "error\00")) (do
    (return (load %proof))
  ))

; debug
  (call @i8$ptr.unsafe-print (args " [Matcher.kleene]  success: \00"))
  (call @Texp$ptr.parenPrint (args %proof))
  (call @println args)

  (return (call @Result.success (args %proof)))
))

(def @Matcher$ptr.exact_ (params
      (%this %struct.Matcher*) (%texp %struct.Texp*) (%rule %struct.Texp*) (%acc %struct.Texp*)
      (%curr-index u64) (%last-index u64))
    void (do
  (let %prod (load (index %rule 1)))
  (let %texp-child (call @Texp$ptr.child (args %texp %curr-index)))
  (let %prod-child (call @Texp$ptr.child (args %prod %curr-index)))

; debug
  (call @i8$ptr.unsafe-print (args " [Matcher.exact_]  matching against \00"))
  (call @Texp$ptr.parenPrint (args %prod-child))
  (call @println args)

  (auto %result %struct.Texp)
  (store (call @Matcher$ptr.is (args %this %texp-child (call @Texp$ptr.value-view (args %prod-child)))) %result)

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
  (call @Matcher$ptr.exact_ (args %this %texp %rule %acc %next-index %last-index))
  (return-void)
))

(def @Matcher$ptr.exact (params (%this %struct.Matcher*) (%texp %struct.Texp*) (%rule %struct.Texp*)) %struct.Texp (do
; TODO assert (rule.size == texp.size)

  (auto %proof %struct.Texp)
  (store (call @Texp.makeFromi8$ptr (args "exact\00")) %proof)

; TODO why does exact_ get the value?

  (let %last (- (load (index %texp 2)) 1))
  (call @Matcher$ptr.exact_ (args %this %texp %rule %proof 0 %last))

  (auto %proof-success-wrapper %struct.Texp)
  (store (call @Texp.makeFromi8$ptr (args "success\00")) %proof-success-wrapper)
  (call @Texp$ptr.push$ptr (args %proof-success-wrapper %proof))
  (return (load %proof-success-wrapper))
))

(def @Matcher$ptr.choice_ (params
         (%this %struct.Matcher*) (%texp %struct.Texp*) (%rule %struct.Texp*)
         (%i u64) (%acc %struct.Texp*)) %struct.Texp (do
  (let %curr-rule (call @Texp$ptr.child (args %rule %i)))
; TODO assert curr.rule length == 0

  (let %curr-rule-view (call @Texp$ptr.value-view (args %curr-rule)))

  (auto %result %struct.Texp)
  (store (call @Matcher$ptr.is (args %this %texp %curr-rule-view)) %result)

  (if (call @Texp$ptr.value-check (args %result "success\00")) (do
    
  ))
  (return (load %result))
))

(def @Matcher$ptr.choice (params (%this %struct.Matcher*) (%texp %struct.Texp*) (%rule %struct.Texp*)) %struct.Texp (do
  (auto %proof %struct.Texp)
  (store (call @Texp.makeFromi8$ptr (args "choice\00")) %proof)

  (auto %attempts %struct.Texp)
  (store (call @Texp.makeFromi8$ptr (args "choice-attempts\00")) %proof)

  (let %rule-first (load (index %rule 1)))
  (let %rule-last (call @Texp$ptr.last (args %rule)))
  (call @Matcher$ptr.choice_ (args %this %texp %rule 0 %attempts))

  (auto %result-error %struct.Texp)
  (store (call @Result.error-from-i8$ptr (args "choice-match\00")) %result-error)

; SOON
  (return (load %result-error))
))

; TODO regexInt
(def @Matcher.regexInt (params (%texp %struct.Texp*)) i1 (do
  (return true)
))

(def @Matcher$ptr.value (params (%this %struct.Matcher*) (%texp %struct.Texp*) (%rule %struct.Texp*)) %struct.Texp (do
; debug
  (call @i8$ptr.unsafe-print (args " [Matcher.value ]  texp: \00"))
  (call @Texp$ptr.parenPrint (args %texp))
  (call @println args)
  (call @i8$ptr.unsafe-print (args " [Matcher.value ]  rule: \00"))
  (call @Texp$ptr.parenPrint (args %rule))
  (call @println args)

; TODO swap 'production' and 'rule' as variable names
  (let %prod (load (index %rule 1)))

  (let %texp-value-view-ref (call @Texp$ptr.value-view (args %rule)))
  (let %prod-value-view-ref (call @Texp$ptr.value-view (args %prod)))

  (auto %prod-value-texp %struct.Texp)
  (store (call @Texp.makeFromStringView (args %prod-value-view-ref)) %prod-value-texp)
  (auto %texp-value-texp %struct.Texp)
  (store (call @Texp.makeFromStringView (args %texp-value-view-ref)) %texp-value-texp)

; debug
; (call @i8$ptr.unsafe-println (args " [Matcher.value ]  before making default-success\00"))
; (call @i8$ptr.unsafe-print (args " [Matcher.value ]  rule-value: \00"))
; (call @Texp$ptr.shallow-dump (args %prod-value-texp))
; (call @println args)

  (let %default-success (call @Result.success (args %prod-value-texp)))

; debug
; (call @i8$ptr.unsafe-println (args " [Matcher.value ]  finished making default-success\00"))

; hash character, '#'
  (let %HASH (+ 35 (0 i8)))
  (let %value-first-char (call @Texp$ptr.value-get (args %prod 0)))
  (let %cond (== %value-first-char %HASH))

  (auto %error-result %struct.Texp)
  (store (call @Texp.makeEmpty args) %error-result)

  (if %cond (do

; debug
;   (call @i8$ptr.unsafe-println (args " [Matcher.value ]  in condition\00"))

    (if (call @Texp$ptr.value-check (args %prod "#int\00")) (do
      (if (call @Matcher.regexInt (args %texp)) (do
        (return %default-success)
      ))
      (store (call @Result.error-from-i8$ptr (args "failed to match #int\00")) %error-result)
    ))

    (if (call @Texp$ptr.value-check (args %prod "#string\00")) (do
; TODO regexString    
      (if (call @Matcher.regexInt (args %texp)) (do
        (return %default-success)
      ))
      (store (call @Result.error-from-i8$ptr (args "failed to match #string\00")) %error-result)
    ))

    (if (call @Texp$ptr.value-check (args %prod "#bool\00")) (do
      (if (call @Matcher.regexInt (args %texp)) (do
        (return %default-success)
      ))
      (store (call @Result.error-from-i8$ptr (args "failed to match #bool\00")) %error-result)
    ))

    (if (call @Texp$ptr.value-check (args %prod "#type\00")) (do
;     (if (call @Matcher.regexInt (args %texp)) (do
        (return %default-success)
;     ))
;     (store (call @Result.error-from-i8$ptr (args "failed to match #type\00")) %error-result)
    ))

    (if (call @Texp$ptr.value-check (args %prod "#name\00")) (do

; debug
;     (call @i8$ptr.unsafe-println (args " [Matcher.value ]  in #name\00"))

;     (if (call @Matcher.regexInt (args %texp)) (do
        (return %default-success)
;     ))
;     (store (call @Result.error-from-i8$ptr (args "failed to match #name\00")) %error-result)
    ))

;   ; check for errors
    (if (call @Texp$ptr.value-check (args %error-result "error")) (do
      (call @Texp$ptr.push$ptr (args %error-result %texp))
      (call @exit (args 1))
    ))

;   ; error out, "unmatched regex check for rule value: '" + rule.value + "'"
    (call-vargs @printf (args "unmatched regex check for rule value: '\00"))
    (call @StringView$ptr.print (args %prod-value-view-ref))
    (call @puts (args "'\00"))
    (call @exit (args 1))
  ))

; else branch, if rule.value doesn't start with '#'
; (if (- 1 %cond) (do
;   return texp.value == rule.value ? texp("success", rule.value)
;                                   : texp("error", "keyword-match", texp.value, rule.value);
  (if (call @StringView$ptr.eq (args %prod-value-view-ref %texp-value-view-ref)) (do
    (return %default-success)
  ))

  (auto %keyword-match-error %struct.Texp)
  (store (call @Texp.makeFromi8$ptr (args "error\00")) %keyword-match-error)
  (call @Texp$ptr.push (args %keyword-match-error (call @Texp.makeFromi8$ptr (args "keyword-match\00"))))
  (call @Texp$ptr.push (args %keyword-match-error (call @Texp$ptr.clone (args %prod-value-texp))))
  (call @Texp$ptr.push (args %keyword-match-error (call @Texp$ptr.clone (args %texp-value-texp))))
  (return (load %keyword-match-error))
; ))
; end else branch
))

;========== Matcher tests ==========================================================================

(def @test.matcher-simple params void (do

  (auto %prog %struct.Texp)
  (store (call @Parser.parse-file-i8$ptr (args "/home/kasra/projects/backbone-test/matcher/exact.texp\00")) %prog)

  (auto %matcher %struct.Matcher)
  (store (call @Grammar.make (args (call @Parser.parse-file-i8$ptr (args "/home/kasra/projects/backbone-test/matcher/exact.grammar\00")))) (index %matcher 0))

  (auto %start-production %struct.StringView)
  (store (call @StringView.makeFromi8$ptr (args "Program\00")) %start-production)

;  (call @Texp$ptr.pretty-print (args (cast %struct.Texp* %matcher)))

  (call @Matcher$ptr.is (args %matcher %prog %start-production))

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

;  (call @Texp$ptr.pretty-print (args %prog))

  (auto %matcher %struct.Matcher)
  (store (call @Grammar.make (args (call @Parser.parse-file-i8$ptr (args "docs/bb-type-tall-str-include-grammar.texp\00")))) (index %matcher 0))

  (auto %start-production %struct.StringView)
  (store (call @StringView.makeFromi8$ptr (args "Program\00")) %start-production)

;  (call @Texp$ptr.pretty-print (args (cast %struct.Texp* %matcher)))

  (auto %result %struct.Texp)
  (store (call @Matcher$ptr.is (args %matcher %prog %start-production)) %result)

  (return-void)
))