package cn.ac.iie.pkcgroup.dws.core.db.response;

import cn.ac.iie.pkcgroup.dws.response.WatermarkStatusCodes;

public class DBStatusCodes extends WatermarkStatusCodes {
    // Error code starts from 0x40
    public final static int CODE_INIT_ENCODER_FAIL = 0x40;
    public final static int CODE_FETCH_DATA_FAIL = 0x41;
    public final static int CODE_MISMATCHED_FIELD = 0x42;
    public final static int CODE_EXPORT_TO_NEW_TABLE_FAIL = 0x43;
    public final static int CODE_EXPORT_TO_CSV_FILE_FAIL = 0x44;
    public final static int CODE_INIT_DECODER_FAIL = 0x45;
    public final static int CODE_NO_SOURCE_TABLE = 0x46;
    public final static int CODE_NO_SUCH_CSV_DATA = 0x47;
    public final static int CODE_READ_CSV_DATA_FAIL = 0x48;
    public final static int CODE_NO_PRIMARY_KEY = 0x49;
    public final static int CODE_FAIL_TO_EXTRACT_DW = 0x4A;
    public final static int CODE_FAIL_TO_GENERATE_DW = 0x4B;
    public final static int CODE_NO_DW_METHOD_ALLOWED = 0x4C;
    public final static int CODE_NO_DW_METHOD_SUCCESS = 0x4D;
    public final static int CODE_NO_CSV_HEADER = 0x4E;
    public final static String MSG_INIT_ENCODER_FAIL = "Unable to initialize the encoder!";
    public final static String MSG_FETCH_DATA_FAIL = "Unable to fetch data from the table!";
    public final static String MSG_MISMATCHED_FIELD = "Mismatched field!";
    public final static String MSG_EXPORT_TO_NEW_TABLE_FAIL = "Fail to save watermark data to new table!";
    public final static String MSG_EXPORT_TO_CSV_FILE_FAIL = "Fail to save watermark data as CSV file!";
    public final static String MSG_INIT_DECODER_FAIL = "Unable to initialize the decoder!";
    public final static String MSG_NO_SOURCE_TABLE = "The table was never resolved!";
    public final static String MSG_NO_SUCH_CSV_DATA = "No such CSV file!";
    public final static String MSG_READ_CSV_DATA_FAIL = "Fail to fetch the CSV data!";
    public final static String MSG_NO_PRIMARY_KEY = "Cannot process a table without primary key when trying to use column-based algorithms!";
    public final static String MSG_FAIL_TO_EXTRACT_DW = "Fail to extract the watermark!";
    public final static String MSG_FAIL_TO_GENERATE_DW = "Fail to generate the watermark!";
    public final static String MSG_NO_DW_METHOD_ALLOWED = "Do not specify any method for watermarking!";
    public final static String MSG_NO_DW_METHOD_SUCCESS = "None of specified methods works!";
    public final static String MSG_NO_CSV_HEADER = "Missing headers of table data.";
}
