package de.gwdg.metadataqa.marc.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
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

import static de.gwdg.metadataqa.marc.Utils.count;

public class MarcRecord implements Extractable, Validatable, Serializable {

  private static final Logger logger = Logger.getLogger(MarcRecord.class.getCanonicalName());
  private static final Pattern dataFieldPattern = Pattern.compile("^(\\d\\d\\d)\\$(.*)$");
  private static final Pattern positionalPattern = Pattern.compile("^(Leader|00[678])/(.*)$");
  private static final List<String> simpleControlTags = Arrays.asList("001", "003", "005");
  private static final Map<String, Boolean> undefinedTags = new HashMap<>();

  private Leader leader;
  private MarcControlField control001;
  private MarcControlField control003;
  private MarcControlField control005;
  private List<Control006> control006 = new ArrayList<>();
  private List<Control007> control007 = new ArrayList<>();
  private Control008 control008;
  private List<DataField> datafields;
  private Map<String, List<DataField>> datafieldIndex;
  private Map<String, List<MarcControlField>> controlfieldIndex;
  Map<String, List<String>> mainKeyValuePairs;
  private List<ValidationError> validationErrors = null;
  private SchemaType schemaType = SchemaType.MARC21;

  public MarcRecord() {
    datafields = new ArrayList<>();
    datafieldIndex = new TreeMap<>();
    controlfieldIndex = new TreeMap<>();
  }

  public MarcRecord(String id) {
    this();
    control001 = new Control001(id);
  }

  // ... (other methods remain unchanged)

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
    } catch (JsonProcessingException e) {
      logger.log(Level.WARNING, "Error converting map to JSON", e);
      json = "{}"; // Return an empty JSON object in case of an error
    }

    return json;
  }

  // ... (rest of the class remains unchanged)
}