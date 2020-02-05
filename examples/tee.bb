(include "../lib/core.bb")

(def @tee (params (%from-fd i32) (%to-1-fd i32) (%to-2-fd i32)) void (do
    (auto %char i8)
    (call @read (args %from-fd %char 1))

    (if (>= %to-1-fd 0) (do
        (call @write (args %to-1-fd %char 1))
    ))
    (if (>= %to-2-fd 0) (do
        (call @write (args %to-2-fd %char 1))
    ))

    (call-tail @tee (args %from-fd %to-1-fd %to-2-fd))
    (return-void)
))

(def @main (params (%argc i32) (%argv i8**)) i32 (do
    (if (== 1 %argc) (do
        (call @tee (args 0 1 -1))
    ))
    (if (== 2 %argc) (do
        (let %path-ptr (cast i8** (+ (cast u64 %argv) 8)))

        (auto %path %struct.String)
        (store (call @String.makeFromi8$ptr (args (load %path-ptr))) %path)
        
        (auto %file %struct.File)
        (store (call @File.openrw (args (cast %struct.StringView* %path))) %file)

        (let %fd (load (index %file 1)))
        
        (call @tee (args 0 %fd 1))
    ))
    (if (> 2 %argc) (do
        (call @i8$ptr.unsafe-println (args "usage: tee [file]\00"))
        (return 1)
    ))

    (return 0)
))
