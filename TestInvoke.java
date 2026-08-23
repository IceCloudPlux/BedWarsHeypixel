import java.lang.reflect.Method;

public class TestInvoke {
    public static void main(String[] args) throws Exception {
        System.out.println("java.version=" + System.getProperty("java.version"));
        Class<?> c = Class.forName("a.hr");
        System.out.println("class loaded: " + c.getName());
        Method m = c.getDeclaredMethod("a", String.class);
        m.setAccessible(true);
        // null key -> verify() short-circuits to false, no network call
        Object r = m.invoke(null, (Object) null);
        System.out.println("invoke a(null) = " + r);
        // empty key -> also false
        Object r2 = m.invoke(null, "");
        System.out.println("invoke a(\"\") = " + r2);
        System.out.println("NO VerifyError - class runs fine");
    }
}
