;========== Matcher ===============================================================================

(struct %struct.Matcher
  (%grammar %struct.Grammar))

(def @Matcher$ptr.is (params (%this %struct.Matcher*) (%texp %struct.Texp*) (%type-name %struct.StringView*)) %struct.Texp (do
  (let %grammar (index %this 0))
  (let %rule (call @Grammar$ptr.getProduction (args %grammar %type-name)))
  (auto %result %struct.Texp)
  (store (call @Matcher$ptr.match (args %this %texp %rule)) %result)
  (let %result-value (load (call @Texp$ptr.value-view (args %result))))
  (if (call @StringView.eq (args %result-value (call @StringView.makeFromi8$ptr (args "success\00")))) (do
    (let %proof-value (index (index (index %this 0) 0) 0))
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

(def @Matcher$ptr.match (params (%this %struct.Matcher*) (%texp %struct.Texp*) (%rule %struct.Texp*)) %struct.Texp (do
  (if (call @Texp$ptr.value-check (args %texp "|\00")) (do
    (return (call @Matcher$ptr.choice (args %this %texp %rule)))
  ))

  (let %value-result (call @Matcher$ptr.value (args %this %texp %rule)))
; check for error
  (if (call @Texp$ptr.value-check (args %texp "error\00")) (do
    (return %value-result)
  ))

  (let %length (load (index %rule 2)))
  (let %last (call @Texp$ptr.last (args %rule)))
  (if (* (!= %length 0) (call @Texp$ptr.value-check (args %rule "*\00"))) (do
    (return (call @Matcher$ptr.kleene (args %this %texp %rule)))
  ))

  (return (call @Matcher$ptr.exact (args %this %texp %rule)))
))

(def @Matcher$ptr.kleene-many (params
      (%this %struct.Matcher*) (%texp %struct.Texp*) (%rule %struct.Texp*) (%acc %struct.Texp*)
      (%curr-index u64) (%last-index u64))
    void (do
  (let %texp-child (call @Texp$ptr.child (args %texp %curr-index)))
  (let %rule-child (call @Texp$ptr.last (args %rule)))

  (auto %result %struct.Texp)
  (store (call @Matcher$ptr.is (args %this %texp-child (call @Texp$ptr.value-view (args %rule-child)))) %result)

; if the check is not successful, clear the accumulator and set the error code.
; CONSIDER see kleene-many
  (if (call @Texp$ptr.value-check (args %result "error\00")) (do
    (call @Texp$ptr.free (args %acc))
    (store (load %result) %acc)
    (return-void)
  ))

  (call @Texp$ptr.push (args %acc (load %result)))
  (if (== %curr-index %last-index) (do (return-void)))
  (let %next-index (+ 1 %curr-index))
  (call @Matcher$ptr.kleene-seq (args %this %texp %rule %acc %next-index %last-index))
  (return-void)
))

(def @Matcher$ptr.kleene-seq (params
      (%this %struct.Matcher*) (%texp %struct.Texp*) (%rule %struct.Texp*) (%acc %struct.Texp*)
      (%curr-index u64) (%last-index u64))
    void (do
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

  (call @Texp$ptr.push (args %acc (load %result)))
  (if (== %curr-index %last-index) (do (return-void)))
  (let %next-index (+ 1 %curr-index))
  (call @Matcher$ptr.kleene-seq (args %this %texp %rule %acc %next-index %last-index))
  (return-void)
))

(def @Matcher$ptr.kleene (params (%this %struct.Matcher*) (%texp %struct.Texp*) (%rule %struct.Texp*)) %struct.Texp (do
  (let %last (call @Texp$ptr.last (args %rule)))
  (let %last-length (load (index %last 2)))
  (if (!= 1 %last-length) (do
    (call @puts (args "error\00"))
    (call @exit (args 1))
  ))
  (let %kleene-prod-view (call @String$ptr.view (args (index %last 0))))

  (if (< (load (index %texp 2)) (- (load (index %rule 2)) 1)) (do
    (auto %failure-result %struct.Texp)
    (store (call @Texp.makeFromi8$ptr (args "failure\00")) %failure-result)

    (call @Texp$ptr.push (args %failure-result 
      (call @Texp.makeFromi8$ptr (args "texp length not less than for rule.len - 1\00"))))
    (call @Texp$ptr.push (args %failure-result (call @Texp$ptr.clone (args %rule))))
    (call @Texp$ptr.push (args %failure-result (call @Texp$ptr.clone (args %texp))))
    (return (load %failure-result))
  ))

  (auto %proof %struct.Texp)
  (store (call @Texp.makeFromi8$ptr (args "kleene\00")) %proof)

  (call @Matcher$ptr.kleene-seq (args %this %texp %rule %proof 0 (- (load (index %rule 2)) 1)))
  (if (call @Texp$ptr.value-check (args %proof "error\00")) (do
    (return (load %proof))
  ))

  (call @Matcher$ptr.kleene-many (args %this %texp %rule %proof (load (index %rule 2)) (- (load (index %texp 2)) 1)))
  (if (call @Texp$ptr.value-check (args %proof "error\00")) (do
    (return (load %proof))
  ))

  (auto %proof-success-wrapper %struct.Texp)
  (store (call @Texp.makeFromi8$ptr (args "success\00")) %proof-success-wrapper)
  (call @Texp$ptr.push$ptr (args %proof-success-wrapper %proof))
  (return (load %proof-success-wrapper))
))

(def @Matcher$ptr.exact_ (params
      (%this %struct.Matcher*) (%texp %struct.Texp*) (%rule %struct.Texp*) (%acc %struct.Texp*)
      (%curr-index u64) (%last-index u64))
    void (do
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

  (call @Texp$ptr.push (args %acc (load %result)))
  (if (== %curr-index %last-index) (do (return-void)))
  (let %next-index (+ 1 %curr-index))
  (call @Matcher$ptr.kleene-seq (args %this %texp %rule %acc %next-index %last-index))
  (return-void)
))

(def @Matcher$ptr.exact (params (%this %struct.Matcher*) (%texp %struct.Texp*) (%rule %struct.Texp*)) %struct.Texp (do
; TODO assert (rule.size == texp.size)

  (auto %proof %struct.Texp)
  (store (call @Texp.makeFromi8$ptr (args "exact\00")) %proof)

  (let %last (- (load (index %texp 2)) 1))
  (call @Matcher$ptr.exact_ (args %this %texp %rule %proof 0 %last))

  (auto %proof-success-wrapper %struct.Texp)
  (store (call @Texp.makeFromi8$ptr (args "success\00")) %proof-success-wrapper)
  (call @Texp$ptr.push$ptr (args %proof-success-wrapper %proof))
  (return (load %proof-success-wrapper))
))

(def @Matcher$ptr.choice (params (%this %struct.Matcher*) (%texp %struct.Texp*) (%rule %struct.Texp*)) %struct.Texp (do
  (auto %proof %struct.Texp)
  (store (call @Texp.makeFromi8$ptr (args "exact\00")) %proof)
; LATER
  (return (load %proof))
))

(def @Matcher.regexInt (params (%texp %struct.Texp*)) i1 (do
  (return true)
))

(def @Matcher$ptr.value (params (%this %struct.Matcher*) (%texp %struct.Texp*) (%rule %struct.Texp*)) %struct.Texp (do

  (let %texp-value-view-ref (call @Texp$ptr.value-view (args %rule)))
  (let %rule-value-view-ref (call @Texp$ptr.value-view (args %rule)))

  (auto %rule-value-texp %struct.Texp)
  (store (call @Texp.makeFromStringView (args %rule-value-view-ref)) %rule-value-texp)
  (auto %texp-value-texp %struct.Texp)
  (store (call @Texp.makeFromStringView (args %texp-value-view-ref)) %texp-value-texp)
  
  (let %default-success (call @Result.success (args %rule-value-texp)))

; hash character, '#'
  (let %HASH (+ 35 (0 i8)))
  (let %value-first-char (call @Texp$ptr.value-get (args %texp 0)))
  (let %cond (== %value-first-char %HASH))

  (auto %error-result %struct.Texp)
  (store (call @Texp.makeEmpty args) %error-result)

  (if %cond (do

; SOON Texp.make-from-value? texp.value
    (if (call @Texp$ptr.value-check (args %rule "#int\00")) (do
      (if (call @Matcher.regexInt (args %texp)) (do
        (return %default-success)
      ))
      (store (call @Result.error (args "failed to match #int\00")) %error-result)
    ))

    (if (call @Texp$ptr.value-check (args %rule "#string\00")) (do
      (if (call @Matcher.regexInt (args %texp)) (do
        (return %default-success)
      ))
      (store (call @Result.error (args "failed to match #string\00")) %error-result)
    ))

    (if (call @Texp$ptr.value-check (args %rule "#bool\00")) (do
      (if (call @Matcher.regexInt (args %texp)) (do
        (return %default-success)
      ))
      (store (call @Result.error (args "failed to match #bool\00")) %error-result)
    ))

    (if (call @Texp$ptr.value-check (args %rule "#type\00")) (do
;     (if (call @Matcher.regexInt (args %texp)) (do
        (return %default-success)
;     ))
;     (store (call @Result.error (args "failed to match #type\00")) %error-result)
    ))

    (if (call @Texp$ptr.value-check (args %rule "#name\00")) (do
;     (if (call @Matcher.regexInt (args %texp)) (do
        (return %default-success)
;     ))
;     (store (call @Result.error (args "failed to match #name\00")) %error-result)
    ))

;   ; check for errors
    (if (call @Texp$ptr.value-check (args %error-result "error")) (do
      ()
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
  (call @Texp$ptr.push$ptr (args %keyword-match-error %rule-value-texp))
  (call @Texp$ptr.push$ptr (args %keyword-match-error %texp-value-texp))
  (return (load %keyword-match-error))
; ))
; end else branch
))

;========== Matcher tests ==========================================================================

(def @test.matcher-simple params void (do
  (auto %filename %struct.StringView)
	(call @StringView$ptr.set (args %filename "lib2/core.bb\00"))
  
  (auto %file %struct.File)
  (store (call @File.openrw (args %filename)) %file)

  (auto %content %struct.StringView)
  (store (call @File$ptr.readwrite (args %file)) %content)

  (auto %parser %struct.Parser)
  (call @Reader$ptr.set (args (index %parser 0) %content))

  (call @Parser$ptr.remove-comments (args %parser 0))

  (auto %prog %struct.Texp) 
  (auto %filename-string %struct.String)
  (store (call @String.makeFromStringView (args %filename)) %filename-string)
  (call @Texp$ptr.setFromString (args %prog %filename-string))

  (call @Parser$ptr.collect (args %parser %prog))

  (call @Texp$ptr.parenPrint (args %prog))

  (call @File.unread (args %content))
  (call @File$ptr.close (args %file))
  (return-void)
))