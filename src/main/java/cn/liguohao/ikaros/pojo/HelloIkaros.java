package cn.liguohao.ikaros.pojo;

/**
 * @author li-guohao
 */
public class HelloIkaros {
    private static final String MSG = "Hello Ikaros!";
    private String message;

    private HelloIkaros() {
    }

    public static HelloIkaros ofMsg() {
        return new HelloIkaros().setMessage(MSG);
    }

    public String message() {
        return message;
    }

    public HelloIkaros setMessage(String message) {
        this.message = message;
        return this;
    }

    @Override
    public String toString() {
        return "HelloIkaros{" + "message='" + message + '\'' + '}';
    }
}
