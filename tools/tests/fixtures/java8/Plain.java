package fx;

/** Java 8 only: must parse. Mentions record and "case A -> b" only in comments/strings. */
public final class Plain {
    private final String note = "record Foo(int x) and case A -> b and \"\"\" are just text";

    public int size(Object o) {
        if (o instanceof String) {
            return ((String) o).length();
        }
        switch (note.length()) {
            case 0:
                return 0;
            default:
                return 1;
        }
    }
}
