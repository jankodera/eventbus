package com.example.eventbus.dto;

public class ConsumptionResult {

    private final boolean success;
    private final Object resultData;
    private final boolean retryable;
    private final String errorMessage;

    private ConsumptionResult(boolean success, Object resultData, boolean retryable, String errorMessage) {
        this.success = success;
        this.resultData = resultData;
        this.retryable = retryable;
        this.errorMessage = errorMessage;
    }

    public static ConsumptionResult success(Object resultData) {
        return new ConsumptionResult(true, resultData, false, null);
    }

    public static ConsumptionResult failure(String errorMessage, boolean retryable) {
        return new ConsumptionResult(false, null, retryable, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public Object getResultData() {
        return resultData;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
