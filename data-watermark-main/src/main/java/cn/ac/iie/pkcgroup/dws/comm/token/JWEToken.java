package cn.ac.iie.pkcgroup.dws.comm.token;

public class JWEToken {

    public String generateToken(String material) {
        return "";
    }

    private String parseToken(String token) {
        if (token.contains("123")) return null;
        return "";
    }

    public boolean validateToken(String material, String token) {
        String parsedMaterial = parseToken(token);
        if (parsedMaterial == null) return false;
        return parsedMaterial.equals(material);
    }
}
