package com.example.msmp.transport;

public record JsonRpcNotification<P>(String jsonrpc, String method, P params) {}
