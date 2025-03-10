@Override
public String toString() {
    final ToStringStrategy2 strategy = new JAXBToStringStrategy();
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
        SourceDocuments.SalesInvoices theSalesInvoices;
        theSalesInvoices = this.getSalesInvoices();
        strategy.appendField(locator, this, "salesInvoices", buffer, theSalesInvoices, (this.salesInvoices!= null));
    }
    {
        SourceDocuments.MovementOfGoods theMovementOfGoods;
        theMovementOfGoods = this.getMovementOfGoods();
        strategy.appendField(locator, this, "movementOfGoods", buffer, theMovementOfGoods, (this.movementOfGoods!= null));
    }
    {
        SourceDocuments.WorkingDocuments theWorkingDocuments;
        theWorkingDocuments = this.getWorkingDocuments();
        strategy.appendField(locator, this, "workingDocuments", buffer, theWorkingDocuments, (this.workingDocuments!= null));
    }
    return buffer;
}
