package com.premiumminds.billy.portugal.services.export.saftpt.v1_02_01.schema;

import java.math.BigDecimal;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;
import org.jvnet.jaxb2_commons.lang.JAXBToStringStrategy;
import org.jvnet.jaxb2_commons.lang.ToString2;
import org.jvnet.jaxb2_commons.lang.ToStringStrategy2;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Settlement", propOrder = {
    "settlementDiscount",
    "settlementAmount",
    "settlementDate",
    "paymentTerms"
})
public class Settlement implements ToString2
{

    @XmlElement(name = "SettlementDiscount")
    protected String settlementDiscount;
    @XmlElement(name = "SettlementAmount")
    protected BigDecimal settlementAmount;
    @XmlElement(name = "SettlementDate")
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar settlementDate;
    @XmlElement(name = "PaymentTerms")
    protected String paymentTerms;

    public String getSettlementDiscount() {
        return settlementDiscount;
    }

    public void setSettlementDiscount(String value) {
        this.settlementDiscount = value;
    }

    public BigDecimal getSettlementAmount() {
        return settlementAmount;
    }

    public void setSettlementAmount(BigDecimal value) {
        this.settlementAmount = value;
    }

    public XMLGregorianCalendar getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(XMLGregorianCalendar value) {
        this.settlementDate = value;
    }

    public String getPaymentTerms() {
        return paymentTerms;
    }

    public void setPaymentTerms(String value) {
        this.paymentTerms = value;
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
        {
            String theSettlementDiscount;
            theSettlementDiscount = this.getSettlementDiscount();
            strategy.appendField(locator, this, "settlementDiscount", buffer, theSettlementDiscount, (this.settlementDiscount != null));
        }
        {
            BigDecimal theSettlementAmount;
            theSettlementAmount = this.getSettlementAmount();
            strategy.appendField(locator, this, "settlementAmount", buffer, theSettlementAmount, (this.settlementAmount != null));
        }
        {
            XMLGregorianCalendar theSettlementDate;
            theSettlementDate = this.getSettlementDate();
            strategy.appendField(locator, this, "settlementDate", buffer, theSettlementDate, (this.settlementDate != null));
        }
        {
            String thePaymentTerms;
            thePaymentTerms = this.getPaymentTerms();
            strategy.appendField(locator, this, "paymentTerms", buffer, thePaymentTerms, (this.paymentTerms != null));
        }
        return buffer;
    }

}