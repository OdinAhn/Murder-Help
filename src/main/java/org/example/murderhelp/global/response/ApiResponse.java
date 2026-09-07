package org.example.murderhelp.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.example.murderhelp.global.error.ErrorCode;


@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

        private static final String SUCCESS_CODE = "SUCCESS";

        private final String code;
        private final String message;
        private final T data;

        private ApiResponse(String code, String message, T data) {
            this.code = code;
            this.message = message;
            this.data = data;
        }

        public static <T> ApiResponse<T> ok(T data) {
            return new ApiResponse<>(SUCCESS_CODE, null, data);
        }

        public static ApiResponse<Void> ok() {
            return new ApiResponse<>(SUCCESS_CODE, null, null);
        }

        public static ApiResponse<Void> error(ErrorCode errorCode) {
            return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null);
        }

        public static ApiResponse<Void> error(ErrorCode errorCode, String message) {
            return new ApiResponse<>(errorCode.getCode(), message, null);
        }

        public static ApiResponse<Void> error(String code, String message) {
            return new ApiResponse<>(code, message, null);
        }
}


   /* =====================================
   // 강의 우선을 위한 주석 처리 입니다(AhnSangYun, dev.JeongMin)

    private final boolean success;
    private final int code;
    private final String message;
    private final String errorCode;
    private final T data;

    // 성공 응답 (데이터 포함)
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, 200, "요청에 성공하였습니다.", null, data);
    }
    
    // 성공 응답 (메시지 + 데이터 포함)
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, 200, message, null, data);
    }

    // 성공 응답 (데이터 없음)
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, 200, "요청에 성공하였습니다.", null, null);
    }

    // 실패 응답
    public static <T> ApiResponse<T> fail(int code, String errorCode, String message) {
        return new ApiResponse<>(false, code, message, errorCode, null);
    }


    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
 */