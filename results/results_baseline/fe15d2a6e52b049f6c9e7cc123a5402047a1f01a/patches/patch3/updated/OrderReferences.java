package com.premiumminds.billy.portugal.services.export.saftpt.v1_02_01.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;
import org.jvnet.jaxb2_commons.lang.ToString2;
import org.jvnet.jaxb2_commons.lang.ToStringStrategy2;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;
import org.jvnet.jaxb2_commons.lang.JAXBToStringStrategy; // Added import

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OrderReferences", propOrder = {
    "originatingON",
    "orderDate"
})
public class OrderReferences implements ToString2
{

    @XmlElement(name = "OriginatingON")
    protected String originatingON;
    @XmlElement(name = "OrderDate")
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar orderDate;

    public String getOriginatingON() {
        return originatingON;
    }

    public void setOriginatingON(String value) {
        this.originatingON = value;
    }

    public XMLGregorianCalendar getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(XMLGregorianCalendar value) {
        this.orderDate = value;
    }

    @Override
    public String toString() {
        final ToStringStrategy2 strategy = new JAXBToStringStrategy(); // Updated instantiation
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
            String theOriginatingON;
            theOriginatingON = this.getOriginatingON();
            strategy.appendField(locator, this, "originatingON", buffer, theOriginatingON, (this.originatingON!= null));
        }
        {
            XMLGregorianCalendar theOrderDate;
            theOrderDate = this.getOrderDate();
            strategy.appendField(locator, this, "orderDate", buffer, theOrderDate, (this.orderDate!= null));
        }
        return buffer;
    }

}