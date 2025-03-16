package com.premiumminds.billy.portugal.services.export.saftpt.v1_03_01.schema;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.jvnet.jaxb2_commons.lang.ToString2;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "supplierID",
    "accountID",
    "supplierTaxID",
    "companyName",
    "contact",
    "billingAddress",
    "shipFromAddress",
    "telephone",
    "fax",
    "email",
    "website",
    "selfBillingIndicator"
})
@XmlRootElement(name = "Supplier")
public class Supplier implements ToString2 {

{
    @XmlElement(name = "SupplierID", required = true)
    protected String supplierID;
    @XmlElement(name = "AccountID", required = true)
    protected String accountID;
    @XmlElement(name = "SupplierTaxID", required = true)
    protected String supplierTaxID;
    @XmlElement(name = "CompanyName", required = true)
    protected String companyName;
    @XmlElement(name = "Contact")
    protected String contact;
    @XmlElement(name = "BillingAddress", required = true)
    protected SupplierAddressStructure billingAddress;
    @XmlElement(name = "ShipFromAddress")
    protected List<SupplierAddressStructure> shipFromAddress;
    @XmlElement(name = "Telephone")
    protected String telephone;
    @XmlElement(name = "Fax")
    protected String fax;
    @XmlElement(name = "Email")
    protected String email;
    @XmlElement(name = "Website")
    protected String website;
    @XmlElement(name = "SelfBillingIndicator")
    protected int selfBillingIndicator;

    // ... (rest of the getter and setter methods)

    @Override
    public String toString() {
        final org.jvnet.jaxb2_commons.lang.ToStringStrategy2 strategy = new org.jvnet.jaxb2_commons.lang.JAXBToStringStrategy();
        final StringBuilder buffer = new StringBuilder();
        append(null, buffer, strategy);
        return buffer.toString();
    }

    @Override
    public StringBuilder append(ObjectLocator locator, StringBuilder buffer, org.jvnet.jaxb2_commons.lang.ToStringStrategy2 strategy) {
        strategy.appendStart(locator, this, buffer);
        appendFields(locator, buffer, strategy);
        strategy.appendEnd(locator, this, buffer);
        return buffer;
    }

    @Override
    public StringBuilder appendFields(ObjectLocator locator, StringBuilder buffer, org.jvnet.jaxb2_commons.lang.ToStringStrategy2 strategy) {
        {
            String theSupplierID;
            theSupplierID = this.getSupplierID();
            strategy.appendField(locator, this, "supplierID", buffer, theSupplierID, (this.supplierID != null));
        }
        // ... (rest of the appendFields method)
    }
}