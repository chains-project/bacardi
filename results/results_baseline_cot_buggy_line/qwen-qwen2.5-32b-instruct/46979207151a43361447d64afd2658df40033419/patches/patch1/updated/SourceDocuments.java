package com.premiumminds.billy.portugal.services.export.saftpt.v1_02_01.schema;

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
import org.jvnet.jaxb2_commons.lang.ToString2;
import org.jvnet.jaxb2_commons.lang.ToStringStrategy2;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "salesInvoices",
    "movementmentOfGoods",
    "workingDocuments"
})
@XmlRootElement(name = "SourceDocuments")
public class SourceDocuments implements ToString2 {

{
    // ... (other fields and methods remain unchanged)

    private static final ToStringStrategy2 STRATEGY = new ToStringStrategy2() {
        // Implement the required methods of ToStringStrategy2 here
        // This is a placeholder for the actual implementation of the strategy
    };

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

    public StringBuilder appendFields(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
        // ... (rest of the appendFields method remains unchanged)
    }

    // ... (other inner classes definitions remain unchanged)

    public static class MovementOfGoods implements ToString2
    {
        // ... (other fields and methods remain unchanged)

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

        public StringBuilder appendFields(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
            // ... (rest of the appendFields method remain unchanged)
        }

        // ... (other inner class definitions remain unchanged)

        public static class StockMovement implements ToString2
        {
            // ... (other fields and methods remain unchanged)

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

            public StringBuilder appendFields(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
                // ... (rest of the appendFields method remain unchanged)
            }

            // ... (other inner class definitions remain unchanged)

            public static class DocumentStatus implements ToString2
            {
                // ... (other fields and methods definitions remain unchanged)

                public String toString() {
                    final StringBuilder buffer = new StringBuilder();
                    append(null, buffer, STRATEGY);
                    return buffer.toString();
                }

                public StringBuilder append(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
                    strategy.appendStart(locator, this, buffer)
                    appendFields(locator, buffer, strategy)
                    strategy.appendEnd(locator, this, buffer)
                    return buffer;
                }

                public StringBuilder appendFields(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
                    // ... (rest of the appendFields method remain unchanged)
                }

                // ... (other inner class definitions remain unchanged)

            }

            // ... (other inner class definitions remain unchanged)

        }

        // ... (other inner class definitions remain unchanged)

    }

    // ... (other inner class definitions remain unchanged)

    public static class SalesInvoices implements ToString2
    {
        // ... (other fields and methods remain unchanged)

        public String toString() {
            final StringBuilder buffer = new StringBuilder();
            append(null, buffer, STRATEGY);
            return buffer.toString();
        }

        public StringBuilder append(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
            strategy.appendStart(locator, this, buffer)
            appendFields(locator, buffer, strategy)
            strategy.appendEnd(locator, this, buffer)
            return buffer;
        }

        public StringBuilder appendFields(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
            // ... (rest of the appendFields method remain unchanged)
        }

        // ... (other inner class definitions remain unchanged)

        public static class Invoice implements ToString2
        {
            // ... (other fields and methods remain unchanged)

            public String toString() {
                final StringBuilder buffer = new StringBuilder();
                append(null, buffer, STRATEGY);
                return buffer.toString();
            }

            public StringBuilder append(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
                strategy.appendStart(locator, this, buffer)
                appendFields(locator, buffer, strategy)
                strategy.appendEnd(locator, this, buffer)
                return buffer;
            }

            public StringBuilder appendFields(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
                // ... (rest of the appendFields method remain unchanged)
            }

            // ... (other inner class definitions remain unchanged)

            public static class DocumentStatus implements ToString2
            {
                // ... (other fields and methods definitions remain unchanged)

                public String toString() {
                    final StringBuilder buffer = new StringBuilder();
                    append(null, buffer, STRATEGY);
                    return buffer.toString();
                }

                public StringBuilder append(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
                    strategy.appendStart(locator, this, buffer)
                    appendFields(locator, buffer, strategy)
                    strategy.appendEnd(locator, this, buffer)
                    return buffer;
                }

                public StringBuilder appendFields(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
                    // ... (rest of the appendFields method remain unchanged)
                }

                // ... (other inner class definitions remain unchanged)

            }

            // ... (other inner class definitions remain unchanged)

        }

        // ... (other inner class definitions remain unchanged)

    }

    // ... (other inner class definitions remain unchanged)

    public static class WorkingDocuments implements ToString2
    {
        // ... (other fields and methods remain unchanged)

        public String toString() {
            final StringBuilder buffer = new StringBuilder();
            append(null, buffer, STRATEGY);
            return buffer.toString();
        }

        public StringBuilder append(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
            strategy.appendStart(locator, this, buffer)
            appendFields(locator, buffer, strategy)
            strategy.appendEnd(locator, this, buffer)
            return buffer;
        }

        public StringBuilder appendFields(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
            // ... (rest of the appendFields method remain unchanged)
        }

        // ... (other inner class definitions remain unchanged)

        public static class WorkDocument implements ToString2
        {
            // ... (other fields and methods definitions remain unchanged)

            public String toString() {
                final StringBuilder buffer = new StringBuilder();
                append(null, buffer, STRATEGY);
                return buffer.toString();
            }

            public StringBuilder append(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
                strategy.appendStart(locator, this, buffer)
                appendFields(locator, buffer, strategy)
                strategy.appendEnd(locator, this, buffer)
                return buffer;
            }

            public StringBuilder appendFields(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
                // ... (rest of the appendFields method remain unchanged)
            }

            // ... (other inner class definitions remain unchanged)

            public static class DocumentStatus implements ToString2
            {
                // ... (other fields and method definitions remain unchanged)

                public String toString() {
                    final StringBuilder buffer = new StringBuilder();
                    append(null, buffer, STRATEGY)
                    return buffer.toString();
                }

                public StringBuilder append(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
                    strategy.appendStart(locator, this, buffer)
                    appendFields(locator, buffer, strategy)
                    strategy.appendEnd(locator, this, buffer)
                    return buffer;
                }

                public StringBuilder appendFields(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
                    // ... (rest of the appendFields method remain unchanged)
                }

                // ... (other inner class definitions remain unchanged)

            }

            // ... (other inner class definitions remain unchanged)

        }

        // ... (other inner class definitions remain unchanged)

    }

    // ... (other inner class definitions remain unchanged)

}