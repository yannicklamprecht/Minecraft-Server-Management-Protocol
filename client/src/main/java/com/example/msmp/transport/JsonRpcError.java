package com.example.msmp.transport;

public record JsonRpcError(int code, String message, Object data) {}
