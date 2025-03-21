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
@XmlType(name = "Payment", propOrder = {
    "paymentMechanism",
    "paymentAmount",
    "paymentDate"
})
public class Payment implements ToString2 {

    @XmlElement(name = "PaymentMechanism", required = true)
    protected String paymentMechanism;
    @XmlElement(name = "PaymentAmount", required = true)
    protected BigDecimal paymentAmount;
    @XmlElement(name = "PaymentDate", required = true)
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar paymentDate;

    public String getPaymentMechanism() {
        return paymentMechanism;
    }

    public void setPaymentMechanism(String value) {
        this.paymentMechanism = value;
    }

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(BigDecimal value) {
        this.paymentAmount = value;
    }

    public XMLGregorianCalendar getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(XMLGregorianCalendar value) {
        this.paymentDate = value;
    }

    @Override
    public String toString() {
        final JAXBToStringStrategy strategy = new JAXBToStringStrategy();
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
            String thePaymentMechanism;
            thePaymentMechanism = this.getPaymentMechanism();
            strategy.appendField(locator, this, "paymentMechanism", buffer, thePaymentMechanism, (this.paymentMechanism != null));
        }
        {
            BigDecimal thePaymentAmount;
            thePaymentAmount = this.getPaymentAmount();
            strategy.appendField(locator, this, "paymentAmount", buffer, thePaymentAmount, (this.paymentAmount != null));
        }
        {
            XMLGregorianCalendar thePaymentDate;
            thePaymentDate = this.getPaymentDate();
            strategy.appendField(locator, this, "paymentDate", buffer, thePaymentDate, (this.paymentDate != null));
        }
        return buffer;
    }
}