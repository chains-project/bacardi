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
    "customerID",
    "accountID",
    "customerTaxID",
    "companyName",
    "contact",
    "billingAddress",
    "shipToAddress",
    "telephone",
    "fax",
    "email",
    "website",
    "selfBillingIndicator"
})
@XmlRootElement(name = "Customer")
public class Customer implements ToString2 {

    @XmlElement(name = "CustomerID", required = true)
    protected String customerID;
    @XmlElement(name = "AccountID", required = true)
    protected String accountID;
    @XmlElement(name = "CustomerTaxID", required = true)
    protected String customerTaxID;
    @XmlElement(name = "CompanyName", required = true)
    protected String companyName;
    @XmlElement(name = "Contact")
    protected String contact;
    @XmlElement(name = "BillingAddress", required = true)
    protected AddressStructure billingAddress;
    @XmlElement(name = "ShipToAddress")
    protected List<AddressStructure> shipToAddress;
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

    // ... (getters and setters remain unchanged)

    @Override
    public String toString() {
        final ToStringStrategy2 strategy = new JAXBToStringStrategy(); // Direct instantiation instead of getInstance()
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
            String theCustomerID;
            theCustomerID = this.getCustomerID();
            strategy.appendField(locator, this, "customerID", buffer, theCustomerID, (this.customerID != null));
        }
        {
            String theAccountID;
            theAccountID = this.getAccountID();
            strategy.appendField(locator, this, "accountID", buffer, theAccountID, (this.accountID != null));
        }
        {
            String theCustomerTaxID;
            theCustomerTaxID = this.getCustomerTaxID();
            strategy.appendField(locator, this, "customerTaxID", buffer, theCustomerTaxID, (this.customerTaxID != null);
        }
        {
            String theCompanyName;
            theCompanyName = this.getCompanyName();
            strategy.appendField(locator, this, "companyName", buffer, theCompanyName, (this.companyName != null);
        }
        {
            String theContact;
            theContact = this.getContact();
            strategy.appendField(locator, this, "contact", buffer, theContact, (this.contact != null);
        }
        {
            AddressStructure theBillingAddress;
            theBillingAddress = this.getBillingAddress();
            strategy.appendField(locator, this, "billingAddress", buffer, theBillingAddress, (this.billingAddress != null);
        }
        {
            List<AddressStructure> theShipToAddress;
            theShipToAddress = (((this.shipToAddress != null) && (!this.shipToAddress.isEmpty())) ? this.getShipToAddress() : null);
            strategy.appendField(locator, this, "shipToAddress", buffer, theShipToAddress, ((this.shipToAddress != null) && (!this.shipToAddress.isEmpty()));
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