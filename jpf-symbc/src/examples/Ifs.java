public class Ifs {
    public static int iffunction(int a) {
    int b;
        if (a+1<0){
           b = 1; }
        else if (a==0) {
           b = 2; }
        else {
           b = 3;
        }
        return b;
    }

    public static void main(String[] args) {
        iffunction(0);
    }
}