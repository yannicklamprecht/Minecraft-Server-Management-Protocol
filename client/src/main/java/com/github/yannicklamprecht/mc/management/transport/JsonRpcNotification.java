package com.github.yannicklamprecht.mc.management.transport;

public record JsonRpcNotification<P>(String jsonrpc, String method, P params) {}
