package cn.ac.iie.pkcgroup.dws.config.mock;

import cn.ac.iie.pkcgroup.dws.utils.SpringContextUtils;
import cn.binarywang.tools.generator.base.GenericGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class MockAddressGenerator extends GenericGenerator {
    MockAddress address;
    final static String ADDR_PREFIX = "广东省广州市";
    private static final GenericGenerator instance = new MockAddressGenerator();

    private MockAddressGenerator() {
        MockConfig mockConfig = (MockConfig) SpringContextUtils.getContext().getBean("mockConfig");
        address = mockConfig.getMockAddressConfig();
    }

    public static GenericGenerator getInstance() {
        return instance;
    }

    public String generate() {
        ArrayList<String> districts = address.getDistrict();
        HashMap<String, ArrayList<String>> map = address.getAvenue();
        Random random = new Random();
        int districtIndex = random.nextInt(districts.size());
        String district = districts.get(districtIndex);
        ArrayList<String> avenues = map.get(district);
        int avenueIndex = random.nextInt(avenues.size());
        String addr = ADDR_PREFIX + district + avenues.get(avenueIndex);
        if (random.nextBoolean()) {
            addr += (1 + random.nextInt(20)) + "栋" + (random.nextInt(5) + 1) + "单元" + ((1 + random.nextInt(20)) * 100 + 1 + random.nextInt(5));
        } else {
            addr += "XXX路" + (1 + random.nextInt(120)) + "号";
        }
        return addr;
    }
}
