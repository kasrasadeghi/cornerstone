#include <unistd.h>

int main(void) {

  size_t len = 5;
  char string[] = {'A', 'B', '\0', 'C', 'D'};
  int stdin_fd = 0;
  int stdout_fd = 1;
  int stderr_fd = 2;

  write(stdout_fd, string, len);

  return 0;
}