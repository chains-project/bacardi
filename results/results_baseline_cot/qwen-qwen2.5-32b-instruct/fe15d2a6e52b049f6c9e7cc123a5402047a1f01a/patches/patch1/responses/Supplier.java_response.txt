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

    private static final ToStringStrategy2 STRATEGY = new JAXBToStringStrategy();

    public String getSupplierID() {
        return supplierID;
    }

    public void setSupplierID(String value) {
        this.supplierID = value;
    }

    public String getAccountID() {
        return accountID;
    }

    public void setAccountID(String value) {
        this.accountID = value;
    }

    public String getSupplierTaxID() {
        return supplierTaxID;
    }

    public void setSupplierTaxID(String value) {
        this.supplierTaxID = value;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String value) {
        this.companyName = value;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String value) {
        this.contact = value;
    }

    public SupplierAddressStructure getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(SupplierAddressStructure value) {
        this.billingAddress = value;
    }

    public List<SupplierAddressStructure> getShipFromAddress() {
        if (shipFromAddress == null) {
            shipFromAddress = new ArrayList<>();
        }
        return this.shipFromAddress;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String value) {
        this.telephone = value;
    }

    public String getFax() {
        return fax;
    }

    public void setFax(String value) {
        this.fax = value;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String value) {
        this.email = value;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String value) {
        this.website = value;
    }

    public int getSelfBillingIndicator() {
        return selfBillingIndicator;
    }

    public void setSelfBillingIndicator(int value) {
        this.selfBillingIndicator = value;
    }

    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        append(null, buffer, STRATEGY);
        return buffer.toString();
    }

    public StringBuilder append(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
        strategy.appendStart(locator, this, buffer);
        appendFields(locator, buffer, strategy);
        strategy.appendEnd(locator, this, buffer);
        return buffer;
    }

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
            String theCompanyNameName;
            theCompanyName = this.getCompanyName();
            strategy.appendField(locator, this, "companyName", buffer, theCompanyName, (this.companyName != null));
        }
        {
            String theContact;
            theContact = this.getContact();
            strategy.appendField(locator, this, "contact", buffer, theContact, (this.contact != null));
        }
        {
            SupplierAddressStructure theBillingAddress;
            theBillingAddress = this.getBillingAddress();
            strategy.appendField(locator, this, "billingAddress", buffer, theBillingAddress, (this.billingAddress != null));
        }
        {
            List<SupplierAddressStructure> theShipFromAddress;
            theShipFromAddress = (((this.shipFromAddress != null) && (!this.shipFromAddress.isEmpty())) ? this.getShipFromAddress() : null);
            strategy.appendField(locator, this, "shipFromAddress", buffer, theShipFromAddress, ((this.shipFromAddress != null) && (!this.shipFromAddress.isEmpty())));
        }
        {
            String theTelephone;
            theTelephone = this.getTelephone();
            strategy.appendField(locator, this, "telephone", buffer, theTelephone, (this.telephone != null));
        }
        {
            String theFax;
            theFax = this.getFax();
            strategy.appendField(locator, this, "fax", buffer, theFax, (this.fax != null));
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