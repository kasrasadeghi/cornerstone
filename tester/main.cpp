#include <sys/types.h> // pid_t
#include <unistd.h> // dup2, fork
#include <sys/wait.h> // wait
#include <fcntl.h>  // open, O_*

#include <cassert>
#include <iostream>
#include <string>
#include <vector>
#include <sstream>
#include <iterator>
#include <algorithm>

auto split_space(std::string s) -> std::vector<std::string> {
  std::stringstream ss(s);
  std::string item;

  std::vector<std::string> acc;
  while (std::getline(ss, item, ' '/*delim*/)) {
    acc.push_back(item);
  }
  return acc;
}

int run(const std::string& s, bool output = true) {
  pid_t pid = fork();
  if (pid == 0) {
    // i am child

    // turn into char* array
    std::vector args = split_space(s);
    std::vector<const char*> cstr_args;
    for (const auto& arg : args) {
      cstr_args.emplace_back(arg.c_str());
    }
    cstr_args.push_back(nullptr);

    if (not output) {
      int fd = open("/dev/null", O_WRONLY);
      dup2(fd, 1); // stdout points to /dev/null
      dup2(fd, 2); // stderr points to /dev/null
      close(fd);
    }

    // exec
    int i = execvp(cstr_args[0], (char* const*) cstr_args.data());

    // error handling
    if (i) perror("tester");

  } else {
    // pid is child, i am parent
    int status;
    wait(&status); // only one child, so no need to waitpid
    return status;
  }
}

int main(int argc, char* argv[]) {
  if (argc != 2) {
    puts("usage: tester <filename>.bb");
    exit(1);
  } else {

  }
  std::string filename{argv[1]};
  run("build/main/cornerstone-cpp ../backbone-test/backbone/" + filename);
  run("");
}