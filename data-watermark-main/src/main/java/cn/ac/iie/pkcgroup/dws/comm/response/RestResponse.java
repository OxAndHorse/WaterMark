package cn.ac.iie.pkcgroup.dws.comm.response;

import cn.ac.iie.pkcgroup.dws.response.BasicResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@Data
public class RestResponse extends BasicResponse {

    public RestResponse(int code, String msg) {
         super(code, msg);
    }
}
