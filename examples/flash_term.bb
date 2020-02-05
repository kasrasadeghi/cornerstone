(include "../lib/core.bb")

(decl @usleep (types i32) i32)
(decl @atoi (types i8*) i32)

(def @flash (params (%i u32)) void (do
    (if (== %i 0) (do
        (return-void)
    ))

    (if (== 0 (% %i 2)) (do
;       Reverse video
        (call @i8$ptr.unsafe-print (args "\1b[?5h\00"))
    ))
    
    (if (== 1 (% %i 2)) (do
;       Disable reverse video
        (call @i8$ptr.unsafe-print (args "\1b[?5l\00"))
    ))

    (call @usleep (args 100000))
    (call-tail @flash (args (- %i 1)))
    (return-void)
))

(def @main (params (%argc i32) (%argv i8**)) i32 (do
    (if (!= %argc 2) (do
        (call @i8$ptr.unsafe-println (args "usage: ./flash <count>\00"))
        (return 1)
    ))

    (let %count-str (load (cast i8** (+ 8 (cast u64 %argv)))))
    (let %count (call @atoi (args %count-str)))
    (call @flash (args (* 2 %count)))
    (return 0)
))

