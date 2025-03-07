package com.premiumminds.billy.portugal.services.export.saftpt.v1_04_01.schema;

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
    "productType",
    "productCode",
    "productGroup",
    "productDescription",
    "productNumberCode",
    "customsDetails"
})
@XmlRootElement(name = "Product")
public class Product implements ToString2 {

    @XmlElement(name = "ProductType", required = true)
    protected String productType;
    @XmlElement(name = "ProductCode", required = true)
    protected String productCode;
    @XmlElement(name = "ProductGroup")
    protected String productGroup;
    @XmlElement(name = "ProductDescription", required = true)
    protected String productDescription;
    @XmlElement(name = "ProductNumberCode", required = true)
    protected String productNumberCode;
    @XmlElement(name = "CustomsDetails")
    protected CustomsDetails customsDetails;

    // ... (getters and setters remain unchanged)

    @Override
    public String toString() {
        final ToStringStrategy2 strategy = new JAXBToStringStrategy(); // Changed from getInstance() to new instance
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
            String theProductType;
            theProductType = this.getProductType();
            strategy.appendField(locator, this, "productType", buffer, theProductType, (this.productType != null));
        }
        {
            String theProductCode;
            theProductCode = this.getProductCode();
            strategy.appendField(locator, this, "productCode", buffer, theProductCode, (this.productCode != null));
        }
        {
            String theProductGroup;
            theProductGroup = this.getProductGroup();
            strategy.appendField(locator, this, "productGroup", buffer, theProductGroup, (this.productGroup != null));
        }
        {
            String theProductDescription;
            theProductDescription = this.getProductDescription();
            strategy.appendField(locator, this, "productDescription", buffer, theProductDescription, (this.productDescription != null));
        }
        {
            String theProductNumberCode;
            theProductNumberCode = this.getProductNumberCode();
            strategy.appendField(locator, this, "productNumberCode", buffer, theProductNumberCode, (this.productNumberCode != null));
        }
        {
            CustomsDetails theCustomsDetails;
            theCustomsDetails = this.getCustomsDetails();
            strategy.appendField(locator, this, "customsDetails", buffer, theCustomsDetails, (this.customsDetails != null));
        }
        return buffer;
    }
}