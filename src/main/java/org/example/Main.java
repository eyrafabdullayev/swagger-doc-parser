package org.example;

import org.example.constraints.Constant;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class Main {

    public static void main(String[] args) throws IOException {
        merge();
    }

    static void merge() throws IOException {
        doMerge(getFileNames());
    }

    static void doMerge(List<String> fileNames) throws IOException {
        Yaml yaml = new Yaml();

        Map<String, Object> resultObj = null;
        for (String fileName : fileNames) {
            Map<String, Object> obj = load(yaml, fileName);
            if (resultObj == null) {
                resultObj = obj;
                continue;
            }
            comparePathObj(obj, resultObj);
            compareSchemasObj(obj, resultObj);
        }

        writeOutputToFile(convertMapToString(resultObj));
    }

    static void writeOutputToFile(String output) throws IOException {
        BufferedWriter writer = null;
        writer = new BufferedWriter(new FileWriter(Constant.RESULT_FILE_NAME));
        writer.write(output);
        writer.close();
    }

    static String convertMapToString(Map<String, Object> resultObj) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);

        Yaml yaml = new Yaml(options);
        return yaml.dump(resultObj);
    }

    @SuppressWarnings("unchecked")
    static void replace(List<?> list) {
        for (Object obj : list) {
            if (obj instanceof Map) {
                replace((Map<String, Object>) obj);
            }
        }
    }

    @SuppressWarnings("unchecked")
    static void replace(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map || value instanceof List) {
                if (value instanceof Map) {
                    checkAndReplaceContent(entry);
                    replace((Map<String, Object>) value);
                } else {
                    replace((List<?>) value);
                }
            } else if (value instanceof String) {
                String[] values = ((String) value).split(Constant.YAML_EXTENSION);
                String newValue = values.length > 1 ? values[1] : values[0];
                map.put(key, newValue);
            }
        }
    }

    @SuppressWarnings("unchecked")
    static void checkAndReplaceContent(Map.Entry<String, Object> parentEntry) {
        Map<String, Object> parentValue = (Map<String, Object>) parentEntry.getValue();
        if (Constant.CONTENT.equals(parentEntry.getKey())) {
            for (Map.Entry<String, Object> entry : parentValue.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (Constant.PATTERN.equals(key)) {
                    if (value instanceof Map) {
                        parentEntry.setValue(Map.entry(Constant.CONTENT,
                                Map.entry(Constant.CONTENT_TYPE, (Map<String, Object>) value)));
                    } else if (value instanceof String) {
                        parentEntry.setValue(Map.entry(Constant.CONTENT, Constant.CONTENT_TYPE));
                    }
                }
            }
        }
    }

    static List<String> getFileNames() {
        List<String> fileNames = new ArrayList<>();

        File folder = new File("/home/ubuntu/Workspace/teamprojects/swagger-documentation-parser/src/main/resources");
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null) return fileNames;

        for (File file : listOfFiles) {
            if (file.isFile()) {
                fileNames.add(file.getName());
            }
        }

        return fileNames;
    }

    @SuppressWarnings("unchecked")
    static void comparePathObj(Map<String, Object> obj, Map<String, Object> resultObj) {
        Map<String, Object> pathsObj = (Map<String, Object>) obj.get(Constant.PATHS_KEY);
        if (pathsObj == null) return;
        Map<String, Object> resultPathsObj = (Map<String, Object>) resultObj.get(Constant.PATHS_KEY);
        for (Map.Entry<String, Object> entry : pathsObj.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (!resultPathsObj.containsKey(key)) {
                replace((Map<String, Object>) value);
                resultPathsObj.put(key, value);
            }
        }
    }

    @SuppressWarnings("unchecked")
    static void compareSchemasObj(Map<String, Object> obj, Map<String, Object> resultObj) {
        Map<String, Object> componentsObj = (Map<String, Object>) obj.get(Constant.COMPONENTS_KEY);
        if (componentsObj == null) return;
        Map<String, Object> schemasObj = (Map<String, Object>) componentsObj.get(Constant.SCHEMAS_KEY);
        if (schemasObj == null) return;
        Map<String, Object> resultComponentsObj = (Map<String, Object>) resultObj.get(Constant.COMPONENTS_KEY);
        Map<String, Object> resultSchemasObj = (Map<String, Object>) resultComponentsObj.get(Constant.SCHEMAS_KEY);
        schemasObj.forEach((key, value) -> {
            if (!resultSchemasObj.containsKey(key)) {
                replace((Map<String, Object>) value);
                resultSchemasObj.put(key, value);
            }
        });
    }

    static Map<String, Object> load(Yaml yaml, String fileName) {
        InputStream inputStream = Main.class
                .getClassLoader()
                .getResourceAsStream(fileName);
        return yaml.load(inputStream);
    }
}