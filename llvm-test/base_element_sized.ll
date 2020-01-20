; ModuleID = ../hello-world.bb
target datalayout = "e-m:e-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-gnu"

%struct.A = type { %struct.B };

define void @example(%struct.A* %this) {
entry:
  %$2 = getelementptr inbounds %struct.A, %struct.A* %this, i32 0, i32 0
  ret void
}

%struct.B = type { i64 };
; ^
; NOTE: base element of getelementptr must be sized
; - all structs used in a function must not only be defined, but also defined beforehand
; - the clang llvm ir assembler will only report the above error ("base element ... must be sized") for
;   when the struct is indirectly referenced (as a field of another struct) but not defined
;
; NOTE: the struct's field must not be of type pointer to another struct, but the struct itself
; - LLVM seems to complain about something not being "sized" if the struct is simply not defined
;
; structs can be defined in any order relative to each other
; - clang seems to not error on recursive structs either


define i32 @main() {
entry:
  ret i32 0
}

; RUN `clang -Wno-override-module base_element_sized.ll; echo $?` to see error

; SOLUTION: define all structs in the beginning of the module