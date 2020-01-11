(def @Result.success (params (%texp %struct.Texp*)) %struct.Texp (do
  (auto %result %struct.Texp)
  (store (call @Texp.makeFromi8$ptr (args "success\00")) %result)
  (call @Texp$ptr.push (args %result (call @Texp$ptr.clone (args %texp))))
  (return (load %result))
))

(def @Result.error (params (%texp %struct.Texp*)) %struct.Texp (do
  (auto %result %struct.Texp)
  (store (call @Texp.makeFromi8$ptr (args "error\00")) %result)
  (call @Texp$ptr.push (args %result (call @Texp$ptr.clone (args %texp))))
  (return (load %result))
))

(def @Result.error-from-view (params (%view %struct.StringView*)) %struct.Texp (do
  (auto %result %struct.Texp)
  (store (call @Texp.makeFromi8$ptr (args "error\00")) %result)
  (call @Texp$ptr.push (args %result (call @Texp.makeFromStringView (args %view))))
  (return (load %result))
))

(def @Result.error-from-i8$ptr (params (%cstr i8*)) %struct.Texp (do
  (auto %result %struct.Texp)
  (store (call @Texp.makeFromi8$ptr (args "error\00")) %result)
  (call @Texp$ptr.push (args %result (call @Texp.makeFromi8$ptr (args %cstr))))
  (return (load %result))
))