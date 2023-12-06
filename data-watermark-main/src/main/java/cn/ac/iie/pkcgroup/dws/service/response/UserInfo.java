package cn.ac.iie.pkcgroup.dws.service.response;

import lombok.Data;

import java.util.ArrayList;

@Data
public class UserInfo {
    ArrayList<String> roles;
    String name;
}
