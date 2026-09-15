package com.example.msmp.codegen;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public final class MsmpOpenRpcGenerator {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("Usage: <schema-root> <output-dir> <base-package>");
        Path schemaRoot = Path.of(args[0]);
        Path output = Path.of(args[1]);
        String basePackage = args[2];
        Files.createDirectories(output);
        if (!Files.exists(schemaRoot)) return;

        try (var versions = Files.list(schemaRoot)) {
            for (Path versionDir : versions.filter(Files::isDirectory).sorted().toList()) {
                Path schema = versionDir.resolve("openrpc.json");
                if (!Files.exists(schema)) schema = versionDir.resolve("json-rpc-api-schema.json");
                if (!Files.exists(schema)) continue;
                generate(schema, versionDir.getFileName().toString(), output, basePackage);
            }
        }
    }

    private static void generate(Path schemaFile, String version, Path output, String basePackage) throws IOException {
        Map<String, Object> root = MAPPER.readValue(schemaFile.toFile(), new TypeReference<>() {});
        String versionPackage = "v" + version.replaceAll("[^A-Za-z0-9_]", "_");
        String pkg = basePackage + "." + versionPackage;
        Path pkgDir = output.resolve(pkg.replace('.', '/'));
        Path dtoDir = pkgDir.resolve("dto");
        Files.createDirectories(dtoDir);

        Map<String, Object> components = map(root.get("components"));
        Map<String, Object> schemas = map(components.get("schemas"));
        for (var entry : schemas.entrySet()) generateSchema(dtoDir, pkg + ".dto", entry.getKey(), map(entry.getValue()));

        List<Map<String, Object>> methods = listOfMaps(root.get("methods"));
        generateProtocolInfo(pkgDir, pkg, version);
        generateApi(pkgDir, pkg, methods);
        generateNotifications(pkgDir, pkg, methods);
    }

    private static void generateSchema(Path dtoDir, String pkg, String rawName, Map<String, Object> schema) throws IOException {
        String name = javaTypeName(rawName);
        List<Object> enumValues = list(schema.get("enum"));
        if (!enumValues.isEmpty()) {
            String constants = enumValues.stream().map(String::valueOf).map(MsmpOpenRpcGenerator::enumConstant).collect(Collectors.joining(",\n    "));
            write(dtoDir.resolve(name + ".java"), "package " + pkg + ";\n\npublic enum " + name + " {\n    " + constants + "\n}\n");
            return;
        }

        Map<String, Object> properties = map(schema.get("properties"));
        if (properties.isEmpty()) {
            // A named non-object schema gets a small value record rather than falling back to JsonNode.
            String valueType = resolveType(schema);
            if (valueType.equals(name)) valueType = "Object";
            write(dtoDir.resolve(name + ".java"), "package " + pkg + ";\n\npublic record " + name + "(" + valueType + " value) {}\n");
            return;
        }

        List<String> fields = new ArrayList<>();
        for (var p : properties.entrySet()) fields.add(resolveType(map(p.getValue())) + " " + javaIdentifier(p.getKey()));
        String body = "package " + pkg + ";\n\n" +
                "import java.util.List;\n\n" +
                "public record " + name + "(\n        " + String.join(",\n        ", fields) + "\n) {}\n";
        write(dtoDir.resolve(name + ".java"), body);
    }

    private static void generateProtocolInfo(Path dir, String pkg, String version) throws IOException {
        write(dir.resolve("ProtocolInfo.java"), "package " + pkg + ";\n\npublic final class ProtocolInfo {\n    public static final String VERSION = \"" + esc(version) + "\";\n    private ProtocolInfo() {}\n}\n");
    }

    private static void generateApi(Path dir, String pkg, List<Map<String, Object>> methods) throws IOException {
        StringBuilder b = new StringBuilder();
        b.append("package ").append(pkg).append(";\n\n")
         .append("import com.example.msmp.transport.MinecraftManagementClient;\n")
         .append("import com.fasterxml.jackson.core.type.TypeReference;\n")
         .append("import java.util.List;\n")
         .append("import java.util.concurrent.CompletableFuture;\n")
         .append("import ").append(pkg).append(".dto.*;\n\n")
         .append("public final class MinecraftManagementApi {\n")
         .append("    private final MinecraftManagementClient client;\n")
         .append("    public MinecraftManagementApi(MinecraftManagementClient client) { this.client = client; }\n\n");

        for (Map<String, Object> method : methods) {
            String rpcName = str(method.get("name"));
            if (rpcName.isBlank() || isNotification(rpcName)) continue;
            appendMethod(b, rpcName, method);
        }
        b.append("}\n");
        write(dir.resolve("MinecraftManagementApi.java"), b.toString());
    }

    private static void appendMethod(StringBuilder b, String rpcName, Map<String, Object> method) {
        List<Map<String, Object>> params = listOfMaps(method.get("params"));
        Map<String, Object> result = map(method.get("result"));
        String resultType = resolveType(map(result.get("schema")));
        if (resultType.equals("Object") && result.containsKey("schema")) resultType = resolveType(map(result.get("schema")));

        List<String> sig = new ArrayList<>();
        List<String> args = new ArrayList<>();
        int i = 0;
        for (Map<String, Object> p : params) {
            String pn = javaIdentifier(Optional.ofNullable(str(p.get("name"))).filter(s -> !s.isBlank()).orElse("arg" + i));
            sig.add(resolveType(map(p.get("schema"))) + " " + pn);
            args.add(pn);
            i++;
        }
        String javaName = methodName(rpcName);
        b.append("    public CompletableFuture<").append(resultType).append("> ").append(javaName).append("(")
         .append(String.join(", ", sig)).append(") {\n")
         .append("        return client.call(\"").append(esc(rpcName)).append("\", List.of(").append(String.join(", ", args)).append("), new TypeReference<").append(resultType).append(">() {});\n")
         .append("    }\n\n");
    }

    private static void generateNotifications(Path dir, String pkg, List<Map<String, Object>> methods) throws IOException {
        StringBuilder b = new StringBuilder();
        b.append("package ").append(pkg).append(";\n\n")
         .append("import com.example.msmp.transport.MinecraftManagementClient;\n")
         .append("import com.fasterxml.jackson.core.type.TypeReference;\n")
         .append("import java.util.List;\n")
         .append("import java.util.function.Consumer;\n")
         .append("import ").append(pkg).append(".dto.*;\n\n")
         .append("public final class MinecraftManagementNotifications {\n")
         .append("    private final MinecraftManagementClient client;\n")
         .append("    public MinecraftManagementNotifications(MinecraftManagementClient client) { this.client = client; }\n\n");

        for (Map<String, Object> method : methods) {
            String rpcName = str(method.get("name"));
            if (!isNotification(rpcName)) continue;
            List<Map<String, Object>> params = listOfMaps(method.get("params"));
            String paramType;
            if (params.isEmpty()) paramType = "Object";
            else if (params.size() == 1) paramType = resolveType(map(params.get(0).get("schema")));
            else paramType = "List<Object>";
            b.append("    public void on").append(javaTypeName(methodName(rpcName))).append("(Consumer<").append(paramType).append("> listener) {\n")
             .append("        client.registerNotification(\"").append(esc(rpcName)).append("\", new TypeReference<").append(paramType).append(">() {}, listener);\n")
             .append("    }\n\n");
        }
        b.append("}\n");
        write(dir.resolve("MinecraftManagementNotifications.java"), b.toString());
    }

    private static boolean isNotification(String name) {
        return name.contains("notification/") || name.startsWith("notification:") || name.contains(":notification/");
    }

    private static String resolveType(Map<String, Object> schema) {
        if (schema.isEmpty()) return "Object";
        String ref = str(schema.get("$ref"));
        if (!ref.isBlank()) return javaTypeName(ref.substring(ref.lastIndexOf('/') + 1));
        List<Object> oneOf = list(schema.get("oneOf"));
        if (!oneOf.isEmpty()) return "Object";
        List<Object> anyOf = list(schema.get("anyOf"));
        if (!anyOf.isEmpty()) return "Object";
        String type = str(schema.get("type"));
        return switch (type) {
            case "string" -> "uuid".equals(str(schema.get("format"))) ? "java.util.UUID" : "String";
            case "integer" -> "Long";
            case "number" -> "java.math.BigDecimal";
            case "boolean" -> "Boolean";
            case "array" -> "List<" + resolveType(map(schema.get("items"))) + ">";
            case "object" -> "Object";
            case "null" -> "Void";
            default -> "Object";
        };
    }

    private static String methodName(String rpc) {
        String[] parts = rpc.split("[^A-Za-z0-9]+");
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

    private static void write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }

    @SuppressWarnings("unchecked") private static Map<String, Object> map(Object o) { return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of(); }
    @SuppressWarnings("unchecked") private static List<Object> list(Object o) { return o instanceof List<?> l ? (List<Object>) l : List.of(); }
    private static List<Map<String, Object>> listOfMaps(Object o) { return list(o).stream().filter(Map.class::isInstance).map(MsmpOpenRpcGenerator::map).toList(); }
    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }
    private static String esc(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }
}
