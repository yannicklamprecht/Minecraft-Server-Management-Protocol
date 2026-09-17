package com.github.yannicklamprecht.mc.management.transport;

public record JsonRpcResponse<T>(String jsonrpc, Long id, T result, JsonRpcError error) {
    public boolean isError() { return error != null; }
}
