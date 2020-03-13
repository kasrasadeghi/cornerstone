(include "core.bb")

(def @main-test-texp params void (do
; (call @test.Texp-value-get args)
  (call @test.texp-pretty-print args)
; (call @test.Texp-find-program-grammar args)
; (call @test.Texp-clone-atom args)
; (call @test.Texp-clone-hard args)

; (call @test.Texp-makeFromi8$ptr args)
; (call @test.Texp-value-view args)

  (return-void)
))

(def @main-test-string params void (do
; (call @test.string-prepend-helloworld args)
  (return-void)
))

(def @main-test-matcher params void (do
; (call @test.matcher-simple args)
; (call @test.matcher-kleene-seq args)
; (call @test.matcher-choice args)
; (call @test.matcher-exact args)
; (call @test.matcher-value args)
; (call @test.matcher-empty-kleene args)
; (call @test.matcher-regexString args)

; (call @test.matcher-regexInt args)
; (call @test.matcher-self args)

  (return-void)
))

;========== main program ==========================================================================

(def @main params i32 (do
  (call @main-test-texp args)
  (return 0)
))
