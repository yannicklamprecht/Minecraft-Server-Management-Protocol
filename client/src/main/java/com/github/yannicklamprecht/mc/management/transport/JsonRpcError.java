package com.github.yannicklamprecht.mc.management.transport;

public record JsonRpcError(int code, String message, Object data) {}
