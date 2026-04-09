package com.taskflow.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class OtpService {
    private final Map<String, OtpData> otpCache = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public String generateOtp(String email) {
        String otp = String.format("%06d", random.nextInt(1000000));
        otpCache.put(email, new OtpData(otp, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5)));
        System.out.println("[OTP SERVICE] Generated OTP for " + email + ": " + otp);
        return otp;
    }

    public boolean verifyOtp(String email, String otp) {
        System.out.println("[OTP SERVICE] Verifying OTP for " + email);
        OtpData data = otpCache.get(email);
        if (data == null) {
            System.out.println("[OTP SERVICE] No OTP found for email");
            return false;
        }
        if (System.currentTimeMillis() > data.expiryTime) {
            System.out.println("[OTP SERVICE] OTP expired");
            otpCache.remove(email);
            return false;
        }
        boolean isValid = data.otp.equals(otp);
        if (isValid) {
            System.out.println("[OTP SERVICE] OTP verified successfully");
            otpCache.remove(email);
        } else {
            System.out.println("[OTP SERVICE] OTP mismatch");
        }
        return isValid;
    }

    private static class OtpData {
        String otp;
        long expiryTime;

        OtpData(String otp, long expiryTime) {
            this.otp = otp;
            this.expiryTime = expiryTime;
        }
    }
}
