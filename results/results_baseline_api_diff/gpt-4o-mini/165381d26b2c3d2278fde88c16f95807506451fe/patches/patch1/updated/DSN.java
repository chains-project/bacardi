private static class UnitTestsVisitor extends VoidVisitorAdapter<Void> {

    private final List<Integer> l;

    private final com.github.javaparser.printer.configuration.PrettyPrinterConfiguration noComments = new com.github.javaparser.printer.configuration.PrettyPrinterConfiguration().setPrintComments(false);

    UnitTestsVisitor(List<Integer> numbers) {
        this.l = numbers;
    }

    @Override
    public void visit(MethodDeclaration n, Void arg) {
        System.out.println(n.getNameAsString());
        if (n.toString(noComments).contains("solve")) {
            n.getRange().ifPresent(r -> l.add(r.end.line - r.begin.line));
        }
        super.visit(n, arg);
    }
}