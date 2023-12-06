package cn.ac.iie.pkcgroup.dws.comm.response;

import cn.ac.iie.pkcgroup.dws.response.BasicStatusCodes;

public class StatusCodes extends BasicStatusCodes {
    public final static int CODE_AUTH_FAIL = 0x01;
    public final static int CODE_NO_DB_SET = 0x02;
    public final static int CODE_ILLEGAL_PARAMS = 0x03;
    public final static int CODE_UNAUTHORIZED_REQUEST = 0x04;
    public final static String MSG_AUTH_FAIL = "Unauthorized account!";
    public final static String MSG_NO_DB_SET = "No database has been set!";
    public final static String MSG_ILLEGAL_PARAMS = "Illegal parameters!";
    public final static String MSG_UNAUTHORIZED_REQUEST = "Unauthorized request!";
}
