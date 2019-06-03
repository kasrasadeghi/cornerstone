module tester;

import std.process;
import std.stdio;

int run(string s) {
  auto pipes = pipeProcess(s, Redirect.stdout);
  scope(exit) wait(pipes.pid);
  while(! tryWait(pipes.pid).terminated) {
    write(pipes.stdout.readln);
  }
  return wait(pipes.pid);
}

void main() {
  run("make");
}