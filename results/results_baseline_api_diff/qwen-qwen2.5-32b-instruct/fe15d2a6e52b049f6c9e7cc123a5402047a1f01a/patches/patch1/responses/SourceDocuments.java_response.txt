package com.premiumminds.billy.portugal.services.exportsaftptv1_02_01schema;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
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
@XmlType(name = "", propOrder = {
    "salesInvoices",
    "movementmentOfGoods",
    "workingDocuments"
})
@XmlRootElement(name = "SourceDocuments")
public class SourceDocuments implements ToString2 {

{
    // ... (rest of the class remains unchanged)

    @Override
    public String toString() {
        final ToStringStrategy2 strategy = new ToStringStrategy2() {
            @Override
            public StringBuilder appendField(ObjectLocator locator, Object object, String name, StringBuilder buffer, Object value, boolean isPrimitive) {
                buffer.append(name).append(": ").append(value);
                return buffer;
            }

            @Override
            public StringBuilder appendStart(ObjectLocator locator, Object object, StringBuilder buffer) {
                buffer.append(object.getClass().getName().append(": [");
                return buffer;
            }

            @Override
            public StringBuilder appendEnd(ObjectLocator locator, Object object, StringBuilder buffer) {
                buffer.append("]");
                return buffer;
            }
        };
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
            SourceDocuments.SaleInvoices theSaleInvoices;
            theSaleInvoices = this.getSaleInvoices();
            strategy.appendField(locator, this, "saleInvoices", buffer, theSaleInvoices, (this.saleInvoices!= null);
        }
        {
            SourceDocuments.MovementOfGoods theMovementOfGoods;
            theMovementOfGoods = this.getMovementOfGoods();
            strategy.appendField(locator, this, "movementmentOfGoods", buffer, theMovementOfGoods, (this.movementOfGoods!= null);
        }
        {
            SourceDocuments.WorkingDocuments theWorkingDocuments;
            theWorkingDocuments = this.getWorkingDocuments();
            strategy.appendField(locator, this, "workingDocuments", buffer, theWorkingDocuments, (this.workingDocuments!= null)
        }
        return buffer;
    }

    // ... (rest of the class remain unchanged)
}