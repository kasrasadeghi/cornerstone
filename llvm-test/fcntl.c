#include <sys/types.h>
#include <sys/stat.h> // 
#include <sys/mman.h> // mmap, PROT_READ, MAP_PRIVATE
#include <fcntl.h>    // O_RDONLY, open
#include <unistd.h>   // lseek, SEEK_END

void open_file() {
  int fd = open("fcntl.c", O_RDONLY);
  long len = lseek(fd, 0, SEEK_END);
  char* data = mmap(0, len, PROT_READ, MAP_PRIVATE, fd, 0);
}