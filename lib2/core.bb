(include "lib2/stdc.bb")
(include "lib2/i8.bb")
(include "lib2/string.bb")
(include "lib2/file.bb")
(include "lib2/reader.bb")
(include "lib2/intstr.bb")

(include "lib2/result.bb")
(include "lib2/optional.bb")

(include "lib2/texp.bb")
(include "lib2/parser.bb")
(include "lib2/pprint.bb")
(include "lib2/grammar.bb")
(include "lib2/matcher.bb")

;========== main program ==========================================================================

(def @main params i32 (do
; (call @test.Texp-value-get args)
; (call @test.texp-pretty-print args)
; (call @test.Texp-find-program-grammar args)
; (call @test.Texp-clone-atom args)
; (call @test.Texp-clone-hard args)
; (call @test.string-prepend-helloworld args)

; (call @test.matcher-simple args)
; (call @test.matcher-kleene-seq args)
; (call @test.matcher-choice args)
; (call @test.matcher-exact args)
; (call @test.matcher-value args)
; (call @test.matcher-empty-kleene args)
; (call @test.matcher-regexString args)

; (call @test.Texp-makeFromi8$ptr args)
; (call @test.Texp-value-view args)
; (call @test.matcher-regexInt args)

  (call @test.matcher-self args)

  (return 0)
))
