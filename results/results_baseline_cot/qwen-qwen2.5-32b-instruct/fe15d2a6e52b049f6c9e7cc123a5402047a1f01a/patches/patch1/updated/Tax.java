package com.premiumminds.billy.portugal.services.export.saftpt.v1_02_01.schema;

import java.math.BigDecimal;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import org.jvnet.jaxb2_commons.lang.JAXBToStringStrategy;
import org.jvnet.jaxb2_commons.lang.ToString2;
import org.jvnet.jaxb2_commons.lang.ToStringStrategy2;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Tax", propOrder = {
    "taxType",
    "taxCountryRegion",
    "taxCode",
    "taxPercentage",
    "taxAmount"
})
public class Tax implements ToString2 {

    @XmlElement(name = "TaxType", required = true)
    protected String taxType;
    @XmlElement(name = "TaxCountryRegion", required = true)
    protected String taxCountryRegion;
    @XmlElement(name = "TaxCode", required = true)
    protected String taxCode;
    @XmlElement(name = "TaxPercentage")
    protected BigDecimal taxPercentage;
    @XmlElement(name = "TaxAmount")
    protected BigDecimal taxAmount;

    // ... (other methods remain unchanged)

    @Override
    public String toString() {
        final JAXBToStringStrategy strategy = new JAXBToStringStrategy(); // Direct instantiation
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
            String theTaxType;
            theTaxType = this.getTaxType();
            strategy.appendField(locator, this, "taxType", buffer, theTaxType, (this.taxType != null));
        }
        {
            String theTaxCountryRegion;
            theTaxCountryRegion = this.getTaxCountryRegion();
            strategy.appendField(locator, this, "taxCountryRegion", buffer, theTaxCountryRegion, (this.taxCountryRegion != null));
        }
        {
            String theTaxCode;
            theTaxCode = this.getTaxCode();
            strategy.appendField(locator, this, "taxCode", buffer, theTaxCode, (this.taxCode != null));
        }
        {
            BigDecimal theTaxPercentage;
            theTaxPercentage = this.getTaxPercentage();
            strategy.appendField(locator, this, "taxPercentage", buffer, theTaxPercentage, (this.taxPercentage != null));
        }
        {
            BigDecimal theTaxAmount;
            theTaxAmount = this.getTaxAmount();
            strategy.appendField(locator, this, "taxAmount", buffer, theTaxAmount, (this.taxAmount != null));
        }
        return buffer;
    }
}