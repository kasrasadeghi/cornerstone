;========== Grammar ===============================================================================

(struct %struct.Grammar
  (%texp %struct.Texp))

(def @Grammar.make (params (%texp %struct.Texp)) %struct.Grammar (do
  (auto %grammar %struct.Grammar)
  (store %texp (index %grammar 0))
  (call @Texp$ptr.demote-free (args (index %grammar 0)))
  (return (load %grammar))
))

(def @Grammar$ptr.getProduction (params (%this %struct.Grammar*) (%type-name %struct.StringView*)) %struct.Texp* (do
  (let %maybe-prod (call @Texp$ptr.find (args (index %this 0) %type-name)))
  (if (== 0 (cast u64 %maybe-prod)) (do
    (call @i8$ptr.unsafe-print (args "\0Aproduction \00"))
    (call @StringView$ptr.print (args %type-name))
    (call @i8$ptr.unsafe-println (args " not found\00"))
;    (call @Texp$ptr.pretty-print (args (index %this 0)))
;    (call @free (args (cast i8* (0 u64))))
    (call @exit (args 1))
  ))

  (return %maybe-prod)
))

; returns optional
; TODO consider renaming to-keyword
; TODO consider returning optional stringview, waaay later, requires parametric types
; TODO is it true that all keywords come from productions that simply (or (are not choices) (do not start with #))?
(def @Grammar$ptr.get-keyword (params (%this %struct.Grammar*) (%type-name %struct.StringView*)) %struct.Texp (do
  (let %prod (call @Grammar$ptr.getProduction (args %this %type-name)))
  (let %rule (load (index %prod 1)))
  (if (call @Texp$ptr.value-check (args %rule "|\00")) (do
    (return (call @Optional.none args))
  ))

; TODO make into assert
  (let %rule-value (index %rule 0))
  (if (call @String$ptr.is-empty (args %rule-value)) (do
    (call @i8$ptr.unsafe-println (args "rule value should not be empty for rule:\00"))
    (call @Texp$ptr.parenPrint (args %rule))
    (call @exit (args 1))
  ))

  (let %HASH (+ 35 (0 i8)))
  (if (== %HASH (call @String$ptr.char-at-unsafe (args %rule-value 0))) (do
    (return (call @Optional.none args))
  ))

  (return (call @Texp.makeFromString (args %rule-value)))
))
