(def @Optional.some (params (%texp %struct.Texp*)) %struct.Texp (do
  (auto %result %struct.Texp)
  (store (call @Texp.makeFromi8$ptr (args "some\00")) %result)
  (call @Texp$ptr.push (args %result (call @Texp$ptr.clone (args %texp))))
  (return (load %result))
))

(def @Optional.none params %struct.Texp (do
  (return (call @Texp.makeFromi8$ptr (args "none\00")))
))