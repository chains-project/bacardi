package com.premiumminds.billy.portugal.services.export.saftpt.v1_02_01.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;
import org.jvnet.jaxb2_commons.lang.ToString2;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OrderReferences", propOrder = {
    "originatingON",
    "orderDate"
})
public class OrderReferences implements ToString2 {

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
        final StringBuilder buffer = new StringBuilder();
        append(null, buffer, new ToStringStrategy());
        return buffer.toString();
    }

    public StringBuilder append(ObjectLocator locator, StringBuilder buffer, ToStringStrategy strategy) {
        strategy.appendStart(locator, this, buffer);
        appendFields(locator, buffer, strategy);
        strategy.appendEnd(locator, this, buffer);
        return buffer;
    }

    public StringBuilder appendFields(ObjectLocator locator, StringBuilder buffer, ToStringStrategy strategy) {
        {
            String theOriginatingON;
            theOriginatingON = this.getOriginatingON();
            strategy.appendField(locator, this, "originatingON", buffer, theOriginatingON, (this.originatingON != null));
        }
        {
            XMLGregorianCalendar theOrderDate;
            theOrderDate = this.getOrderDate();
            strategy.appendField(locator, this, "orderDate", buffer, theOrderDate, (this.orderDate != null));
        }
        return buffer;
    }

    private interface ToStringStrategy {
        void appendStart(ObjectLocator locator, Object o, StringBuilder buffer);
        void appendEnd(ObjectLocator locator, Object o, StringBuilder buffer);
        void appendField(ObjectLocator locator, Object o, String fieldName, StringBuilder buffer, Object fieldValue, boolean isFieldPresent);
    }

    private class DefaultToStringStrategy implements ToStringStrategy {
        @Override
        public void appendStart(ObjectLocator locator, Object o, StringBuilder buffer) {
            buffer.append(o.getClass().getSimpleName()).append("@").append(Integer.toHexString(System.identityHashCode(o))).append(" {");
        }

        @Override
        public void appendEnd(ObjectLocator locator, Object o, StringBuilder buffer) {
            buffer.append("}");
        }

        @Override
        public void appendField(ObjectLocator locator, Object o, String fieldName, StringBuilder buffer, Object fieldValue, boolean isFieldPresent) {
            if (isFieldPresent) {
                buffer.append(fieldName).append("=").append(fieldValue).append(", ");
            }
        }
    }

    private ToStringStrategy newToStringStrategy() {
        return new DefaultToStringStrategy();
    }
}