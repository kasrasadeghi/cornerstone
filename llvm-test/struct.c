struct Hello {
  int a;
  int b;
};

int getA(struct Hello* hello) {
  return hello->a;
}

int getB(struct Hello* hello) {
  return hello->b;
}