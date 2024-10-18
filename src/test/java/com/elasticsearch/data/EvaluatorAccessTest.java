package com.elasticsearch.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.apache.accumulo.access.AccessEvaluator;
import org.apache.accumulo.access.Authorizations;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class EvaluatorAccessTest {
  private static final Logger logger = Logger.getLogger(EvaluatorAccessTest.class.getName());

  AccessEvaluator evaluator;
  static String[] authorizations_1 = {"BLUE", "GREEN", "PINK", "RED"};
  static String[] authorizations_2 = {
    "BLUE_GREEN", "BLUE-GREEN", "BLUE:GREEN", "BLUE.GREEN", "BLUE/GREEN"
  };
  static String[] authorizations_3 = {"\"IFSR, SDGT\""};
  static String[] authorizations_4 = {"\"IFSR,SDGT\""};
  static String[] authorizations_5 = {"IFSR", "SDGT"};
  static String[] authorizations_6 = {"IFSR,SDGT"};
  boolean canAccess;

  /**
   * Acceptable tokens are defined in Tokenizer class in accumulo-access library. - 0-9 - a-z - A-Z
   * - _-:./
   *
   * <p>Acceptable operators are '&' (AND) and '|' (OR).
   */
  private static Stream<Arguments> scenarios() {
    return Stream.of(
        Arguments.of(authorizations_1, "BLUE", true),
        Arguments.of(authorizations_1, "BLUE|GREEN", true),
        Arguments.of(authorizations_1, "BLUE&GREEN", true),
        Arguments.of(authorizations_1, "(BLUE)&GREEN", true),
        Arguments.of(authorizations_1, "(BLUE)&(GREEN)", true),
        Arguments.of(authorizations_1, "BLUE&GREEN", true),
        Arguments.of(authorizations_1, "\"BLUE\"", true),
        Arguments.of(authorizations_1, "(RED&GREEN)|(BLUE&PINK)", true),
        Arguments.of(authorizations_2, "BLUE_GREEN", true),
        Arguments.of(authorizations_2, "BLUE#GREEN", false),
        Arguments.of(authorizations_2, "BLUE\\GREEN", false),
        Arguments.of(authorizations_2, "BLUE GREEN", false),
        Arguments.of(authorizations_2, "BLUE,GREEN", false),
        Arguments.of(authorizations_2, "BLUE-GREEN", true),
        Arguments.of(authorizations_2, "BLUE:GREEN", true),
        Arguments.of(authorizations_2, "BLUE.GREEN", true),
        Arguments.of(authorizations_2, "BLUE/GREEN", true),
        Arguments.of(authorizations_3, "\"IFSR, SDGT\"", false),
        Arguments.of(authorizations_4, "\"IFSR,SDGT\"", false),
        Arguments.of(authorizations_5, "IFSR|SDGT", true),
        Arguments.of(authorizations_5, "IFSR&SDGT", true),
        Arguments.of(authorizations_6, "\"IFSR,SDGT\"", true)
    );
  }

  @ParameterizedTest
  @MethodSource("scenarios")
  public void verify(String[] authorizations, String expression, boolean expected) {

    this.evaluator = AccessEvaluator.of(Authorizations.of(Set.of(authorizations)));

    try {
      canAccess = this.evaluator.canAccess(expression);
    } catch (Exception e) {
      logger.info("evaluator threw exception, error: " + e.getMessage());
      canAccess = false;
    }

    logger.info(String.format("authorizations: %s, expression: %s, canAccess: %b", Arrays.deepToString(authorizations), expression, canAccess));

    assertEquals(expected, canAccess);
  }
}
