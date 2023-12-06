package cn.ac.iie.pkcgroup.dws.core.db.processor.text.punc;

public class PunctuationUtils {
    static boolean isEnPunc(char ch) {
        if (0x21 <= ch && ch <= 0x22) return true;
        if (ch == 0x27 || ch == 0x2C) return true;
        if (ch == 0x2E || ch == 0x3A) return true;
        return ch == 0x3B || ch == 0x3F;
    }

    static boolean isCjkPunc(char ch) {
        if (0x3001 <= ch && ch <= 0x3003) return true;
        return 0x301D <= ch && ch <= 0x301F;
    }

    public static boolean isPunctuation(char ch) {
        if (isCjkPunc(ch)) return true;
        if (isEnPunc(ch)) return true;

        if (0x2018 <= ch && ch <= 0x201F) return true;
        if (ch == 0xFF01 || ch == 0xFF02) return true;
        if (ch == 0xFF07 || ch == 0xFF0C) return true;
        if (ch == 0xFF1A || ch == 0xFF1B) return true;
        if (ch == 0xFF1F || ch == 0xFF61) return true;
        if (ch == 0xFF0E) return true;
        return ch == 0xFF65;
    }

}
