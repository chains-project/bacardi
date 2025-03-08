package com.premiumminds.billy.portugal.services.export.saftpt.v1_03_01.schema;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;
import org.jvnet.jaxb2_commons.lang.ToStringStrategy2;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "salesInvoices",
    "movementOfGoods",
    "workingDocuments",
    "payments"
})
@XmlRootElement(name = "SourceDocuments")
public class SourceDocuments implements ToString2 {

{
    // ... (other fields and methods remain unchanged)

    @Override
    public String toString() {
        final ToStringStrategy2 strategy = new ToStringStrategy2() {
            @Override
            public void appendField(ObjectLocator locator, Object object, String name, StringBuilder buffer, Object value, boolean isPrimitive) {
                // Implement the appendField method as needed
            }

            @Override
            public void appendStart(ObjectLocator locator, Object object, StringBuilder buffer) {
                // Implement the appendStart method as needed
            }

            @Override
            public void appendEnd(ObjectLocator locator, Object object, StringBuilder buffer) {
                // Implement the appendEnd method as needed
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
            strategy.appendField(locator, this, "saleInvoices", buffer, theSaleInvoices, (this.saleInvoices != null);
        }
        {
            SourceDocuments.MovementOfGoods theMovementOfGoods;
            theMovementOfGoods = this.getMovementOfGoods();
            strategy.appendField(locator, this, "movementOfGoods", buffer, theMovementOfGoods, (this.movementOfGoods != null);
        }
        {
            SourceDocuments.WorkingDocuments theWorkDocuments;
            theWorkDocuments = this.getWorkDocuments();
            strategy.appendField(locator, this, "workDocuments", buffer, theWorkDocuments, (this.workDocuments != null);
        }
        {
            SourceDocuments.Payments thePayments;
            thePayments = this.getPayments();
            strategy.appendField(locator, this, "payments", buffer, thePayments, (this.payments != null);
        }
        return buffer;
    }

    // ... (other inner classes and methods remain unchanged)
}