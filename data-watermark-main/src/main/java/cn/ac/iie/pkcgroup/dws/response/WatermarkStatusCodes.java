package cn.ac.iie.pkcgroup.dws.response;

public class WatermarkStatusCodes extends BasicStatusCodes {
    // Start from 0xf0
    public final static int CODE_EXCEPTION = 0xf0;
    public final static String MSG_EXCEPTION = "Embedding watermark fail due to unhandled error!";
}
