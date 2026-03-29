package com.example.myapplication.network;

import android.content.Context;
import android.widget.Toast;

import org.json.JSONObject;

import retrofit2.Response;

/**
 * ApiErrorHelper — Parse message lỗi từ backend response.
 *
 * Backend (GlobalExceptionHandler) trả JSON dạng:
 *   {"timestamp": "...", "status": 400, "message": "Lỗi cụ thể..."}
 *
 * Class này tự parse field "message" và hiện Toast cho user.
 *
 * Cách dùng (1 dòng thay vì try-catch ở mỗi callback):
 *   ApiErrorHelper.show(context, response, "Thao tác thất bại");
 *
 * Hoặc chỉ lấy message (không hiện Toast):
 *   String msg = ApiErrorHelper.parse(response, "Thao tác thất bại");
 */
public class ApiErrorHelper {

    /**
     * Parse message lỗi từ error body.
     * Nếu không parse được → dùng fallback + mã lỗi HTTP.
     */
    public static String parse(Response<?> response, String fallback) {
        try {
            if (response.errorBody() != null) {
                String body = response.errorBody().string();
                JSONObject json = new JSONObject(body);
                if (json.has("message")) {
                    String msg = json.getString("message");
                    if (msg != null && !msg.isEmpty()) return msg;
                }
            }
        } catch (Exception ignored) {}

        // Fallback với mã lỗi HTTP có ý nghĩa
        int code = response.code();
        switch (code) {
            case 400: return fallback + " — dữ liệu không hợp lệ";
            case 401: return "Phiên đăng nhập hết hạn, vui lòng đăng nhập lại";
            case 403: return "Bạn không có quyền thực hiện thao tác này";
            case 404: return "Không tìm thấy dữ liệu";
            case 409: return fallback + " — dữ liệu bị trùng lặp";
            case 500: return "Lỗi hệ thống, vui lòng thử lại sau";
            default:  return fallback + " (mã lỗi: " + code + ")";
        }
    }

    /**
     * Parse + hiện Toast LONG luôn. Dùng cho đa số trường hợp.
     */
    public static void show(Context context, Response<?> response, String fallback) {
        Toast.makeText(context, parse(response, fallback), Toast.LENGTH_LONG).show();
    }

    /**
     * Parse + hiện Toast SHORT (cho lỗi nhẹ).
     */
    public static void showShort(Context context, Response<?> response, String fallback) {
        Toast.makeText(context, parse(response, fallback), Toast.LENGTH_SHORT).show();
    }
}