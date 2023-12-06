package cn.ac.iie.pkcgroup.dws.comm.response;

import cn.ac.iie.pkcgroup.dws.response.BasicResponse;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class ExtractResponse extends BasicResponse {
    String extractedMessage;
}
