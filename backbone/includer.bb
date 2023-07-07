; the include pass implementation for the backbone programming language

(def @Includer.on-Program (params (%texp %struct.Texp*)) %struct.Texp (do
  (return (load %texp))
))