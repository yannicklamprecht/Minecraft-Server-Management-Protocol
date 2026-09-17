package com.github.yannicklamprecht.mc.management.transport;

public final class JsonRpcException extends RuntimeException {
    private final JsonRpcError error;

    public JsonRpcException(JsonRpcError error) {
        super("JSON-RPC error " + error.code() + ": " + error.message());
        this.error = error;
    }

    public JsonRpcError error() { return error; }
}
