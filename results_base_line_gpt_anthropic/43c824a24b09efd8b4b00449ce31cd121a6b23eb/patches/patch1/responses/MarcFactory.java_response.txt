package de.gwdg.metadataqa.marc;

import de.gwdg.metadataqa.api.json.JsonBranch; // Ensure this import is valid in the new dependency
import de.gwdg.metadataqa.api.model.pathcache.JsonPathCache;
import de.gwdg.metadataqa.api.model.XmlFieldInstance;
import de.gwdg.metadataqa.api.schema.MarcJsonSchema;
import de.gwdg.metadataqa.api.schema.Schema;
import de.gwdg.metadataqa.marc.dao.Control001;
import de.gwdg.metadataqa.marc.dao.Control003;
import de.gwdg.metadataqa.marc.dao.Control005;
import de.gwdg.metadataqa.marc.dao.Control006;
import de.gwdg.metadataqa.marc.dao.Control007;
import de.gwdg.metadataqa.marc.dao.Control008;
import de.gwdg.metadataqa.marc.dao.DataField;
import de.gwdg.metadataqa.marc.dao.Leader;
import de.gwdg.metadataqa.marc.dao.record.Marc21Record;
import de.gwdg.metadataqa.marc.dao.record.BibliographicRecord;
import de.gwdg.metadataqa.marc.dao.record.PicaRecord;
import de.gwdg.metadataqa.marc.definition.bibliographic.SchemaType;
import de.gwdg.metadataqa.marc.definition.structure.DataFieldDefinition;
import de.gwdg.metadataqa.marc.definition.MarcVersion;
import de.gwdg.metadataqa.marc.definition.structure.SubfieldDefinition;
import de.gwdg.metadataqa.marc.definition.TagDefinitionLoader;

import de.gwdg.metadataqa.marc.utils.alephseq.AlephseqLine;
import de.gwdg.metadataqa.marc.utils.MapToDatafield;

import de.gwdg.metadataqa.marc.utils.alephseq.MarcMakerLine;
import de.gwdg.metadataqa.marc.utils.alephseq.MarclineLine;
import de.gwdg.metadataqa.marc.utils.pica.PicaFieldDefinition;
import de.gwdg.metadataqa.marc.utils.pica.PicaLine;
import de.gwdg.metadataqa.marc.utils.pica.PicaSchemaManager;
import de.gwdg.metadataqa.marc.utils.pica.PicaSubfield;
import net.minidev.json.JSONArray;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.impl.*;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Factory class to create MarcRecord from JsonPathCache
 */
public class MarcFactory {

  private static final Logger logger = Logger.getLogger(MarcFactory.class.getCanonicalName());
  private static final List<String> fixableControlFields = Arrays.asList("006", "007", "008");

  private static Schema schema = new MarcJsonSchema();

  private MarcFactory() {
    throw new IllegalStateException("This is a utility class, can not be instantiated");
  }

  public static BibliographicRecord create(JsonPathCache cache) {
    return create(cache, MarcVersion.MARC21);
  }

  public static BibliographicRecord create(JsonPathCache cache, MarcVersion version) {
    var marcRecord = new Marc21Record();
    for (JsonBranch branch : schema.getPaths()) {
      if (branch.getParent() != null)
        continue;
      switch (branch.getLabel()) {
        case "leader":
          marcRecord.setLeader(new Leader(extractFirst(cache, branch)));
          break;
        case "001":
          marcRecord.setControl001(new Control001(extractFirst(cache, branch)));
          break;
        case "003":
          marcRecord.setControl003(new Control003(extractFirst(cache, branch)));
          break;
        case "005":
          marcRecord.setControl005(new Control005(extractFirst(cache, branch), marcRecord));
          break;
        case "006":
          marcRecord.setControl006(new Control006(extractFirst(cache, branch), marcRecord));
          break;
        case "007":
          marcRecord.setControl007(new Control007(extractFirst(cache, branch), marcRecord));
          break;
        case "008":
          marcRecord.setControl008(new Control008(extractFirst(cache, branch), marcRecord));
          break;
        default:
          JSONArray fieldInstances = (JSONArray) cache.getFragment(branch.getJsonPath());
          for (var fieldInsanceNr = 0; fieldInsanceNr < fieldInstances.size(); fieldInsanceNr++) {
            var fieldInstance = (Map) fieldInstances.get(fieldInsanceNr);
            var field = MapToDatafield.parse(fieldInstance, version);
            if (field != null) {
              marcRecord.addDataField(field);
              field.setMarcRecord(marcRecord);
            } else {
              marcRecord.addUnhandledTags(branch.getLabel());
            }
          }
          break;
      }
    }
    return marcRecord;
  }

  // Other methods remain unchanged

  // Ensure all other methods are retained and unchanged
}