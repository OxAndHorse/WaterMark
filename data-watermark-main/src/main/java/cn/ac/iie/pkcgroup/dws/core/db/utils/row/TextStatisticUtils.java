package cn.ac.iie.pkcgroup.dws.core.db.utils.row;

import cn.ac.iie.pkcgroup.dws.config.mock.MockAddressGenerator;
import cn.ac.iie.pkcgroup.dws.config.mock.MockConfig;
import cn.ac.iie.pkcgroup.dws.utils.SpringContextUtils;
import cn.binarywang.tools.generator.ChineseAddressGenerator;
import cn.binarywang.tools.generator.ChineseIDCardNumberGenerator;
import cn.binarywang.tools.generator.ChineseMobileNumberGenerator;
import cn.binarywang.tools.generator.ChineseNameGenerator;
import cn.binarywang.tools.generator.base.GenericGenerator;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class TextStatisticUtils {
    enum TextType {
        ID,
        PHONE,
        ADDRESS,
        NAME,
        DFT, // default
    }

    public static ArrayList<String> forgeRelatedText(ArrayList<String> material, int num, boolean isSinglePrimaryKey) {
        MockConfig mockConfig = (MockConfig) SpringContextUtils.getContext().getBean("mockConfig");
        ArrayList<String> forgedText = new ArrayList<>(num);
        int[] count = new int[TextType.values().length];
        for (String value : material) {
            int counter;
            if (isIdNumber(value)) {
                counter = ++count[TextType.ID.ordinal()];
            } else if (isPhoneNumber(value)) {
                counter = ++count[TextType.PHONE.ordinal()];
            } else if (isAddress(value)) {
                counter = ++count[TextType.ADDRESS.ordinal()];
            } else if (isName(value)) {
                counter = ++count[TextType.NAME.ordinal()];
            } else {
                counter = ++count[TextType.DFT.ordinal()];
            }

            if (counter >= material.size() / 2) break; // over one half
        }
        int maxIndex = findMaxValueIndex(count);
        GenericGenerator generator = null;
        switch (TextType.values()[maxIndex]) {
            case ID:
                generator = ChineseIDCardNumberGenerator.getInstance();
                break;
            case PHONE:
                generator = ChineseMobileNumberGenerator.getInstance();
                break;
            case ADDRESS:
                generator = mockConfig.getUseMock() == 1 ? /*use mock*/ MockAddressGenerator.getInstance() : ChineseAddressGenerator.getInstance();
                break;
            case NAME:
                generator = ChineseNameGenerator.getInstance();
                break;
            case DFT:
                // random copy existed string
                if (isSinglePrimaryKey) {
                    log.error("Do not support for such single primary key.");
                    return null;
                }
                SecureRandom secureRandom = new SecureRandom(Long.toString(new Date().getTime()).getBytes(StandardCharsets.UTF_8));
                for (int i = 0; i < num; ++i)
                    forgedText.add(material.get(secureRandom.nextInt(material.size())));
                break;
        }
        if (generator == null) return forgedText;
        for (int i = 0; i < num; ++i)
            forgedText.add(generator.generate());
        return forgedText;
    }

    private static int findMaxValueIndex(int[] array) {
        int maxPtr = 0;
        for (int ptr = 0; ptr < array.length; ++ptr) {
            maxPtr = Math.max(array[maxPtr], array[ptr]) == array[maxPtr] ? maxPtr : ptr;
        }

        return maxPtr;
    }

    public static boolean isIdNumber(String val) {
        if (val == null) return false;
        // 定义判别用户身份证号的正则表达式（15位或者18位，最后一位可以为字母）
        String regularExpression = "(^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$)|" +
                "(^[1-9]\\d{5}\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3}$)";
        return Pattern.compile(regularExpression).matcher(val).matches();
/*
        // 判断第18位校验值
        if (matches) {
            if (val.length() == 18) {
                try {
                    char[] charArray = val.toCharArray();
                    // 前十七位加权因子
                    int[] idCardWi = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
                    // 这是除以11后，可能产生的11位余数对应的验证码
                    String[] idCardY = {"1", "0", "X", "9", "8", "7", "6", "5", "4", "3", "2"};
                    int sum = 0;
                    for (int i = 0; i < idCardWi.length; i++) {
                        int current = Integer.parseInt(String.valueOf(charArray[i]));
                        int count = current * idCardWi[i];
                        sum += count;
                    }
                    char idCardLast = charArray[17];
                    int idCardMod = sum % 11;
                    return idCardY[idCardMod].equalsIgnoreCase(String.valueOf(idCardLast));
                } catch (Exception e) {
                    return false;
                }
            }
        }
        return matches;
*/
    }

    public static boolean isAddress(String val) {
        if (val == null) return false;
        String regex = "(?<province>[^省]+自治区|.*?省|.*?行政区|.*?市)(?<city>[^市]+自治州|.*?地区|.*?行政单位|.+盟|市辖区|.*?市|.*?县)(?<county>[^县]+县|.+区|.+市|.+旗|.+海域|.+岛)?(?<town>[^区]+区|.+镇)?(?<village>.*)";
        Matcher m = Pattern.compile(regex).matcher(val);
        return m.find();
    }

    public static boolean isName(String val) {
        if (val == null) return false;
        return Pattern.compile("^[\\u4e00-\\u9fa5]{2,4}$").matcher(val).matches();
    }

    public static boolean isPhoneNumber(String val) {
        if (val == null) return false;
        Pattern p1 = Pattern.compile("^([5-6]|[8-9])\\d{7}$"); // HK
        Pattern p2 = Pattern.compile("^((13[0-9])|(14[0-1,4-9])|(15[0-3,5-9])|(16[2,5-7])|(17[0-8])|(18[0-9])|(19[0-3,5-9]))\\\\d{8}$");         // 验证没有区号的
        return p1.matcher(val).matches() || p2.matcher(val).matches();
    }

}
