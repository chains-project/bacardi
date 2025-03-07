package com.premiumminds.billy.portugal.services.export.saftpt.v1_02_01.schema;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;
import org.jvnet.jaxb2_commons.lang.JAXBToStringStrategy;
import org.jvnet.jaxb2_commons.lang.ToString2;
import org.jvnet.jaxb2_commons.lang.ToStringStrategy2;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "auditFileVersion",
    "companyID",
    "taxRegistrationNumber",
    "taxAccountingBasis",
    "companyName",
    "businessName",
    "companyAddress",
    "fiscalYear",
    "startDate",
    "endDate",
    "currencyCode",
    "dateCreated",
    "taxEntity",
    "productCompanyTaxID",
    "softwareCertificateNumber",
    "productID",
    "productVersion",
    "headerComment",
    "telephone",
    "fax",
    "email",
    "website"
})
@XmlRootElement(name = "Header")
public class Header implements ToString2 {

    // ... (fields and getters/setters remain unchanged)

    @Override
    public String toString() {
        final ToStringStrategy2 strategy = new JAXBToStringStrategy(); // Use constructor instead of getInstance()
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
            String theAuditFileVersion;
            theAuditFileVersion = this.getAuditFileVersion();
            strategy.appendField(locator, this, "auditFileVersion", buffer, theAuditFileVersion, (this.auditFileVersion != null));
        }
        {
            String theCompanyID;
            theCompanyID = this.getCompanyID();
            strategy.appendField(locator, this, "companyID", buffer, theCompanyID, (this.companyID != null));
        }
        {
            int theTaxRegistrationNumber;
            theTaxRegistrationNumber = this.getTaxRegistrationNumber();
            strategy.appendField(locator, this, "taxRegistrationNumber", buffer, theTaxRegistrationNumber, true);
        }
        {
            String theTaxAccountingBasis;
            theTaxAccountingBasis = this.getTaxAccountingBasis();
            strategy.appendField(locator, this, "taxAccountingBasis", buffer, theTaxAccountingBasis, (this.taxAccountingBasis != null));
        }
        {
            String theCompanyNameName;
            theCompanyNameName = this.getCompanyNameName();
            strategy.appendField(locator, this, "companyName", buffer, theCompanyNameName, (this.companyName != null));
        }
        {
            String theBusinessName;
            theBusinessName = this.getBusinessName();
            strategy.appendField(locator, this, "businessName", buffer, theBusinessName, (this.businessName != null));
        }
        {
            AddressStructurePT theCompanyAddress;
            theCompanyAddress = this.getCompanyAddress();
            strategy.appendField(locator, this, "companyAddress", buffer, theCompanyAddress, (this.companyAddress != null);
        }
        {
            int theFiscalYear;
            theFiscalYear = this.getFiscalYear();
            strategy.appendField(locator, this, "fiscalYear", buffer, theFiscalYear, true);
        }
        {
            XMLGregorianCalendar theStartDate;
            theStartDate = this.getStartDate();
            strategy.appendField(locator, this, "startDate", buffer, theStartDate, (this.startDate != null);
        }
        {
            XMLGregorianCalendar theEndDate;
            theEndDate = this.getEndDate();
            strategy.appendField(locator, this, "endDate", buffer, theEndDate, (this.endDate != null);
        }
        {
            Object theCurrencyCode;
            theCurrencyCode = this.getCurrencyCode();
            strategy.appendField(locator, this, "currencyCode", buffer, theCurrencyCode, (this.currencyCode != null);
        }
        {
            XMLGregorianCalendar theDateCreated;
            theDateCreated = this.getDateCreated();
            strategy.appendField(locator, this, "dateCreated", buffer, theDateCreated, (this.dateCreated != null);
        }
        {
            String theTaxEntity;
            theTaxEntity = this.getTaxEntity();
            strategy.appendField(locator, this, "taxEntity", buffer, theTaxEntity, (this.taxEntity != null);
        }
        {
            String theProductCompanyTaxID;
            theProductCompanyTaxID = this.getProductCompanyTaxID();
            strategy.appendField(locator, this, "productCompanyTaxID", buffer, theProductCompanyTaxID, (this.productCompanyTaxID != null);
        }
        {
            BigInteger theSoftwareCertificateNumber;
            theSoftwareCertificateNumber = this.getSoftwareCertificateNumber();
            strategy.appendField(locator, this, "softwareCertificateNumber", buffer, theSoftwareCertificateNumber, (this.softwareCertificateNumber != null);
        }
        {
            String theProductID;
            theProductID = this.getProductID();
            strategy.appendField(locator, this, "productID", buffer, theProductID, (this.productID != null);
        }
        {
            String theProductVersion;
            theProductVersion = this.getProductVersion();
            strategy.appendField(locator, this, "productVersion", buffer, theProductVersion, (this.productVersion != null);
        }
        {
            String theHeaderComment;
            theHeaderComment = this.getHeaderComment();
            strategy.appendField(locator, this, "headerComment", buffer, theHeaderComment, (this.headerComment != null);
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
        return buffer;
    }
}