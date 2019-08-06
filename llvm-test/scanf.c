#include <unistd.h>

int main() {
  char arr;
  read(0, &arr, 1);
  return 0;
}
