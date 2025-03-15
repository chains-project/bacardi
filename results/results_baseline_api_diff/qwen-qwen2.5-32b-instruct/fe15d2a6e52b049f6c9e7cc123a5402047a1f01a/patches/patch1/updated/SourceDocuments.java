package com.premiumminds.billy.portugal.services.export.saftpt.v1_02_01.schema;

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

    @XmlElement(name = "SalesInvoices")
    protected SourceDocuments.SalesInvoices salesInvoice;
    @XmlElement(name = "MovementOfGoods")
    protected SourceDocuments.MovementOfGoods movementOfGoods;
    @XmlElement(name = "WorkingDocuments")
    protected SourceDocuments.WorkingDocuments workingDocuments;

    // ... (rest of the class remains unchanged)

    @Override
    public String toString() {
        final ToStringStrategy2 strategy = new ToStringStrategy2() {
            @Override
            public StringBuilder appendStart(ObjectLocator locator, Object o, StringBuilder buffer) {
                return buffer;
            }

            @Override
            public StringBuilder appendEnd(ObjectLocator locator, Object o, StringBuilder buffer) {
                return buffer;
            }

            @Override
            public StringBuilder appendField(ObjectLocator locator, Object o, String name, StringBuilder buffer, Object value, boolean isPrimitive) {
                if (value != null) {
                    buffer.append(name).concat(": ").append(value.toString());
                }
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
            SourceDocuments.SalesInvoice theSalesInvoice;
            theSalesInvoice = this.getSalesInvoice();
            strategy.appendField(locator, this, "salesInvoice", buffer, theSalesInvoice, (this.salesInvoice!= null);
        }
        {
            SourceDocuments.MovementOfGoods theMovementOfGoods;
            theMovementOfGoods = this.getMovementOfGoods();
            strategy.appendField(locator, this, "movementmentOfGoods", buffer, theMovementOfGoods, (this.movementOfGoods!= null);
        }
        {
            SourceDocuments.WorkingDocuments theWorkingDocuments;
            theWorkingDocuments = this.getWorkingDocuments();
            strategy.appendField(locator, this, "workingDocuments", buffer, theWorkingDocuments, (this.workingDocuments!= null);
        }
        return buffer;
    }

    // ... (rest of the class remain unchanged)

}