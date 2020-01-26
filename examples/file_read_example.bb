(def @read_file (params (%file-name i8*) (%file-size u64*) (%fd i32*)) i8* (do

  (auto %filename-view %struct.StringView)
  (store (call @StringView.makeFromi8$ptr (args %file-name)) %filename-view)

  (auto %file %struct.File)
  (store (call @File.open (args %filename-view)) %file)

  (auto %content-view %struct.StringView)
  (store (call @File$ptr.read (args %file)) %content-view)

  (store (load (index %file 1)) %fd)
  (store (load (index %content-view 1)) %file-size)
  (return (load (index %content-view 0)))
))
