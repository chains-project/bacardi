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

    // ... (rest of the code remains unchanged)

    @Override
    public String toString() {
        final ToStringStrategy2 strategy = new JAXBToStringStrategy();
        final StringBuilder buffer = new StringBuilder();
        append(null, buffer, strategy);
        return buffer.toString();
    }

    // ... (rest of the code remains unchanged)

}