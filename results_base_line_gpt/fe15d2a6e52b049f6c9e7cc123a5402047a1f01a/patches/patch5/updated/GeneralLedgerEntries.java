import org.jvnet.jaxb2_commons.lang.JAXBToStringStrategy;
import org.jvnet.jaxb2_commons.lang.ToStringStrategy2;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.bind.annotation.XmlSchemaType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "numberOfEntries",
    "totalDebit",
    "totalCredit",
    "journal"
})
@XmlRootElement(name = "GeneralLedgerEntries")
public class GeneralLedgerEntries implements ToString2
{

    @XmlElement(name = "NumberOfEntries", required = true)
    @XmlSchemaType(name = "nonNegativeInteger")
    protected BigInteger numberOfEntries;
    @XmlElement(name = "TotalDebit", required = true)
    protected BigDecimal totalDebit;
    @XmlElement(name = "TotalCredit", required = true)
    protected BigDecimal totalCredit;
    @XmlElement(name = "Journal")
    protected List<GeneralLedgerEntries.Journal> journal;

    public String toString() {
        final ToStringStrategy2 strategy = JAXBToStringStrategy.getInstance(); // Updated instantiation
        final StringBuilder buffer = new StringBuilder();
        append(null, buffer, strategy);
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
            BigInteger theNumberOfEntries;
            theNumberOfEntries = this.getNumberOfEntries();
            strategy.appendField(locator, this, "numberOfEntries", buffer, theNumberOfEntries, (this.numberOfEntries!= null));
        }
        {
            BigDecimal theTotalDebit;
            theTotalDebit = this.getTotalDebit();
            strategy.appendField(locator, this, "totalDebit", buffer, theTotalDebit, (this.totalDebit!= null));
        }
        {
            BigDecimal theTotalCredit;
            theTotalCredit = this.getTotalCredit();
            strategy.appendField(locator, this, "totalCredit", buffer, theTotalCredit, (this.totalCredit!= null));
        }
        {
            List<GeneralLedgerEntries.Journal> theJournal;
            theJournal = (((this.journal!= null)&&(!this.journal.isEmpty()))?this.getJournal():null);
            strategy.appendField(locator, this, "journal", buffer, theJournal, ((this.journal!= null)&&(!this.journal.isEmpty())));
        }
        return buffer;
    }

    public StringBuilder appendFields(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
        {
            String theTransactionID;
            theTransactionID = this.getTransactionID();
            strategy.appendField(locator, this, "transactionID", buffer, theTransactionID, (this.transactionID!= null));
        }
        {
            int thePeriod;
            thePeriod = this.getPeriod();
            strategy.appendField(locator, this, "period", buffer, thePeriod, true);
        }
        {
            XMLGregorianCalendar theTransactionDate;
            theTransactionDate = this.getTransactionDate();
            strategy.appendField(locator, this, "transactionDate", buffer, theTransactionDate, (this.transactionDate!= null));
        }
        {
            String theSourceID;
            theSourceID = this.getSourceID();
            strategy.appendField(locator, this, "sourceID", buffer, theSourceID, (this.sourceID!= null));
        }
        {
            String theDescription;
            theDescription = this.getDescription();
            strategy.appendField(locator, this, "description", buffer, theDescription, (this.description!= null));
        }
        {
            String theDocArchivalNumber;
            theDocArchivalNumber = this.getDocArchivalNumber();
            strategy.appendField(locator, this, "docArchivalNumber", buffer, theDocArchivalNumber, (this.docArchivalNumber!= null));
        }
        {
            String theTransactionType;
            theTransactionType = this.getTransactionType();
            strategy.appendField(locator, this, "transactionType", buffer, theTransactionType, (this.transactionType!= null));
        }
        {
            XMLGregorianCalendar theGLPostingDate;
            theGLPostingDate = this.getGLPostingDate();
            strategy.appendField(locator, this, "glPostingDate", buffer, theGLPostingDate, (this.glPostingDate!= null));
        }
        {
            String theCustomerID;
            theCustomerID = this.getCustomerID();
            strategy.appendField(locator, this, "customerID", buffer, theCustomerID, (this.customerID!= null));
        }
        {
            String theSupplierID;
            theSupplierID = this.getSupplierID();
            strategy.appendField(locator, this, "supplierID", buffer, theSupplierID, (this.supplierID!= null));
        }
        {
            List<GeneralLedgerEntries.Journal.Transaction.Line> theLine;
            theLine = (((this.line!= null)&&(!this.line.isEmpty()))?this.getLine():null);
            strategy.appendField(locator, this, "line", buffer, theLine, ((this.line!= null)&&(!this.line.isEmpty())));
        }
        return buffer;
    }
}