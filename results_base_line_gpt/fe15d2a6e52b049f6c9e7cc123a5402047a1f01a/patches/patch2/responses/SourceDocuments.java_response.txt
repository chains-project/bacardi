@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "salesInvoices",
    "movementOfGoods",
    "workingDocuments"
})
@XmlRootElement(name = "SourceDocuments")
public class SourceDocuments implements ToString2
{

    @XmlElement(name = "SalesInvoices")
    protected SourceDocuments.SalesInvoices salesInvoices;
    @XmlElement(name = "MovementOfGoods")
    protected SourceDocuments.MovementOfGoods movementOfGoods;
    @XmlElement(name = "WorkingDocuments")
    protected SourceDocuments.WorkingDocuments workingDocuments;

    // ... (rest of the code remains unchanged)

    @Override
    public String toString() {
        final ToStringStrategy2 strategy = new ToStringStrategy2() {
            @Override
            public void appendStart(ObjectLocator locator, Object object, StringBuilder buffer) {
                buffer.append(object.getClass().getSimpleName()).append(" [");
            }

            @Override
            public void appendEnd(ObjectLocator locator, Object object, StringBuilder buffer) {
                buffer.append("]");
            }

            @Override
            public void appendField(ObjectLocator locator, Object object, String fieldName, StringBuilder buffer, Object value, boolean isSet) {
                if (isSet) {
                    buffer.append(fieldName).append(": ").append(value).append(", ");
                }
            }
        };
        final StringBuilder buffer = new StringBuilder();
        append(null, buffer, strategy);
        return buffer.toString();
    }

    // ... (rest of the code remains unchanged)
}