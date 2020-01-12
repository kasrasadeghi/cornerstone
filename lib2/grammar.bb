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
