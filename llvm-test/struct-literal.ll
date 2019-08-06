; ModuleID = 'struct-literal.c'
source_filename = "struct-literal.c"
target datalayout = "e-m:e-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-pc-linux-gnu"

%struct.Hello = type { i32, i32, i8, i32, i64 }

@staticHello = dso_local local_unnamed_addr global %struct.Hello { i32 5, i32 6, i8 10, i32 70, i64 15 }, align 8

; Function Attrs: norecurse nounwind readonly sspstrong uwtable
define dso_local i32 @getA(%struct.Hello* byval nocapture readonly align 8) local_unnamed_addr #0 {
  %2 = getelementptr inbounds %struct.Hello, %struct.Hello* %0, i64 0, i32 0
  %3 = load i32, i32* %2, align 8, !tbaa !4
  ret i32 %3
}

; Function Attrs: norecurse nounwind readonly sspstrong uwtable
define dso_local i32 @getB(%struct.Hello* byval nocapture readonly align 8) local_unnamed_addr #0 {
  %2 = getelementptr inbounds %struct.Hello, %struct.Hello* %0, i64 0, i32 1
  %3 = load i32, i32* %2, align 4, !tbaa !10
  ret i32 %3
}

; Function Attrs: norecurse nounwind readonly sspstrong uwtable
define dso_local i32 @getE(%struct.Hello* byval nocapture readonly align 8) local_unnamed_addr #0 {
  %2 = getelementptr inbounds %struct.Hello, %struct.Hello* %0, i64 0, i32 4
  %3 = load i64, i64* %2, align 8, !tbaa !11
  %4 = trunc i64 %3 to i32
  ret i32 %4
}

; Function Attrs: nounwind sspstrong uwtable
define dso_local void @setToStatic(%struct.Hello* nocapture) local_unnamed_addr #1 {
  %2 = bitcast %struct.Hello* %0 to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* align 8 %2, i8* align 8 bitcast (%struct.Hello* @staticHello to i8*), i64 24, i1 false), !tbaa.struct !12
  ret void
}

; Function Attrs: argmemonly nounwind
declare void @llvm.memcpy.p0i8.p0i8.i64(i8* nocapture writeonly, i8* nocapture readonly, i64, i1) #2

; Function Attrs: nounwind sspstrong uwtable
define dso_local void @setA(%struct.Hello* noalias nocapture sret, %struct.Hello* byval nocapture align 8) local_unnamed_addr #1 {
  %3 = getelementptr inbounds %struct.Hello, %struct.Hello* %1, i64 0, i32 0
  store i32 5, i32* %3, align 8, !tbaa !4
  %4 = bitcast %struct.Hello* %0 to i8*
  %5 = bitcast %struct.Hello* %1 to i8*
  call void @llvm.memcpy.p0i8.p0i8.i64(i8* align 8 %4, i8* nonnull align 8 %5, i64 24, i1 false), !tbaa.struct !12
  ret void
}

attributes #0 = { norecurse nounwind readonly sspstrong uwtable "correctly-rounded-divide-sqrt-fp-math"="false" "disable-tail-calls"="false" "less-precise-fpmad"="false" "min-legal-vector-width"="0" "no-frame-pointer-elim"="false" "no-infs-fp-math"="false" "no-jump-tables"="false" "no-nans-fp-math"="false" "no-signed-zeros-fp-math"="false" "no-trapping-math"="false" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+fxsr,+mmx,+sse,+sse2,+x87" "unsafe-fp-math"="false" "use-soft-float"="false" }
attributes #1 = { nounwind sspstrong uwtable "correctly-rounded-divide-sqrt-fp-math"="false" "disable-tail-calls"="false" "less-precise-fpmad"="false" "min-legal-vector-width"="0" "no-frame-pointer-elim"="false" "no-infs-fp-math"="false" "no-jump-tables"="false" "no-nans-fp-math"="false" "no-signed-zeros-fp-math"="false" "no-trapping-math"="false" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+fxsr,+mmx,+sse,+sse2,+x87" "unsafe-fp-math"="false" "use-soft-float"="false" }
attributes #2 = { argmemonly nounwind }

!llvm.module.flags = !{!0, !1, !2}
!llvm.ident = !{!3}

!0 = !{i32 1, !"wchar_size", i32 4}
!1 = !{i32 7, !"PIC Level", i32 2}
!2 = !{i32 7, !"PIE Level", i32 2}
!3 = !{!"clang version 8.0.1 (tags/RELEASE_801/final)"}
!4 = !{!5, !6, i64 0}
!5 = !{!"Hello", !6, i64 0, !6, i64 4, !7, i64 8, !6, i64 12, !9, i64 16}
!6 = !{!"int", !7, i64 0}
!7 = !{!"omnipotent char", !8, i64 0}
!8 = !{!"Simple C/C++ TBAA"}
!9 = !{!"long", !7, i64 0}
!10 = !{!5, !6, i64 4}
!11 = !{!5, !9, i64 16}
!12 = !{i64 0, i64 4, !13, i64 4, i64 4, !13, i64 8, i64 1, !14, i64 12, i64 4, !13, i64 16, i64 8, !15}
!13 = !{!6, !6, i64 0}
!14 = !{!7, !7, i64 0}
!15 = !{!9, !9, i64 0}
