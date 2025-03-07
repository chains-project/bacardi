package com.premiumminds.billy.portugal.services.export.saftpt.v1_03_01.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.jvnet.jaxb2_commons.lang.ToString2;
import org.jvnet.jaxb2_commons.lang.ToStringStrategy2;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "productType",
    "productCode",
    "productGroup",
    "productDescription",
    "productNumberCode"
})
@XmlRootElement(name = "Product")
public class Product implements ToString2
{

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

    // ... (other methods remain unchanged)

    @Override
    public String toString() {
        final ToStringStrategy2 strategy = new ToStringStrategy2() {
            // Implement the necessary methods of ToStringStrategy2 here
            // This is a simplified example, you may need to implement more methods based on the actual requirements
            @Override
            public StringBuilder append(ObjectLocator locator, StringBuilder buffer, Object object) {
                return buffer.append(object.toString());
            }

            @Override
            public StringBuilder appendFields(ObjectLocator locator, StringBuilder buffer, Object object) {
                return buffer;
            }

            @Override
            public StringBuilder appendStart(ObjectLocator locator, StringBuilder buffer, Object object) {
                return buffer.append(object.getClass().getSimpleName()).append("@").append(Integer.toHexString(System.identityHashCode(object))).append(" { ");
            }

            @Override
            public StringBuilder appendEnd(ObjectLocator locator, StringBuilder buffer, Object object) {
                return buffer.append(" }");
            }
        };
        final StringBuilder buffer = new StringBuilder();
        append(null, buffer, strategy);
        return buffer.toString();
    }

    @Override
    public StringBuilder append(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
        strategy.appendStart(locator, buffer, this);
        appendFields(locator, buffer, strategy);
        strategy.appendEnd(locator, buffer, this);
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
        return buffer;
    }
}