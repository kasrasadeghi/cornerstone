(include "core.bb")

;========== main program ===========================================================================

(def @main (params (%argc i32) (%argv i8**)) i32 (do

  (if (!= 2 %argc) (do
    (call @i8$ptr.unsafe-println (args
      "usage: matcher <test-case> from <test-case> in ../backbone-test/matcher/*\00"))
    (call @exit (args 1))
  ))

  (let %arg (cast i8** (+ 8 (cast u64 %argv))))

  (auto %test-case %struct.String)
  (store (call @String.makeFromi8$ptr (args (load %arg))) %test-case)
  (call @i8$ptr.unsafe-print (args "test case: \00"))
  (call @String$ptr.println (args %test-case))

  (auto %test-dir %struct.String)
  (store (call @String.makeFromi8$ptr (args "/home/kasra/projects/backbone-test/matcher/\00")) %test-dir)

  (auto %test-case-path %struct.String)
  (store (call @String.add (args %test-dir %test-case)) %test-case-path)

  (auto %grammar-path %struct.String)
  (store (call @String.makeFromi8$ptr (args ".grammar\00")) %grammar-path)
  (call @String$ptr.prepend (args %grammar-path %test-case-path))

  (auto %texp-file-path %struct.String)
  (store (call @String.makeFromi8$ptr (args ".texp\00")) %texp-file-path)
  (call @String$ptr.prepend (args %texp-file-path %test-case-path))
  
  (auto %prog %struct.Texp)
  (store (call @Parser.parse-file (args (cast %struct.StringView* %texp-file-path))) %prog)

  (auto %matcher %struct.Matcher)
  (store (call @Grammar.make (args (call @Parser.parse-file (args (cast %struct.StringView* %grammar-path)))))
    (index %matcher 0))

  (auto %start-production %struct.StringView)
  (store (call @StringView.makeFromi8$ptr (args "Program\00")) %start-production)

  (auto %result %struct.Texp)
  (store (call @Matcher$ptr.is (args %matcher %prog %start-production)) %result)
  (call @Texp$ptr.parenPrint (args %result))
  (call @println args)
  
  (return 0)
))
