(include "cornerstone/lib/core.bb")

(def @fibonacci (params (%n u64)) u64 (do
    (if (< %n 2) (do (return 1)))
    (return (+ (call @fibonacci (args (- %n 1))) 
               (call @fibonacci (args (- %n 2)))
    ))

))

(def @main params i32 (do
    (call @i8$ptr.unsafe-println (args "Hello World!\00"))
    (call @u64.println (args (call @fibonacci (args 50))))
    (return 0)
))
