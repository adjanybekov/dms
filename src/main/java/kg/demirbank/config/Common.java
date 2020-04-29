package kg.demirbank.config;

public class Common {
    public static <T> T getValue(T o, T def){
        if(o==null){
            return def;
        }
        return o;
    }
}
