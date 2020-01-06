;========== Grammar ===============================================================================

(struct %struct.Grammar
  (%texp %struct.Texp))

(def @Grammar.make (params (%texp %struct.Texp)) %struct.Grammar (do
  (auto %grammar %struct.Grammar)
  (store %texp (index %grammar 0))
  (return (load %grammar))
))

(def @Grammar$ptr.getProduction (params (%this %struct.Grammar*) (%type-name %struct.StringView*)) %struct.Texp* (do
  (let %maybe-prod (call @Texp$ptr.find (args (index %this 0) %type-name)))
  (if (== 0 (cast u64 %maybe-prod)) (do
    (auto %msg %struct.StringView)
    (store (call @StringView.makeFromi8$ptr (args "production not found\0A\00")) %msg)
    (call @StringView$ptr.print (args %msg))
    (call @exit (args 1))
  ))

  (return %maybe-prod)
))
