package com.github.yannicklamprecht.mc.management.transport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JsonRpcMessageHeader(String jsonrpc, Long id, String method) {}
