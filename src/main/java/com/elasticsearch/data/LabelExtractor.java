package com.elasticsearch.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class LabelExtractor {
  private static final Logger logger = LogManager.getLogger(LabelExtractor.class);

  public enum Policy {
    NONE,
    SPLIT_OR,
    SPLIT_AND
  }

  public static Optional<String> extract(Map<String, Object> data, String labelField) {
    return extract(data, labelField, Policy.NONE);
  }

  public static Optional<String> extract(
      Map<String, Object> data, String labelField, Policy policy) {
    // if label field exists
    if (data.containsKey(labelField)) {
      Object value = data.get(labelField);

      // return the value if it is a String
      if (value instanceof String) {
        return Optional.of(sanitizeLabel((String) value, policy));
      }

      // return the value if it is a Number
      if (value instanceof Number) {
        return Optional.of(value.toString());
      }

      // if value is an array, 'OR' the elements in the array and return the joined string.
      if (value instanceof List) {
        List<?> list = (List<?>) value;
        // use LinkedHashSet to keep the ordering
        Set<String> set = new LinkedHashSet<>();

        list.forEach(
            item -> {
              Optional<String> v;

              if (item instanceof String) {
                v = sanitizeLabel(Optional.of((String) item), policy);
              } else if (item instanceof Number) {
                v = Optional.of((item).toString());
              } else {
                v = Optional.empty();
              }

              v.ifPresent(set::add);
            });

        if (set.isEmpty()) {
          return Optional.empty();
        } else {
          // 'OR' the elements in the set and return the joined string.
          String label = String.join("|", set);
          return Optional.of(label);
        }
      }

      // for everything else, return nothing.
      logger.info(
          "label field [{}] is expected to contain either String, Number or Array but is type of [{}]",
          labelField,
          value.getClass().getSimpleName());
      return Optional.empty();
    }

    // split the labelField into string array so that we can drill down
    String[] labelFields = labelField.split("\\.");

    // only proceed if labelField points to nested field
    if (labelFields.length == 0 || !data.containsKey(labelFields[0])) {
      logger.info(
          "label field [{}] does not exist", labelFields.length == 0 ? labelField : labelFields[0]);
      return Optional.empty();
    }

    Object value = data.get(labelFields[0]);
    // construct new label field excluding the first part in the path
    String newLabelField = String.join(".", Arrays.copyOfRange(labelFields, 1, labelFields.length));

    if (value instanceof Map) {
      return extract((Map) value, newLabelField);
    }

    if (value instanceof List) {
      List<?> list = (List<?>) value;

      if (list.get(0) instanceof Map) {
        Set<String> set = new LinkedHashSet<>();

        list.forEach(
            item -> {
              Optional<String> label = extract((Map) item, newLabelField);
              label.ifPresent(set::add);
            });

        if (!set.isEmpty()) {
          String label = String.join("|", set);
          return Optional.of(label);
        }
      }
    }

    // For everything else, return nothing.
    return Optional.empty();
  }

  public static Optional<String> sanitizeLabel(Optional<String> labelOptional, Policy policy) {
    return labelOptional.map(s -> sanitizeLabel(s, policy));
  }

  public static String sanitizeLabel(String label, Policy policy) {
    switch (policy) {
      case SPLIT_OR:
        Set<String> set1 = new LinkedHashSet<>();
        Stream.of(label.split(",")).forEach(item -> set1.add(sanitizeString(item)));
        return String.join("|", set1);
      case SPLIT_AND:
        Set<String> set2 = new LinkedHashSet<>();
        Stream.of(label.split(",")).forEach(item -> set2.add(sanitizeString(item)));
        return String.join("&", set2);
      case NONE:
      default:
        return sanitizeString(label);
    }
  }

  public static String sanitizeString(String source) {
    String output = source.trim();

    if (output.contains(" ")) {
      output = "\"" + output + "\"";
    }

    return output;
  }
}
