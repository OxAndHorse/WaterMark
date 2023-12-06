package cn.ac.iie.pkcgroup.dws;

import java.util.HashMap;
import java.util.Map;

public class Constants {
    public static int NORMAL_CODE = 0x00;
    public static String FILE_NAME_HEADER = "File-Name";

    public static final String DEFAULT_DB_NAME = "pkc_db"; // test only
    public static final String DEFAULT_TABLE_NAME = "test_data"; // test only

    public static int ROLE_ADMIN = 0;
    public static int ROLE_OWNER = 1;
    public static int ROLE_USER = 2;

    public static Map<Integer, String> ROLE_MAP;

    public static final int AUTH_KEY_LENGTH = 16;

    static {
        ROLE_MAP = new HashMap<>(3);
        ROLE_MAP.put(ROLE_ADMIN, "admin");
        ROLE_MAP.put(ROLE_OWNER, "owner");
        ROLE_MAP.put(ROLE_USER, "user");
    }
}
