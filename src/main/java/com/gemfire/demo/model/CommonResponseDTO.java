package com.gemfire.demo.model;

public class CommonResponseDTO<T> {

    private boolean success;
    private T data;
    private String message;

    public CommonResponseDTO() {
    }

    public CommonResponseDTO(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }

    public static <T> CommonResponseDTO<T> ok(T data, String message) {
        return new CommonResponseDTO<>(true, data, message);
    }

    public static <T> CommonResponseDTO<T> ok(T data) {
        return new CommonResponseDTO<>(true, data, "Success");
    }

    public static <T> CommonResponseDTO<T> error(String message) {
        return new CommonResponseDTO<>(false, null, message);
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
