package com.example.msmp.plugin.generator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.palantir.javapoet.*;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class OpenRpcCodeGenerator {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ClassName clientClass;

    public OpenRpcCodeGenerator(String clientClassName) {
        if (clientClassName == null || clientClassName.isBlank()) {
            clientClassName = "com.example.msmp.transport.MinecraftManagementClient";
        }
        int lastDot = clientClassName.lastIndexOf('.');
        if (lastDot > 0) {
            this.clientClass = ClassName.get(clientClassName.substring(0, lastDot), clientClassName.substring(lastDot + 1));
        } else {
            this.clientClass = ClassName.get("", clientClassName);
        }
    }

    public void generateAll(Path schemaRoot, Path output, String basePackage) throws IOException {
        Files.createDirectories(output);
        if (!Files.exists(schemaRoot)) {
            Files.createDirectories(schemaRoot);
            return;
        }

        try (var versions = Files.list(schemaRoot)) {
            for (Path versionDir : versions.filter(Files::isDirectory).sorted().toList()) {
                Path schema = versionDir.resolve("openrpc.json");
                if (!Files.exists(schema)) schema = versionDir.resolve("json-rpc-api-schema.json");
                if (!Files.exists(schema)) continue;
                generate(schema, versionDir.getFileName().toString(), output, basePackage);
            }
        }
    }

    public void generate(Path schemaFile, String version, Path output, String basePackage) throws IOException {
        Map<String, Object> root = MAPPER.readValue(schemaFile.toFile(), new TypeReference<>() {});
        String versionPackage = "v" + version.replaceAll("[^A-Za-z0-9_]", "_");
        String pkg = basePackage + "." + versionPackage;
        String dtoPkg = pkg + ".dto";

        Map<String, Object> info = map(root.get("info"));
        String title = str(info.getOrDefault("title", "Minecraft Server JSON-RPC"));

        Map<String, Object> components = map(root.get("components"));
        Map<String, Object> schemas = map(components.get("schemas"));
        for (var entry : schemas.entrySet()) {
            generateSchema(output, dtoPkg, entry.getKey(), map(entry.getValue()));
        }

        List<Map<String, Object>> methods = listOfMaps(root.get("methods"));
        generateProtocolInfo(output, pkg, version, title);
        generateApi(output, pkg, dtoPkg, version, methods);
        generateNotifications(output, pkg, dtoPkg, version, methods);
    }

    private void generateSchema(Path output, String pkg, String rawName, Map<String, Object> schema) throws IOException {
        String name = javaTypeName(rawName);
        List<Object> enumValues = list(schema.get("enum"));
        if (!enumValues.isEmpty()) {
            TypeSpec.Builder enumBuilder = TypeSpec.enumBuilder(name)
                    .addModifiers(Modifier.PUBLIC);

            for (Object val : enumValues) {
                String valStr = String.valueOf(val);
                String constName = enumConstant(valStr);
                enumBuilder.addEnumConstant(constName, TypeSpec.anonymousClassBuilder("$S", valStr)
                        .addAnnotation(AnnotationSpec.builder(JsonProperty.class)
                                .addMember("value", "$S", valStr)
                                .build())
                        .build());
            }

            enumBuilder.addField(FieldSpec.builder(String.class, "value", Modifier.PRIVATE, Modifier.FINAL).build());

            enumBuilder.addMethod(MethodSpec.constructorBuilder()
                    .addParameter(String.class, "value")
                    .addStatement("this.value = value")
                    .build());

            enumBuilder.addMethod(MethodSpec.methodBuilder("getValue")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(String.class)
                    .addAnnotation(JsonValue.class)
                    .addStatement("return this.value")
                    .build());

            enumBuilder.addMethod(MethodSpec.methodBuilder("value")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(String.class)
                    .addStatement("return this.value")
                    .build());

            JavaFile.builder(pkg, enumBuilder.build())
                    .indent("    ")
                    .build()
                    .writeTo(output);
            return;
        }

        Map<String, Object> properties = map(schema.get("properties"));
        MethodSpec.Builder ctorBuilder = MethodSpec.constructorBuilder();

        if (properties.isEmpty()) {
            TypeName valueType = resolveTypeName(schema, pkg);
            if (valueType.equals(ClassName.get(pkg, name))) {
                valueType = ClassName.get(Object.class);
            }
            ctorBuilder.addParameter(ParameterSpec.builder(valueType, "value")
                    .addAnnotation(AnnotationSpec.builder(JsonProperty.class)
                            .addMember("value", "$S", "value")
                            .build())
                    .build());
        } else {
            for (var p : properties.entrySet()) {
                String propName = p.getKey();
                TypeName propType = resolveTypeName(map(p.getValue()), pkg);
                String fieldName = javaIdentifier(propName);

                ctorBuilder.addParameter(ParameterSpec.builder(propType, fieldName)
                        .addAnnotation(AnnotationSpec.builder(JsonProperty.class)
                                .addMember("value", "$S", propName)
                                .build())
                        .build());
            }
        }

        TypeSpec recordSpec = TypeSpec.recordBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(JsonIgnoreProperties.class)
                        .addMember("ignoreUnknown", "true")
                        .build())
                .recordConstructor(ctorBuilder.build())
                .build();

        JavaFile.builder(pkg, recordSpec)
                .indent("    ")
                .build()
                .writeTo(output);
    }

    private void generateProtocolInfo(Path output, String pkg, String version, String title) throws IOException {
        TypeSpec protocolInfo = TypeSpec.classBuilder("ProtocolInfo")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addField(FieldSpec.builder(String.class, "VERSION", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$S", version)
                        .build())
                .addField(FieldSpec.builder(String.class, "TITLE", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$S", title)
                        .build())
                .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build())
                .build();

        JavaFile.builder(pkg, protocolInfo)
                .indent("    ")
                .build()
                .writeTo(output);
    }

    private void generateApi(Path output, String pkg, String dtoPkg, String version, List<Map<String, Object>> methods) throws IOException {
        TypeSpec.Builder apiBuilder = TypeSpec.classBuilder("MinecraftManagementApi")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc("Typed JSON-RPC API facade for Minecraft Management Protocol v$L.\n", version)
                .addField(FieldSpec.builder(clientClass, "client", Modifier.PRIVATE, Modifier.FINAL).build())
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(clientClass, "client")
                        .addStatement("this.client = client")
                        .build())
                .addMethod(MethodSpec.methodBuilder("client")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(clientClass)
                        .addStatement("return client")
                        .build());

        for (Map<String, Object> method : methods) {
            String rpcName = str(method.get("name"));
            if (rpcName.isBlank() || isNotification(rpcName)) continue;

            String description = str(method.get("description"));
            List<Map<String, Object>> params = listOfMaps(method.get("params"));
            Map<String, Object> result = map(method.get("result"));
            TypeName resultType = resolveTypeName(map(result.get("schema")), dtoPkg);

            TypeName completableFutureType = ParameterizedTypeName.get(
                    ClassName.get(CompletableFuture.class),
                    resultType
            );

            String javaName = methodName(rpcName);
            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(javaName)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(completableFutureType);

            if (!description.isBlank()) {
                methodBuilder.addJavadoc("$L\n", description);
            }

            List<String> mapEntries = new ArrayList<>();
            int i = 0;
            for (Map<String, Object> p : params) {
                String rawParamName = Optional.ofNullable(str(p.get("name"))).filter(s -> !s.isBlank()).orElse("arg" + i);
                String pn = javaIdentifier(rawParamName);
                TypeName pType = resolveTypeName(map(p.get("schema")), dtoPkg);

                methodBuilder.addParameter(pType, pn);
                mapEntries.add("\"" + esc(rawParamName) + "\", " + pn);
                i++;
            }

            TypeName typeReferenceType = ParameterizedTypeName.get(
                    ClassName.get(TypeReference.class),
                    resultType
            );

            if (mapEntries.isEmpty()) {
                methodBuilder.addStatement("return client.call($S, new $T() {})",
                        rpcName, typeReferenceType);
            } else if (mapEntries.size() <= 10) {
                methodBuilder.addStatement("return client.call($S, $T.of(" + String.join(", ", mapEntries) + "), new $T() {})",
                        rpcName, Map.class, typeReferenceType);
            } else {
                String entriesStr = mapEntries.stream()
                        .map(e -> "Map.entry(" + e + ")")
                        .collect(Collectors.joining(", "));
                methodBuilder.addStatement("return client.call($S, $T.ofEntries(" + entriesStr + "), new $T() {})",
                        rpcName, Map.class, typeReferenceType);
            }

            apiBuilder.addMethod(methodBuilder.build());
        }

        JavaFile.builder(pkg, apiBuilder.build())
                .indent("    ")
                .build()
                .writeTo(output);
    }

    private void generateNotifications(Path output, String pkg, String dtoPkg, String version, List<Map<String, Object>> methods) throws IOException {
        TypeSpec.Builder notifBuilder = TypeSpec.classBuilder("MinecraftManagementNotifications")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc("Typed notification listeners for Minecraft Management Protocol v$L.\n", version)
                .addField(FieldSpec.builder(clientClass, "client", Modifier.PRIVATE, Modifier.FINAL).build())
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(clientClass, "client")
                        .addStatement("this.client = client")
                        .build())
                .addMethod(MethodSpec.methodBuilder("client")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(clientClass)
                        .addStatement("return client")
                        .build());

        for (Map<String, Object> method : methods) {
            String rpcName = str(method.get("name"));
            if (!isNotification(rpcName)) continue;

            String description = str(method.get("description"));
            List<Map<String, Object>> params = listOfMaps(method.get("params"));
            String handlerName = "on" + javaTypeName(methodName(rpcName));

            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(handlerName)
                    .addModifiers(Modifier.PUBLIC);

            if (!description.isBlank()) {
                methodBuilder.addJavadoc("$L\n", description);
            }

            if (params.isEmpty()) {
                methodBuilder.addParameter(Runnable.class, "listener")
                        .addStatement("client.registerNotification($S, listener)", rpcName);
            } else if (params.size() == 1) {
                Map<String, Object> firstParam = params.get(0);
                String rawParamName = str(firstParam.get("name"));
                TypeName paramType = resolveTypeName(map(firstParam.get("schema")), dtoPkg);

                TypeName consumerType = ParameterizedTypeName.get(ClassName.get(Consumer.class), paramType);
                TypeName typeRefType = ParameterizedTypeName.get(ClassName.get(TypeReference.class), paramType);

                methodBuilder.addParameter(consumerType, "listener")
                        .addStatement("client.registerNotificationProperty($S, $S, new $T() {}, listener)",
                                rpcName, rawParamName, typeRefType);
            } else {
                TypeName mapType = ParameterizedTypeName.get(
                        ClassName.get(Map.class),
                        ClassName.get(String.class),
                        ClassName.get(Object.class)
                );
                TypeName consumerType = ParameterizedTypeName.get(ClassName.get(Consumer.class), mapType);
                TypeName typeRefType = ParameterizedTypeName.get(ClassName.get(TypeReference.class), mapType);

                methodBuilder.addParameter(consumerType, "listener")
                        .addStatement("client.registerNotification($S, new $T() {}, listener)", rpcName, typeRefType);
            }

            notifBuilder.addMethod(methodBuilder.build());
        }

        JavaFile.builder(pkg, notifBuilder.build())
                .indent("    ")
                .build()
                .writeTo(output);
    }

    private static boolean isNotification(String name) {
        return name.contains("notification/") || name.startsWith("notification:") || name.contains(":notification/");
    }

    private static TypeName resolveTypeName(Map<String, Object> schema, String dtoPkg) {
        if (schema == null || schema.isEmpty()) return ClassName.get(Object.class);
        String ref = str(schema.get("$ref"));
        if (!ref.isBlank()) {
            String targetName = javaTypeName(ref.substring(ref.lastIndexOf('/') + 1));
            return ClassName.get(dtoPkg, targetName);
        }
        List<Object> oneOf = list(schema.get("oneOf"));
        if (!oneOf.isEmpty()) return ClassName.get(Object.class);
        List<Object> anyOf = list(schema.get("anyOf"));
        if (!anyOf.isEmpty()) return ClassName.get(Object.class);

        Object typeObj = schema.get("type");
        if (typeObj instanceof List<?>) {
            return ClassName.get(Object.class);
        }
        String type = str(typeObj);
        return switch (type) {
            case "string" -> "uuid".equals(str(schema.get("format"))) ? ClassName.get(UUID.class) : ClassName.get(String.class);
            case "integer" -> ClassName.get(Long.class);
            case "number" -> ClassName.get(BigDecimal.class);
            case "boolean" -> ClassName.get(Boolean.class);
            case "array" -> ParameterizedTypeName.get(ClassName.get(List.class), resolveTypeName(map(schema.get("items")), dtoPkg));
            case "object" -> ClassName.get(Object.class);
            case "null" -> ClassName.get(Void.class);
            default -> ClassName.get(Object.class);
        };
    }

    private static String methodName(String rpc) {
        String cleaned = rpc;
        if (cleaned.startsWith("minecraft:notification/")) {
            cleaned = cleaned.substring("minecraft:notification/".length());
        } else if (cleaned.startsWith("minecraft:")) {
            cleaned = cleaned.substring("minecraft:".length());
        } else if (cleaned.startsWith("notification/")) {
            cleaned = cleaned.substring("notification/".length());
        }
        String[] parts = cleaned.split("[^A-Za-z0-9]+");
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isBlank()) continue;
            String p = parts[i];
            if (b.isEmpty()) b.append(Character.toLowerCase(p.charAt(0))).append(p.substring(1));
            else b.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return javaIdentifier(b.isEmpty() ? "call" : b.toString());
    }

    private static String javaTypeName(String raw) {
        String[] parts = raw.split("[^A-Za-z0-9]+");
        StringBuilder b = new StringBuilder();
        for (String p : parts) if (!p.isBlank()) b.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        return b.isEmpty() ? "Unnamed" : b.toString();
    }

    private static String javaIdentifier(String raw) {
        String s = raw.replaceAll("[^A-Za-z0-9_$]", "_");
        if (s.isEmpty()) s = "value";
        if (!Character.isJavaIdentifierStart(s.charAt(0))) s = "_" + s;
        Set<String> keywords = Set.of("class", "record", "interface", "enum", "public", "private", "protected", "static", "final", "void", "int", "long", "double", "boolean", "new", "return", "default", "switch", "case", "package", "import", "this", "super", "extends", "implements", "throws", "throw", "try", "catch", "finally", "if", "else", "for", "while", "do", "break", "continue", "instanceof", "var", "yield", "sealed", "permits", "non-sealed");
        return keywords.contains(s) ? s + "Value" : s;
    }

    private static String enumConstant(String raw) {
        String s = raw.replaceAll("[^A-Za-z0-9]+", "_").replaceAll("^_+|_+$", "").toUpperCase(Locale.ROOT);
        if (s.isEmpty()) s = "UNKNOWN";
        if (!Character.isJavaIdentifierStart(s.charAt(0))) s = "_" + s;
        return s;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object o) {
        return o instanceof List<?> l ? (List<Object>) l : List.of();
    }

    private static List<Map<String, Object>> listOfMaps(Object o) {
        return list(o).stream().filter(Map.class::isInstance).map(OpenRpcCodeGenerator::map).toList();
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
