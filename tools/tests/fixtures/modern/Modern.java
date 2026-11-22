package fx;

public sealed interface Modern permits Modern.A {
    final class A implements Modern {
        String text = """
            hello
            """;

        int k(Object o, int d) {
            if (o instanceof String s) {
                return s.length();
            }
            return switch (d) {
                case 1 -> 10;
                default -> {
                    yield 0;
                }
            };
        }
    }
}
