(def @basename_ (params (%path %struct.StringView) (%i u64)) %struct.StringView (do
  (let %FORWARD_SLASH (+ 47 (0 i8)))
  (if (== 0 %i) (do
    (return %path)
  ))
; (if (== ()))
  (return (call @basename_ (args %path (- %i 1))))
))

(def @basename (params (%path %struct.StringView)) %struct.StringView (do
  (call @basename_ (args %path %path (call @StringView.strlen %path)))
))