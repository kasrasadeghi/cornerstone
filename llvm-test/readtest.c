#include <sys/types.h>
#include <sys/stat.h> // 
#include <sys/mman.h> // mmap, PROT_READ, MAP_PRIVATE
#include <fcntl.h>    // O_RDONLY, open
#include <unistd.h>   // lseek, SEEK_END

#include <stdio.h>

int main() {
  int fd = open("lib2/core.bb.type.tall", O_RDWR);
  long size = lseek(fd, 0, SEEK_END);
  printf("%ld\n", size);
  
  char* content = mmap(0, size, PROT_READ | PROT_WRITE, MAP_PRIVATE, fd, 0);
  char* content_end = content + size;

  printf("%lu\n", (unsigned long)content);
  printf("%lu\n", (unsigned long)content_end);
  
  for (char* i = content; i != content_end; ++i) {
    
  }
}
