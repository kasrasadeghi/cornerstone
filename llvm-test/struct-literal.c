struct Hello {
  int a;
  int b;
  char c;
  int d;
  long e;
};

struct Hello staticHello = {.a = 5, .b = 6, .c = '\n', .d = 70, .e = 15 };

int getA(const struct Hello hello) {
  return hello.a;
}

int getB(const struct Hello hello) {
  return hello.b;
}

int getE(const struct Hello hello) {
  // gep, trunc
  return hello.e;
}

void setToStatic(struct Hello* hello) {
  // memcpy
  *hello = staticHello;
}

struct Hello setA(struct Hello this) {
  this.a = 5;
  return this;
}