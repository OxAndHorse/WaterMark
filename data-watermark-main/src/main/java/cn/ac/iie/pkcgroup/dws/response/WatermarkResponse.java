package cn.ac.iie.pkcgroup.dws.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@Data
@SuperBuilder
public class WatermarkResponse extends BasicResponse {
    public WatermarkResponse(int code, String msg) {
        super(code, msg);
    }
}
