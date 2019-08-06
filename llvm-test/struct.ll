; ModuleID = 'struct.c'
source_filename = "struct.c"
target datalayout = "e-m:e-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-pc-linux-gnu"

%struct.Hello = type { i32, i32 }

; Function Attrs: norecurse nounwind readonly sspstrong uwtable
define dso_local i32 @getA(%struct.Hello* nocapture readonly) local_unnamed_addr #0 {
  %2 = getelementptr inbounds %struct.Hello, %struct.Hello* %0, i64 0, i32 0
  %3 = load i32, i32* %2, align 4, !tbaa !4
  ret i32 %3
}

; Function Attrs: norecurse nounwind readonly sspstrong uwtable
define dso_local i32 @getB(%struct.Hello* nocapture readonly) local_unnamed_addr #0 {
  %2 = getelementptr inbounds %struct.Hello, %struct.Hello* %0, i64 0, i32 1
  %3 = load i32, i32* %2, align 4, !tbaa !9
  ret i32 %3
}

attributes #0 = { norecurse nounwind readonly sspstrong uwtable "correctly-rounded-divide-sqrt-fp-math"="false" "disable-tail-calls"="false" "less-precise-fpmad"="false" "min-legal-vector-width"="0" "no-frame-pointer-elim"="false" "no-infs-fp-math"="false" "no-jump-tables"="false" "no-nans-fp-math"="false" "no-signed-zeros-fp-math"="false" "no-trapping-math"="false" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+fxsr,+mmx,+sse,+sse2,+x87" "unsafe-fp-math"="false" "use-soft-float"="false" }

!llvm.module.flags = !{!0, !1, !2}
!llvm.ident = !{!3}

!0 = !{i32 1, !"wchar_size", i32 4}
!1 = !{i32 7, !"PIC Level", i32 2}
!2 = !{i32 7, !"PIE Level", i32 2}
!3 = !{!"clang version 8.0.1 (tags/RELEASE_801/final)"}
!4 = !{!5, !6, i64 0}
!5 = !{!"Hello", !6, i64 0, !6, i64 4}
!6 = !{!"int", !7, i64 0}
!7 = !{!"omnipotent char", !8, i64 0}
!8 = !{!"Simple C/C++ TBAA"}
!9 = !{!5, !6, i64 4}
