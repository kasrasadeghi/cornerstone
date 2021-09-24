;========== File ==================================================================================

(struct %struct.File
  (%name %struct.String)
  (%file_descriptor i32))

(def @File._open (params (%filename-view %struct.StringView*) (%flags i32)) %struct.File (do
  (auto %result %struct.File)
  
  (let %filename (load (index %filename-view 0)))
  (store (call @String.makeFromStringView (args %filename-view)) (index %result 0))

  (let %fd (call-vargs @open (args %filename %flags)))

  (if (== %fd (- (0 i32) 1)) (do
    (call-vargs @printf (args "error opening file at '%s'\0A\00" %filename))
    (call @exit (args 1))
  ))

  (store %fd (index %result 1))
  (return (load %result))
))

(def @File.open (params (%filename-view %struct.StringView*)) %struct.File (do
  (let %O_RDONLY (+ 1 (0 i32)))
  (return (call @File._open (args %filename-view %O_RDONLY)))
))

(def @File.openrw (params (%filename-view %struct.StringView*)) %struct.File (do
  (let %O_RDWR (+ 2 (0 i32)))
  (return (call @File._open (args %filename-view %O_RDWR)))
))

(def @File$ptr.getSize (params (%this %struct.File*)) i64 (do
  (let %SEEK_END (+ 2 (0 i32)))
  (return (call @lseek (args (load (index %this 1)) 0 %SEEK_END)))
))

(def @File$ptr.read (params (%this %struct.File*)) %struct.StringView (do
  (let %PROT_READ (+ 3 (0 i32)))
  (let %MAP_PRIVATE (+ 2 (0 i32)))

  (let %file-length (call @File$ptr.getSize (args %this)))
  (let %char-ptr (call @mmap (args (cast i8* (0 u64)) %file-length %PROT_READ %MAP_PRIVATE (load (index %this 1)) 0)))
  (return (call @StringView.make (args %char-ptr %file-length)))
))

(def @File$ptr.readwrite (params (%this %struct.File*)) %struct.StringView (do
  (let %PROT_RDWR (+ 3 (0 i32)))
  (let %MAP_PRIVATE (+ 2 (0 i32)))

  (let %file-length (call @File$ptr.getSize (args %this)))
  (let %char-ptr (call @mmap (args (cast i8* (0 u64)) %file-length %PROT_RDWR %MAP_PRIVATE (load (index %this 1)) 0)))
  (return (call @StringView.make (args %char-ptr %file-length)))
))

(def @File.unread (params (%view %struct.StringView*)) void (do
  (call @munmap (args (load (index %view 0)) (load (index %view 1))))
  (return-void)
))

(def @File$ptr.close (params (%this %struct.File*)) void (do
; TODO free string
  (call @close (args (load (index %this 1))))
  (return-void)
))

;========== File tests ============================================================================

(def @test.file-cat params void (do
  (auto %filename %struct.StringView)
  (call @StringView$ptr.set (args %filename "todo.json\00"))
  
  (auto %file %struct.File)
  (store (call @File.open (args %filename)) %file)
  
; print file name
  (call @String$ptr.println (args (index %file 0)))

  (auto %content %struct.StringView)
  (store (call @File$ptr.read (args %file)) %content)

  (call @StringView$ptr.print (args %content))

  (call @File.unread (args %content))
  (call @File$ptr.close (args %file))

  (return-void)
))

(def @test.file-size params void (do
  (auto %filename %struct.StringView)
	(call @StringView$ptr.set (args %filename "lib2/core.bb.type.tall\00"))
  
  (auto %file %struct.File)
  (store (call @File.open (args %filename)) %file)

  (let %size (call @File$ptr.getSize (args %file)))

  (call-vargs @printf (args "%ul\0A\00" %size))

  (call @File$ptr.close (args %file))

  (return-void)
))

(def @test.bad-file-open params void (do
  (auto %filename %struct.StringView)
	(call @StringView$ptr.set (args %filename "lib2/core.bb.type.tall\00"))
  (call @File.open (args %filename))
  (return-void)
))