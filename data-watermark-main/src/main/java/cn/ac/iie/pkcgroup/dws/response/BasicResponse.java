package cn.ac.iie.pkcgroup.dws.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class BasicResponse {
    public int statusCode;
    public String message;
}
