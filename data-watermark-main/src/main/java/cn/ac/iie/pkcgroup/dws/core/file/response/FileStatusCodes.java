package cn.ac.iie.pkcgroup.dws.core.file.response;

import cn.ac.iie.pkcgroup.dws.response.WatermarkStatusCodes;

public class FileStatusCodes extends WatermarkStatusCodes {
    // Error code starts from 0x80
    public final static int CODE_INIT_ENCODER_FAIL = 0x80;
    public final static int CODE_EMBEDDING_FAIL = 0x81;
    public final static int CODE_INIT_DECODER_FAIL = 0x90;
    public final static int CODE_EXTRACTING_FAIL = 0x91;
    public final static int CODE_SAVE_MAPPING_FAIL = 0xA0;
    public final static int CODE_GET_MAPPING_FAIL = 0xA1;
    public final static int CODE_NO_SUCH_FILE = 0xA2;
    public final static int CODE_ILLEGAL_FILENAME = 0xA3;
    public final static String MSG_INIT_ENCODER_FAIL = "Unable to initialize the file encoder!";
    public final static String MSG_EMBEDDING_FAIL = "Unable to embed watermark!";
    public final static String MSG_INIT_DECODER_FAIL = "Unable to initialize the file decoder!";
    public final static String MSG_EXTRACTING_FAIL = "Unable to extract watermark!";
    public final static String MSG_SAVE_MAPPING_FAIL = "Unable to record file id mapping!";
    public final static String MSG_GET_MAPPING_FAIL = "Unable to find file according to this file id (name)!";
    public final static String MSG_NO_SUCH_FILE = "No such file!";
    public final static String MSG_ILLEGAL_FILENAME = "Illegal file name!";
}
