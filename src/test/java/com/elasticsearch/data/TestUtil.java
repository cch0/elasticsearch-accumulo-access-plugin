package com.elasticsearch.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class TestUtil {
  private static ObjectMapper objectMapper = new ObjectMapper();

  public static String readFileAsString(String filename) throws IOException {
    return new String(Files.readAllBytes(Paths.get(filename)));
  }

  public static Map<String, Object> readFileAsMap(String filename) throws IOException {
    return objectMapper.readValue(
        readFileAsString(filename), new TypeReference<Map<String, Object>>() {});
  }
}
