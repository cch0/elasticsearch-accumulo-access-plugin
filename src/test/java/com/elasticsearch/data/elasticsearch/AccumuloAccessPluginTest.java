package com.elasticsearch.data.elasticsearch;

import static com.elasticsearch.data.LabelExtractor.extract;
import static com.elasticsearch.data.TestUtil.readFileAsMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Stream;

import com.elasticsearch.data.LabelExtractor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class AccumuloAccessPluginTest {
  private static final Logger logger = Logger.getLogger(AccumuloAccessPluginTest.class.getName());
  private Map<String, Object> data;
  private String testSourcePath = "src/test/resources/";

  private static Stream<Arguments> scenarios() {
    return Stream.of(
        Arguments.of("sanction1.json", "sanction_programs", "SDGT"),
        Arguments.of("sanction1.json", "sanction_list_ids", "\"SDN List\""),
        Arguments.of("sanction1.json", "identity_documents.issuing_country", "Pakistan"),
        Arguments.of("sanction2.json", "identity_documents.document_name", "\"CHEN, Mei Hsiang\""),
        Arguments.of("sanction3.json", "identity_documents.document_name", "\"SUN, Sidong\""),
        Arguments.of("test_data1.json", "field1", "BLUE"),
        Arguments.of("test_data1.json", "field2", "BLUE|GREEN"),
        Arguments.of("test_data1.json", "field3", "BLUE"),
        Arguments.of("test_data1.json", "field4", "\"BL UE\"|\"GREEN, PINK\""),
        Arguments.of("test_data1.json", "field5.field51", "BLUE"),
        Arguments.of("test_data1.json", "field5.field52", "BLUE|GREEN"),
        Arguments.of("test_data1.json", "field5.field53", "\"BL UE\"|\"GREEN, PINK\""),
        Arguments.of("test_data1.json", "field5.field54.field541", "BLUE"),
        Arguments.of("test_data1.json", "field5.field54.field542", "BLUE|GREEN"),
        Arguments.of("test_data1.json", "field5.field54.field543.field5431", "BLUE"),
        Arguments.of("test_data1.json", "field6", "123"),
        Arguments.of("test_data1.json", "field7", "123.456"),
        Arguments.of("test_data1.json", "field8.field81", "123"),
        Arguments.of("test_data1.json", "field8.field82", "123.456"),
        Arguments.of("test_data1.json", "field8.field83", "123"),
        Arguments.of("test_data1.json", "field8.field84", "123.456"),
        Arguments.of("test_data1.json", "field8.field85", "123.456|789.0"),
        Arguments.of("test_data1.json", "field8.field86.field861", "123"),
        Arguments.of("test_data1.json", "field8.field86.field862", "123.456"),
        Arguments.of("test_data1.json", "field8.field86.field863", "123.456|789.0"),
        Arguments.of("test_data1.json", "field8.field86.field864.field8641", "123"),
        Arguments.of("test_data1.json", "field8.field86.field865", "123|456"),
        Arguments.of("test_data1.json", "field8.field86.field866", "123.456"),
        Arguments.of("test_data1.json", "field8.field87", "123|456"),
        Arguments.of("test_data1.json", "field9", "123|456"),
        Arguments.of("test_data1.json", "field10", "123"),
        Arguments.of("test_data1.json", "field11", "123.456"),
        Arguments.of("test_data1.json", "field12", "123.456|789.0"),
        Arguments.of(
            "test_data1.json", "field13", "123|789.0|BLUE|GREEN|\"BL UE\"|\"PINK, GREEN\""),
        Arguments.of(
            "test_data1.json",
            "field14.field141",
            "123|789.0|BLUE|GREEN|\"BL UE\"|\"PINK, GREEN\""),
        Arguments.of("test_data1.json", "field15", null),
        Arguments.of("test_data1.json", "field16", "BLUE_GREEN|BLUE#GREEN|BLUE\\GREEN|BLUE,GREEN|BLUE-GREEN|BLUE:GREEN|BLUE.GREEN|BLUE/GREEN"),
        Arguments.of("test_data1.json", "field17", "\"IFSR, SDGT\""),
        Arguments.of("test_data1.json", "field18", "\"IFSR SDGT\"")
    );
  }

  @ParameterizedTest
  @MethodSource("scenarios")
  public void verify(String filename, String field, String expected) throws IOException {
    this.data = readFileAsMap(testSourcePath + filename);

    Optional<String> actual = LabelExtractor.extract(this.data, field);
    boolean shouldExist = expected != null;

    logger.info(String.format("shouldExist? %b, expected: %s", shouldExist, expected));

    assertEquals(shouldExist, actual.isPresent());

    if (shouldExist) {
      assertEquals(expected, actual.get());
    } else {
      assertNull(expected);
    }
  }

  private static Stream<Arguments> splitOrScenarios() {
    return Stream.of(
        Arguments.of("test_data1.json", "field17", "IFSR|SDGT"),
        Arguments.of("test_data1.json", "field18", "\"IFSR SDGT\"")
    );
  }

  @ParameterizedTest
  @MethodSource("splitOrScenarios")
  public void splitOrScenarios(String filename, String field, String expected) throws IOException {
    this.data = readFileAsMap(testSourcePath + filename);

    Optional<String> actual = LabelExtractor.extract(this.data, field, LabelExtractor.Policy.SPLIT_OR);
    boolean shouldExist = expected != null;

    logger.info(String.format("shouldExist? %b, expected: %s", shouldExist, expected));

    assertEquals(shouldExist, actual.isPresent());

    if (shouldExist) {
      assertEquals(expected, actual.get());
    } else {
      assertNull(expected);
    }
  }

  private static Stream<Arguments> splitAndScenarios() {
    return Stream.of(
        Arguments.of("test_data1.json", "field17", "IFSR&SDGT"),
        Arguments.of("test_data1.json", "field18", "\"IFSR SDGT\"")
    );
  }

  @ParameterizedTest
  @MethodSource("splitAndScenarios")
  public void splitAndScenarios(String filename, String field, String expected) throws IOException {
    this.data = readFileAsMap(testSourcePath + filename);

    Optional<String> actual = LabelExtractor.extract(this.data, field, LabelExtractor.Policy.SPLIT_AND);
    boolean shouldExist = expected != null;

    logger.info(String.format("shouldExist? %b, expected: %s", shouldExist, expected));

    assertEquals(shouldExist, actual.isPresent());

    if (shouldExist) {
      assertEquals(expected, actual.get());
    } else {
      assertNull(expected);
    }
  }
}
