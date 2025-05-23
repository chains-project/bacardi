private static class UnitTestsVisitor extends VoidVisitorAdapter<Void> {

    private final List<Integer> l;

    UnitTestsVisitor(List<Integer> numbers) {
        this.l = numbers;
    }

    @Override
    public void visit(MethodDeclaration n, Void arg) {
        System.out.println(n.getNameAsString());
        if (n.toString().contains("solve")) {
            n.getRange().ifPresent(r -> l.add(r.end.line - r.begin.line));
        }
        super.visit(n, arg);
    }
}