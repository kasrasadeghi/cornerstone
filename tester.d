module tester;

import std.process;
import std.stdio;

/// runs a shell command and returns the exit code
int run(string s) {
  auto pipes = pipeShell(s, Redirect.stdout);
  scope(exit) wait(pipes.pid);
  while(! tryWait(pipes.pid).terminated) {
    write(pipes.stdout.readln);
  }
  return wait(pipes.pid);
}

void main() {
  run("make build");
  string filename = "argcall.bb";
  run("build/main/cornerstone-cpp < ../backbone-test/backbone/" ~ filename);
}