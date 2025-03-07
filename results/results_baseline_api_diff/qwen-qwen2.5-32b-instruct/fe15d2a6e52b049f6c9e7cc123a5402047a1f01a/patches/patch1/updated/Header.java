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
    @XmlElement(name = "CompanyNameName", required = true)
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

    public String getAuditFileVersion() {
        return auditFileVersion;
    }

    public void setAuditFileVersion(String value) {
        this.auditFileVersion = value;
    }

    public String getCompanyID() {
        return companyID;
    }

    public void setCompanyID(String value) {
        this.companyID = value;
    }

    public int getTaxRegistrationNumber() {
        return taxRegistrationNumber;
    }

    public void setTaxRegistrationNumber(int value) {
        this.taxRegistrationNumber = value;
    }

    public String getTaxAccountingBasis() {
        return taxAccountingBasis;
    }

    public void setTaxAccountingBasis(String value) {
        this.taxAccountingBasis = value;
    }

    public String getCompanyNameName() {
        return companyName;
    }

    public void setCompanyNameName(String value) {
        this.companyName = value;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String value) {
        this.businessName = value;
    }

    public AddressStructurePT getCompanyAddress() {
        return companyAddress;
    }

    public void setCompanyAddress(AddressStructurePT value) {
        this.companyAddress = value;
    }

    public int getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(int value) {
        this.fiscalYear = value;
    }

    public XMLGregorianCalendar getStartDate() {
        return startDate;
    }

    public void setStartDate(XMLGregorianCalendar value) {
        this.startDate = value;
    }

    public XMLGregorianCalendar getEndDate() {
        return endDate;
    }

    public void setEndDate(XMLGregorianCalendar value) {
        this.endDate = value;
    }

    public Object getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(Object value) {
        this.currencyCode = value;
    }

    public XMLGregorianCalendar getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(XMLGregorianCalendar value) {
        this.dateCreated = value;
    }

    public String getTaxEntity() {
        return taxEntity;
    }

    public void setTaxEntity(String value) {
        this.taxEntity = value;
    }

    public String getProductCompanyTaxID() {
        return productCompanyTaxID;
    }

    public void setProductCompanyTaxID(String value) {
        this.productCompanyTaxID = value;
    }

    public BigInteger getSoftwareCertificateNumber() {
        return softwareCertificateNumber;
    }

    public void setSoftwareCertificateNumber(BigInteger value) {
        this.softwareCertificateNumber = value;
    }

    public String getProductID() {
        return productID;
    }

    public void setProductID(String value) {
        this.productID = value;
    }

    public String getProductVersion() {
        return productVersion;
    }

    public void setProductVersion(String value) {
        this.productVersion = value;
    }

    public String getHeaderComment() {
        return headerComment;
    }

    public void setHeaderComment(String value) {
        this.headerComment = value;
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

    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        append(null, buffer);
        return buffer.toString();
    }

    public StringBuilder append(ObjectLocator locator, StringBuilder buffer) {
        buffer.append("Header [auditFileVersion=").append(auditFileVersion)
             .append(", companyID=").append(companyID)
             .append(", taxRegistrationNumber=").append(taxRegistrationNumber)
             .append(", taxAccountingBasis=").append(taxAccountingBasis)
             .append(", companyName=").append(companyName)
             .append(", businessName=").append(businessName)
             .append(", companyAddress=").append(companyAddress)
             .append(", fiscalYear=").append(fiscalYear)
             .append(", startDate=").append(startDate)
             .append(", endDate=").append(endDate)
             .append(", currencyCode=").append(currencyCode)
             .append(", dateCreated=").append(dateCreated)
             .append(", taxEntity=").append(taxEntity)
             .append(", productCompanyTaxID=").append(productCompanyTaxID)
             .append(", softwareCertificateNumber=").append(softwareCertificateNumber)
             .append(", productID=").append(productID)
             .append(", productVersion=").append(productVersion)
             .append(", headerComment=").append(headerComment)
             .append(", telephone=").append(telephone)
             .append(", fax=").append(fax)
             .append(", email=").append(email)
             .append(", website=").append(website)
             .append("]");
        return buffer;
    }
}