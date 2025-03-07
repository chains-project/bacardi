package com.premiumminds.billy.portugal.services.export.saftpt.v1_03_01.schema;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.jvnet.jaxb2_commons.lang.JAXBToStringStrategy;
import org.jvnet.jaxb2_commons.lang.ToString2;
import org.jvnet.jaxb2_commons.lang.ToStringStrategy2;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "taxTableEntry"
})
@XmlRootElement(name = "TaxTable")
public class TaxTable implements ToString2 {

    @XmlElement(name = "TaxTableEntry", required = true)
    protected List<TaxTableEntry> taxTableEntry;

    public List<TaxTableEntry> getTaxTableEntry() {
        if (taxTableEntry == null) {
            taxTableEntry = new ArrayList<TaxTableEntry>();
        }
        return this.taxTableEntry;
    }

    @Override
    public String toString() {
        final ToStringStrategy2 strategy = new JAXBToStringStrategy(); // Direct instantiation
        final StringBuilder buffer = new StringBuilder();
        append(null, buffer, strategy);
        return buffer.toString();
    }

    @Override
    public StringBuilder append(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
        strategy.appendStart(locator, this, buffer);
        appendFields(locator, buffer, strategy);
        strategy.appendEnd(locator, this, buffer);
        return buffer;
    }

    @Override
    public StringBuilder appendFields(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
        {
            List<TaxTableEntry> theTaxTableEntry;
            theTaxTableEntry = (((this.taxTableEntry != null) && (!this.taxTableEntry.isEmpty())) ? this.getTaxTableEntry() : null);
            strategy.appendField(locator, this, "taxTableEntry", buffer, theTaxTableEntry, ((this.taxTableEntry != null) && (!this.taxTableEntry.isEmpty())));
        }
        return buffer;
    }
}