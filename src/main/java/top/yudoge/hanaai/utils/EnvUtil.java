package top.yudoge.hanaai.utils;

import io.github.cdimascio.dotenv.Dotenv;

public class EnvUtil {

    private static final Dotenv env;

    static {
        env = Dotenv.configure()
                .ignoreIfMissing()
                .load();
    }

    public static String get(String key) {
        return env.get(key);
    }

}
