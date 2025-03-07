package com.premiumminds.billy.portugal.services.export.saftpt.v1_04_01.schema;

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

    // ... (fields and getters/setters remain unchanged)

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
            String theSupplierID;
            theSupplierID = this.getSupplierID();
            strategy.appendField(locator, this, "supplierID", buffer, theSupplierID, (this.supplierID != null));
        }
        {
            String theAccountID;
            theAccountID = this.getAccountID();
            strategy.appendField(locator, this, "accountID", buffer, theAccountID, (this.accountID != null));
        }
        {
            String theSupplierTaxID;
            theSupplierTaxID = this.getSupplierTaxID();
            strategy.appendField(locator, this, "supplierTaxID", buffer, theSupplierTaxID, (this.supplierTaxID != null));
        }
        {
            String theCompanyName;
            theCompanyName = this.getCompanyName();
            strategy.appendField(locator, this, "companyName", buffer, theCompanyName, (this.companyName != null));
        }
        {
            String theContact;
            theContact = this.getContact();
            strategy.appendField(locator, this, "contact", buffer, theContact, (this.contact != null);
        }
        {
            SupplierAddressStructure theBillingAddress;
            theBillingAddress = this.getBillingAddress();
            strategy.appendField(locator, this, "billingAddress", buffer, theBillingAddress, (this.billingAddress != null);
        }
        {
            List<SupplierAddressStructure> theShipFromAddress;
            theShipFromAddress = (((this.shipFromAddress != null) && (!this.shipFromAddress.isEmpty())) ? this.getShipFromAddress() : null);
            strategy.appendField(locator, this, "shipFromAddress", buffer, theShipFromAddress, ((this.shipFromAddress != null) && (!this.shipFromAddress.isEmpty())));
        }
        {
            String theTelephone;
            theTelephone = this.getTelephone();
            strategy.appendField(locator, this, "telephone", buffer, theTelephone, (this.telephone != null);
        }
        {
            String theFax;
            theFax = this.getFax();
            strategy.appendField(locator, this, "fax", buffer, theFax, (this.fax != null);
        }
        {
            String theEmail;
            theEmail = this.getEmail();
            strategy.appendField(locator, this, "email", buffer, theEmail, (this.email != null);
        }
        {
            String theWebsite;
            theWebsite = this.getWebsite();
            strategy.appendField(locator, this, "website", buffer, theWebsite, (this.website != null);
        }
        {
            int theSelfBillingIndicator;
            theSelfBillingIndicator = this.getSelfBillingIndicator();
            strategy.appendField(locator, this, "selfBillingIndicator", buffer, theSelfBillingIndicator, true);
        }
        return buffer;
    }
}