package com.elasticsearch.data.elasticsearch;

import static com.elasticsearch.data.LabelExtractor.extract;

import com.elasticsearch.data.LabelExtractor;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.accumulo.access.AccessEvaluator;
import org.apache.accumulo.access.Authorizations;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.ScriptPlugin;
import org.elasticsearch.script.DocReader;
import org.elasticsearch.script.FilterScript;
import org.elasticsearch.script.FilterScript.LeafFactory;
import org.elasticsearch.script.ScriptContext;
import org.elasticsearch.script.ScriptEngine;
import org.elasticsearch.script.ScriptFactory;
import org.elasticsearch.search.lookup.SearchLookup;

/** A plugin to apply ABAC access policy to documents */
public class AccumuloAccessPlugin extends Plugin implements ScriptPlugin {
  private static final Logger logger = LogManager.getLogger(AccumuloAccessPlugin.class);

  private static final String LABEL_FIELD_NAME = "labelField";
  private static final String AUTHORIZATIONS_FIELD_NAME = "authorizations";
  private static final String LABEL_EXTRACTION_POLICY = "labelExtractionPolicy";

  public AccumuloAccessPlugin() {}

  @Override
  public ScriptEngine getScriptEngine(Settings settings, Collection<ScriptContext<?>> contexts) {
    return new ABACAccessEngine();
  }

  // tag::abac_security
  private static class ABACAccessEngine implements ScriptEngine {
    public ABACAccessEngine() {}

    @Override
    public String getType() {
      return "expert_scripts";
    }

    @Override
    public <T> T compile(
        String scriptName,
        String scriptSource,
        ScriptContext<T> context,
        Map<String, String> params) {

      // debugging information
      logger.debug("scriptName:{}, scriptSource:{}", scriptName, scriptSource);
      params.forEach((k, v) -> logger.debug("params, key:{}, value:{}", k, v));

      if (!context.equals(FilterScript.CONTEXT)) {
        throw new IllegalArgumentException(
            getType() + " scripts cannot be used for context [" + context.name + "]");
      }

      // we use the script "source" as the script identifier
      if ("accumulo-access".equals(scriptSource)) {
        FilterScript.Factory factory = new ContainsMultipleFactory();
        return context.factoryClazz.cast(factory);
      }

      throw new IllegalArgumentException("Unknown script source " + scriptSource);
    }

    @Override
    public void close() {
      // optionally close resources
    }

    @Override
    public Set<ScriptContext<?>> getSupportedContexts() {
      return Set.of(FilterScript.CONTEXT);
    }

    private static class ContainsMultipleFactory implements FilterScript.Factory, ScriptFactory {
      @Override
      public boolean isResultDeterministic() {
        return true;
      }

      @Override
      public LeafFactory newFactory(Map<String, Object> params, SearchLookup lookup) {
        return new ABACAccessLeafFactory(params, lookup);
      }
    }

    private static class ABACAccessLeafFactory implements LeafFactory {
      private final Map<String, Object> params;
      private final SearchLookup lookup;
      private final String labelField;
      private final String authorizations;
      private final LabelExtractor.Policy policy;
      private AccessEvaluator evaluator;

      private ABACAccessLeafFactory(Map<String, Object> params, SearchLookup lookup) {
        params.forEach((k, v) -> logger.debug("param, key:{}, value:{}", k, v));

        if (!params.containsKey(LABEL_FIELD_NAME)) {
          throw new IllegalArgumentException(
              String.format("Missing parameter [%s]", LABEL_FIELD_NAME));
        }

        if (!params.containsKey(AUTHORIZATIONS_FIELD_NAME)) {
          throw new IllegalArgumentException(
              String.format("Missing parameter [%s]", AUTHORIZATIONS_FIELD_NAME));
        }

        if (params.containsKey(LABEL_EXTRACTION_POLICY)) {
          policy = LabelExtractor.Policy.valueOf((String) params.get(LABEL_EXTRACTION_POLICY));
        } else {
          policy = LabelExtractor.Policy.NONE;
        }

        this.params = params;
        this.lookup = lookup;
        labelField = params.get(LABEL_FIELD_NAME).toString();
        authorizations = params.get(AUTHORIZATIONS_FIELD_NAME).toString();
      }

      @Override
      public FilterScript newInstance(DocReader docReader) {
        return new FilterScript(params, lookup, docReader) {
          @Override
          public boolean execute() {
            Optional<String> labelOptional =
                extract(docReader.source().get().source(), labelField, policy);

            if (labelOptional.isEmpty()) {
              logger.info("Unable to determine label for label field [{}]", labelField);
              return false;
            }

            String labelValue = labelOptional.get();

            try {
              evaluator = AccessEvaluator.of(Authorizations.of(Set.of(authorizations.split(","))));

              boolean canAccess = evaluator.canAccess(labelValue);

              logger.debug(
                  "labelField:{}, value:{}, canAccess?:{}", labelField, labelValue, canAccess);

              return canAccess;
            } catch (Exception e) {
              logger.info(
                  "Failed to process label field [{}], value: [{}], error:{}",
                  labelField,
                  labelValue,
                  e.getMessage());
              return false;
            }
          }
        };
      }
    }
    // end::abac_security
  }
}
