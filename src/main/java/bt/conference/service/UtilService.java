package bt.conference.service;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;
import java.util.TimeZone;

@Service
public class UtilService {
    private static final BigInteger N = BigInteger.TEN.pow(10); // 10^10
    private static final String HMAC_ALGO = "HmacSHA256";
    private static final String SECRET = "MySecretKey12345";
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom random = new SecureRandom();
    private static SecretKeySpec getKey() {
        return new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "AES");
    }
    public static   Date toUtc(Date date) {
        // Get the instant in milliseconds
        long time = date.getTime();

        // Offset of default timezone in milliseconds
        TimeZone tz = TimeZone.getDefault();
        int offset = tz.getOffset(time);

        // Subtract offset to get UTC
        return new Date(time - offset);
    }
    public static String encodeUsingSecretOnly(long id) {
        BigInteger mask = deriveMaskFromSecretOnly(); // independent of id
        BigInteger codeBI = BigInteger.valueOf(id).add(mask).mod(N);
        return String.format("%010d", codeBI.longValue());
    }

    private static BigInteger deriveMaskFromSecretOnly() {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(getKey());
            byte[] h = mac.doFinal("GLOBAL-MASK".getBytes(StandardCharsets.UTF_8));
            return new BigInteger(1, h).mod(N);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static long decodeUsingSecretOnly(String code10) {
        BigInteger codeBI = new BigInteger(code10);
        BigInteger mask = deriveMaskFromSecretOnly();
        BigInteger idBI = codeBI.subtract(mask).mod(N);
        return idBI.longValue();
    }

    public static String generatePassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
}
