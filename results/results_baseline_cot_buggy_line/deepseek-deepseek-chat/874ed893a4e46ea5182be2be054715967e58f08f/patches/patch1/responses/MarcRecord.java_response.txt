package de.gwdg.metadataqa.marc.dao;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gwdg.metadataqa.marc.Extractable;
import de.gwdg.metadataqa.marc.MarcFactory;
import de.gwdg.metadataqa.marc.MarcSubfield;
import de.gwdg.metadataqa.marc.Utils;
import de.gwdg.metadataqa.marc.Validatable;
import de.gwdg.metadataqa.marc.cli.utils.IgnorableFields;
import de.gwdg.metadataqa.marc.definition.*;
import de.gwdg.metadataqa.marc.definition.bibliographic.SchemaType;
import de.gwdg.metadataqa.marc.definition.general.validator.ClassificationReferenceValidator;
import de.gwdg.metadataqa.marc.definition.structure.ControlfieldPositionDefinition;
import de.gwdg.metadataqa.marc.definition.structure.DataFieldDefinition;
import de.gwdg.metadataqa.marc.definition.structure.Indicator;
import de.gwdg.metadataqa.marc.model.SolrFieldType;
import de.gwdg.metadataqa.marc.model.validation.ValidationError;
import de.gwdg.metadataqa.marc.model.validation.ValidationErrorType;
import de.gwdg.metadataqa.marc.utils.marcspec.legacy.MarcSpec;

import de.gwdg.metadataqa.marc.utils.unimarc.UnimarcConverter;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.gwdg.metadataqa.marc.Utils.count;

public class MarcRecord implements Extractable, Validatable, Serializable {

  // ... (previous code remains unchanged until asJson() method)

  public String asJson() {
    ObjectMapper mapper = new ObjectMapper();

    Map<String, Object> map = new LinkedHashMap<>();
    map.put("leader", leader.getContent());

    for (MarcControlField field : getControlfields())
      if (field != null)
        map.put(field.getDefinition().getTag(), field.getContent());

    for (DataField field : datafields) {
      if (field != null) {
        Map<String, Object> fieldMap = new LinkedHashMap<>();
        fieldMap.put("ind1", field.getInd1());
        fieldMap.put("ind2", field.getInd2());

        Map<String, String> subfields = new LinkedHashMap<>();
        for (MarcSubfield subfield : field.getSubfields()) {
          subfields.put(subfield.getCode(), subfield.getValue());
        }
        fieldMap.put("subfields", subfields);

        String tag = field.getDefinition() != null
          ? field.getDefinition().getTag()
          : field.getTag();

        map.computeIfAbsent(tag, s -> new ArrayList<Map<String, Object>>());
        ((ArrayList)map.get(tag)).add(fieldMap);
      }
    }

    String json = null;
    try {
      json = mapper.writeValueAsString(map);
    } catch (JacksonException e) {
      logger.log(Level.WARNING, "error in asJson()", e);
    }

    return json;
  }

  // ... (rest of the class remains unchanged)
}