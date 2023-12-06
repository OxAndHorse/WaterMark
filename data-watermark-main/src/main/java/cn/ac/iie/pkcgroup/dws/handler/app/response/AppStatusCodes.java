package cn.ac.iie.pkcgroup.dws.handler.app.response;

import cn.ac.iie.pkcgroup.dws.response.BasicStatusCodes;

public class AppStatusCodes extends BasicStatusCodes {
    // ERROR CODE start from 0xC0
    public final static int CODE_INVALID_APP_REQUEST = 0xC0;
    public final static int CODE_READ_PROPERTY_FAIL = 0xC1;
    public final static int CODE_UNREGISTERED_SYSTEM = 0xC2;
    public final static int CODE_INVALID_DB_PARAMS = 0xC3;
    public final static int CODE_FAIL_TO_UPDATE_DB_CONFIG = 0xC4;
    public final static String MSG_INVALID_APP_REQUEST = "Invalid application request!";
    public final static String MSG_READ_PROPERTY_FAIL = "Unable to read the property file!";
    public final static String MSG_UNREGISTERED_SYSTEM = "No register information is found for current system!";
    public final static String MSG_INVALID_DB_PARAMS = "Invalid database parameters!";
    public final static String MSG_FAIL_TO_UPDATE_DB_CONFIG = "Fail to update database connection configuration, please check connection of the target database!";
}
