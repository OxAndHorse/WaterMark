package cn.ac.iie.pkcgroup.dws.config.mock;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;

// ONLY USED FOR DEMO
@Data
public class MockAddress {
    public ArrayList<String> district;
    public HashMap<String, ArrayList<String>> avenue;
    public ArrayList<String> community;

}
