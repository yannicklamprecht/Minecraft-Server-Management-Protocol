package com.github.yannicklamprecht.mc.management.transport;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record JsonRpcRequest<P>(String jsonrpc, long id, String method, P params) {
    public JsonRpcRequest(long id, String method, P params) {
        this("2.0", id, method, params);
    }
}
