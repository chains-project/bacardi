package com.premiumminds.billy.portugal.services.export.saftpt.v1_02_01.schema;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;
import org.jvnet.jaxb2_commons.lang.ToString2;
import org.jvnet.jaxb2_commons.lang.ToStringStrategy2;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;
import org.jvnet.jaxb2_commons.lang.JAXBToStringStrategy; // Ensure this import is correct

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
public class Header implements ToString2
{

    @XmlElement(name = "AuditFileVersion", required = true)
    protected String auditFileVersion;
    @XmlElement(name = "CompanyID", required = true)
    protected String companyID;
    @XmlElement(name = "TaxRegistrationNumber")
    @XmlSchemaType(name = "integer")
    protected int taxRegistrationNumber;
    @XmlElement(name = "TaxAccountingBasis", required = true)
    protected String taxAccountingBasis;
    @XmlElement(name = "CompanyName", required = true)
    protected String companyName;
    @XmlElement(name = "BusinessName")
    protected String businessName;
    @XmlElement(name = "CompanyAddress", required = true)
    protected AddressStructurePT companyAddress;
    @XmlElement(name = "FiscalYear")
    protected int fiscalYear;
    @XmlElement(name = "StartDate", required = true)
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar startDate;
    @XmlElement(name = "EndDate", required = true)
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar endDate;
    @XmlElement(name = "CurrencyCode", required = true)
    protected Object currencyCode;
    @XmlElement(name = "DateCreated", required = true)
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar dateCreated;
    @XmlElement(name = "TaxEntity", required = true)
    protected String taxEntity;
    @XmlElement(name = "ProductCompanyTaxID", required = true)
    protected String productCompanyTaxID;
    @XmlElement(name = "SoftwareCertificateNumber", required = true)
    @XmlSchemaType(name = "nonNegativeInteger")
    protected BigInteger softwareCertificateNumber;
    @XmlElement(name = "ProductID", required = true)
    protected String productID;
    @XmlElement(name = "ProductVersion", required = true)
    protected String productVersion;
    @XmlElement(name = "HeaderComment")
    protected String headerComment;
    @XmlElement(name = "Telephone")
    protected String telephone;
    @XmlElement(name = "Fax")
    protected String fax;
    @XmlElement(name = "Email")
    protected String email;
    @XmlElement(name = "Website")
    protected String website;

    // Getters and Setters...

    @Override
    public String toString() {
        final ToStringStrategy2 strategy = JAXBToStringStrategy.DEFAULT; // Updated to use DEFAULT
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
        // Append fields logic...
        return buffer;
    }
}