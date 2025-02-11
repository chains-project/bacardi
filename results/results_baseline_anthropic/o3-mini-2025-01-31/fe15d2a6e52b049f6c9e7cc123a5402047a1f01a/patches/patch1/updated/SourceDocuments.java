package com.premiumminds.billy.portugal.services.export.saftpt.v1_04_01.schema;

import java.math.BigDecimal;
import java.math.BigInteger;
import javax.xml.bind.annotation.*;
import javax.xml.datatype.XMLGregorianCalendar;
import org.jvnet.jaxb2_commons.lang.ToString2;
import org.jvnet.jaxb2_commons.lang.ToStringStrategy2;
import org.jvnet.jaxb2_commons.lang.JAXBToStringStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "salesInvoices"
})
@XmlRootElement(name = "SourceDocuments")
public class SourceDocuments implements ToString2 {

    @XmlElement(name = "SalesInvoices")
    protected SourceDocuments.SalesInvoices salesInvoices;

    public SourceDocuments.SalesInvoices getSalesInvoices() {
        return salesInvoices;
    }

    public void setSalesInvoices(SourceDocuments.SalesInvoices value) {
        this.salesInvoices = value;
    }

    @Override
    public String toString() {
        final ToStringStrategy2 strategy = JAXBToStringStrategy.INSTANCE;
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
        strategy.appendField(locator, this, "salesInvoices", buffer, getSalesInvoices(), (salesInvoices != null));
        return buffer;
    }
    
    public static class SalesInvoices implements ToString2 {

        @XmlElement(name = "NumberOfEntries", required = true)
        @XmlSchemaType(name = "nonNegativeInteger")
        protected BigInteger numberOfEntries;

        @XmlElement(name = "TotalDebit", required = true)
        protected BigDecimal totalDebit;

        @XmlElement(name = "TotalCredit", required = true)
        protected BigDecimal totalCredit;

        public BigInteger getNumberOfEntries() {
            return numberOfEntries;
        }

        public void setNumberOfEntries(BigInteger value) {
            this.numberOfEntries = value;
        }

        public BigDecimal getTotalDebit() {
            return totalDebit;
        }

        public void setTotalDebit(BigDecimal value) {
            this.totalDebit = value;
        }

        public BigDecimal getTotalCredit() {
            return totalCredit;
        }

        public void setTotalCredit(BigDecimal value) {
            this.totalCredit = value;
        }

        @Override
        public String toString() {
            final ToStringStrategy2 strategy = JAXBToStringStrategy.INSTANCE;
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
            strategy.appendField(locator, this, "numberOfEntries", buffer, getNumberOfEntries(), (numberOfEntries != null));
            strategy.appendField(locator, this, "totalDebit", buffer, getTotalDebit(), (totalDebit != null));
            strategy.appendField(locator, this, "totalCredit", buffer, getTotalCredit(), (totalCredit != null));
            return buffer;
        }
    }
}