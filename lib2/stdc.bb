;========== C declarations ========================================================================

; GLOSSARY
; fd -> file descripter
; len -> length
; dst -> destination
; src -> source
; prot -> protocol
; unisuccess -> unix success: 0 success, -1 on failure and set ERRNO

;; alloc
(decl @malloc (types u64) i8*)
(decl @free (types i8*) void)

;                     ptr newsize -> newptr
(decl @realloc (types i8* u64) i8*)
; remember to store newptr or keep it as a value as it might change from ptr

;                    len size
(decl @calloc (types u64 u64) i8*)

;; io
(decl @printf (types i8* ...)     i32)
(decl @puts   (types i8*)         i32)

;                    fd  src len -> len
(decl @write  (types i32 i8* u64) i64)

;                    fd  buf len -> i64
(decl @read   (types i32 i8* u64) i64)

;                    fd -> unisuccess
(decl @fflush (types i32) i32)

; STDIN = 0, STDOUT = 1, STDERR = 2

;                    dest src len
(decl @memcpy (types i8* i8* u64) i8*)

;; file

;            filename, O_flags, S_mode -> fd:i32
(decl @open (types i8* i32 ...) i32)
; #define O_RDONLY	     00

;             file_descriptor, offset, SEEK_whence
(decl @lseek (types i32 i64 i32) i64)
; # define SEEK_END	2	/* Seek from end of file.  */

;                      length prot flags fd offset -> out
(decl @mmap (types i8* u64 i32 i32 i32 i64) i8*)
; #define PROT_READ	0x1		/* Page can be read.  */
; #define PROT_WRITE	0x2		/* Page can be written.  */
; #define MAP_PRIVATE	0x02		/* Changes are private.  */

;                    src len -> unisuccess
(decl @munmap (types i8* u64) i32)

;                   fd -> unisuccess
(decl @close (types i32) i32)

;; misc
(decl @exit (types i32) void)