public class T {
    static boolean x(int n) {
        boolean r = false;
        for (int i = 0; i < n; i++) r = !r;
        if (r) { r = n > 2; } else { r = n < 1; }
        return r;
    }
}
