package com.premiumminds.billy.portugal.services.export.saftpt.v1_03_01.schema;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
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
    "movementOfGoods",
    "workingDocuments",
    "payments"
})
@XmlRootElement(name = "SourceDocuments")
public class SourceDocuments implements ToString2 {

{
    private static final ToStringStrategy2 STRATEGY = JAXBToStringStrategy.getInstance();

    // ... (other fields declarations)

    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        append(null, buffer, STRATEGY);
        return buffer.toString();
    }

    public StringBuilder append(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
        strategy.appendStart(locator, this, buffer);
        appendFields(locator, buffer, strategy);
        strategy.appendEnd(locator, this, buffer);
        return buffer;
    }

    // ... (other methods and nested classes)

    public static class MovementOfGoods implements ToString2
    {
        private static final ToStringStrategy2 STRATEGY = JAXBToStringStrategy.getInstance();

        // ... (other field declarations)

        public String toString() {
            final StringBuilder buffer = new StringBuilder();
            append(null, buffer, STRATEGY);
            return buffer.toString();
        }

        public StringBuilder append(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
            strategy.appendStart(locator, this, buffer);
            appendFields(locator, buffer, strategy);
            strategy.appendEnd(locator, this, buffer);
            return buffer;
        }

        // ... (other methods and nested classes)

        public static class DocumentStatus implements ToString2
        {
            private static final ToStringStrategy2 STRATEGY = JAXBToStringStrategy.getInstance();

            // ... (other field declarations)

            public String toString() {
                final StringBuilder buffer = new StringBuilder();
                append(null, buffer, STRATEGY);
                return buffer.toString();
            }

            public StringBuilder append(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
                strategy.appendStart(locator, this, buffer);
                appendFields(locator, buffer, strategy);
                strategy.appendEnd(locator, this, buffer);
                return buffer;
            }

            // ... (other methods and nested classes)

        }

        // ... (other nested classes implementations)

    }

    // ... (other nested class implementations)

}