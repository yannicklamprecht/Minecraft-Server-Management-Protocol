package com.example.msmp.transport;

public record JsonRpcResponse<T>(String jsonrpc, Long id, T result, JsonRpcError error) {
    public boolean isError() { return error != null; }
}
