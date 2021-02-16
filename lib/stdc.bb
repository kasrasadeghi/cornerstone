;========== C declarations ========================================================================

; GLOSSARY
; fd -> file descripter
; len -> length
; dst -> destination
; src -> source
; prot -> protocol
; unisuccess -> unix success: 0 success, -1 on failure and set ERRNO

; NOTES
; (3) means from man(3)



;;; alloc ===================================
; (3) stdlib.h -> malloc, free, realloc, calloc

(decl @malloc (types u64) i8*)
(decl @free (types i8*) void)

;                     ptr newsize -> newptr
(decl @realloc (types i8* u64) i8*)
; remember to store newptr or keep it as a value as it might change from ptr

;                    len size
(decl @calloc (types u64 u64) i8*)



;;; io ======================================
; (3) stdio.h -> printf, puts, fflush

(decl @printf (types i8* ...)     i32)
(decl @puts   (types i8*)         i32)

;                    fd -> unisuccess
(decl @fflush (types i32) i32)


; unistd.h -> write, read
; STDIN = 0, STDOUT = 1, STDERR = 2

;                    fd  src len -> len
(decl @write  (types i32 i8* u64) i64)

;                    fd  buf len -> i64
(decl @read   (types i32 i8* u64) i64)



;;; memcpy ==================================
; (3) string.h -> memcpy, memmove

;                    dest src len
(decl @memcpy  (types i8* i8* u64) i8*)
(decl @memmove (types i8* i8* u64) i8*)



;;; file ====================================

; fcntl.h -> open
; fcntl.h > bits/fcntl.h > bits/fcntl-linux.h
;   -> O_RDONLY, O_WRONLY, O_RDWR

;            filename, O_flags, S_mode -> fd:i32
(decl @open (types i8* i32 ...) i32)
; #define O_RDONLY       00
; #define O_WRONLY       01
; #define O_RDWR         02

; unistd.h -> lseek, close, SEEK_END

;             file_descriptor, offset, SEEK_whence
(decl @lseek (types i32 i64 i32) i64)
; # define SEEK_END	2	/* Seek from end of file.  */


; sys/mman.h -> mmap, munmap
; sys/mman.h > bits/mman.h > bits/mman-linux.h
;   -> PROT_READ, PROT_WRITE, MAP_PRIVATE

;                      length prot flags fd offset -> out
(decl @mmap (types i8* u64 i32 i32 i32 i64) i8*)
; #define PROT_READ	0x1		/* Page can be read.  */
; #define PROT_WRITE	0x2		/* Page can be written.  */
; #define MAP_PRIVATE	0x02		/* Changes are private.  */

;                    src len -> unisuccess
(decl @munmap (types i8* u64) i32)

;                   fd -> unisuccess
(decl @close (types i32) i32)



;;; misc ===================================

; (3) stdlib.h -> exit, perro

(decl @exit (types i32) void)

; (3) stdio.h -> perror
; related:
;   errno.h -> errno
(decl @perror (types i8*) void)
