package com.example.myapplication.network;

import android.util.Base64;
import org.json.JSONObject;

/**
 * Tiện ích giải mã JWT token phía client để kiểm tra hạn (exp).
 * Không cần secret key — chỉ đọc payload (phần 2 của JWT).
 */
public class TokenUtils {

    /**
     * Kiểm tra xem token JWT đã hết hạn chưa.
     * @return true nếu token null, rỗng, sai định dạng, hoặc đã hết hạn.
     */
    public static boolean isTokenExpired(String token) {
        if (token == null || token.trim().isEmpty()) return true;

        try {
            // JWT gồm 3 phần: header.payload.signature
            String[] parts = token.split("\\.");
            if (parts.length < 2) return true;

            // Giải mã payload (phần thứ 2) từ Base64
            String payload = new String(
                    Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_WRAP),
                    "UTF-8"
            );
            JSONObject json = new JSONObject(payload);

            // Lấy thời gian hết hạn (exp là Unix timestamp tính bằng giây)
            long expSeconds = json.getLong("exp");
            long nowSeconds = System.currentTimeMillis() / 1000;

            return nowSeconds >= expSeconds;
        } catch (Exception e) {
            // Token sai định dạng → coi như hết hạn
            return true;
        }
    }
}
