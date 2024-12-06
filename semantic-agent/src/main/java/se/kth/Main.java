package se.kth;

public class Main {
    public static void main(String[] args) {
        foo(2);
        System.out.println("Hello world!");
        foo(1);
        foo(0);
    }

    public static String foo(int i) {
        System.out.println("Now in the target method");
        return i % 2 == 0 ? "even" : "odd";
    }
}