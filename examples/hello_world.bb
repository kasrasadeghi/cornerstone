(include "../lib/core.bb")

(def @main params i32 (do
  (call @i8$ptr.unsafe-println (args "hello world\00"))
  (return 0)
))