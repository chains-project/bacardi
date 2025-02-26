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

    public SourceDocuments.SalesInvoices getSalesInvoices() {
        return salesInvoices;
    }

    public void setSalesInvoices(SourceDocuments.SalesInvoices value) {
        this.salesInvoices = value;
    }

    public SourceDocuments.MovementOfGoods getMovementOfGoods() {
        return movementOfGoods;
    }

    public void setMovementOfGoods(SourceDocuments.MovementOfGoods value) {
        this.movementOfGoods = value;
    }

    public SourceDocuments.WorkingDocuments getWorkingDocuments() {
        return workingDocuments;
    }

    public void setWorkingDocuments(SourceDocuments.WorkingDocuments value) {
        this.workingDocuments = value;
    }

    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        append(null, buffer, null);
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
            strategy.appendField(locator, this, "salesInvoices", buffer, theSalesInvoices, (this.salesInvoices != null));
        }
        {
            SourceDocuments.MovementOfGoods theMovementOfGoods;
            theMovementOfGoods = this.getMovementOfGoods();
            strategy.appendField(locator, this, "movementOfGoods", buffer, theMovementOfGoods, (this.movementOfGoods != null));
        }
        {
            SourceDocuments.WorkingDocuments theWorkingDocuments;
            theWorkingDocuments = this.getWorkingDocuments();
            strategy.appendField(locator, this, "workingDocuments", buffer, theWorkingDocuments, (this.workingDocuments != null));
        }
        return buffer;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "numberOfMovementLines",
        "totalQuantityIssued",
        "stockMovement"
    })
    public static class MovementOfGoods implements ToString2
    {

        @XmlElement(name = "NumberOfMovementLines", required = true)
        @XmlSchemaType(name = "nonNegativeInteger")
        protected BigInteger numberOfMovementLines;
        @XmlElement(name = "TotalQuantityIssued", required = true)
        protected BigDecimal totalQuantityIssued;
        @XmlElement(name = "StockMovement")
        protected List<SourceDocuments.MovementOfGoods.StockMovement> stockMovement;

        public BigInteger getNumberOfMovementLines() {
            return numberOfMovementLines;
        }

        public void setNumberOfMovementLines(BigInteger value) {
            this.numberOfMovementLines = value;
        }

        public BigDecimal getTotalQuantityIssued() {
            return totalQuantityIssued;
        }

        public void setTotalQuantityIssued(BigDecimal value) {
            this.totalQuantityIssued = value;
        }

        public List<SourceDocuments.MovementOfGoods.StockMovement> getStockMovement() {
            if (stockMovement == null) {
                stockMovement = new ArrayList<SourceDocuments.MovementOfGoods.StockMovement>();
            }
            return this.stockMovement;
        }

        @Override
        public String toString() {
            final StringBuilder buffer = new StringBuilder();
            append(null, buffer, null);
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
                BigInteger theNumberOfMovementLines;
                theNumberOfMovementLines = this.getNumberOfMovementLines();
                strategy.appendField(locator, this, "numberOfMovementLines", buffer, theNumberOfMovementLines, (this.numberOfMovementLines != null));
            }
            {
                BigDecimal theTotalQuantityIssued;
                theTotalQuantityIssued = this.getTotalQuantityIssued();
                strategy.appendField(locator, this, "totalQuantityIssued", buffer, theTotalQuantityIssued, (this.totalQuantityIssued != null));
            }
            {
                List<SourceDocuments.MovementOfGoods.StockMovement> theStockMovement;
                theStockMovement = (((this.stockMovement != null) && (!this.stockMovement.isEmpty())) ? this.getStockMovement() : null);
                strategy.appendField(locator, this, "stockMovement", buffer, theStockMovement, ((this.stockMovement != null) && (!this.stockMovement.isEmpty())));
            }
            return buffer;
        }

        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
            "documentNumber",
            "documentStatus",
            "hash",
            "hashControl",
            "period",
            "movementDate",
            "movementType",
            "systemEntryDate",
            "transactionID",
            "customerID",
            "supplierID",
            "sourceID",
            "eacCode",
            "movementComments",
            "shipTo",
            "shipFrom",
            "movementEndTime",
            "movementStartTime",
            "atDocCodeID",
            "line",
            "documentTotals"
        })
        public static class StockMovement implements ToString2
        {

            @XmlElement(name = "DocumentNumber", required = true)
            protected String documentNumber;
            @XmlElement(name = "DocumentStatus", required = true)
            protected SourceDocuments.MovementOfGoods.StockMovement.DocumentStatus documentStatus;
            @XmlElement(name = "Hash", required = true)
            protected String hash;
            @XmlElement(name = "HashControl")
            protected String hashControl;
            @XmlElement(name = "Period")
            protected Integer period;
            @XmlElement(name = "MovementDate", required = true)
            @XmlSchemaType(name = "date")
            protected XMLGregorianCalendar movementDate;
            @XmlElement(name = "MovementType", required = true)
            protected String movementType;
            @XmlElement(name = "SystemEntryDate", required = true)
            @XmlSchemaType(name = "dateTime")
            protected XMLGregorianCalendar systemEntryDate;
            @XmlElement(name = "TransactionID")
            protected String transactionID;
            @XmlElement(name = "CustomerID")
            protected String customerID;
            @XmlElement(name = "SupplierID")
            protected String supplierID;
            @XmlElement(name = "SourceID", required = true)
            protected String sourceID;
            @XmlElement(name = "EACCode")
            protected String eacCode;
            @XmlElement(name = "MovementComments")
            protected String movementComments;
            @XmlElement(name = "ShipTo")
            protected ShippingPointStructure shipTo;
            @XmlElement(name = "ShipFrom")
            protected ShippingPointStructure shipFrom;
            @XmlElement(name = "MovementEndTime")
            @XmlSchemaType(name = "dateTime")
            protected XMLGregorianCalendar movementEndTime;
            @XmlElement(name = "MovementStartTime", required = true)
            @XmlSchemaType(name = "dateTime")
            protected XMLGregorianCalendar movementStartTime;
            @XmlElement(name = "ATDocCodeID")
            protected String atDocCodeID;
            @XmlElement(name = "Line", required = true)
            protected List<SourceDocuments.MovementOfGoods.StockMovement.Line> line;
            @XmlElement(name = "DocumentTotals", required = true)
            protected SourceDocuments.MovementOfGoods.StockMovement.DocumentTotals documentTotals;

            public String getDocumentNumber() {
                return documentNumber;
            }

            public void setDocumentNumber(String value) {
                this.documentNumber = value;
            }

            public SourceDocuments.MovementOfGoods.StockMovement.DocumentStatus getDocumentStatus() {
                return documentStatus;
            }

            public void setDocumentStatus(SourceDocuments.MovementOfGoods.StockMovement.DocumentStatus value) {
                this.documentStatus = value;
            }

            public String getHash() {
                return hash;
            }

            public void setHash(String value) {
                this.hash = value;
            }

            public String getHashControl() {
                return hashControl;
            }

            public void setHashControl(String value) {
                this.hashControl = value;
            }

            public Integer getPeriod() {
                return period;
            }

            public void setPeriod(Integer value) {
                this.period = value;
            }

            public XMLGregorianCalendar getMovementDate() {
                return movementDate;
            }

            public void setMovementDate(XMLGregorianCalendar value) {
                this.movementDate = value;
            }

            public String getMovementType() {
                return movementType;
            }

            public void setMovementType(String value) {
                this.movementType = value;
            }

            public XMLGregorianCalendar getSystemEntryDate() {
                return systemEntryDate;
            }

            public void setSystemEntryDate(XMLGregorianCalendar value) {
                this.systemEntryDate = value;
            }

            public String getTransactionID() {
                return transactionID;
            }

            public void setTransactionID(String value) {
                this.transactionID = value;
            }

            public String getCustomerID() {
                return customerID;
            }

            public void setCustomerID(String value) {
                this.customerID = value;
            }

            public String getSupplierID() {
                return supplierID;
            }

            public void setSupplierID(String value) {
                this.supplierID = value;
            }

            public String getSourceID() {
                return sourceID;
            }

            public void setSourceID(String value) {
                this.sourceID = value;
            }

            public String getEACCode() {
                return eacCode;
            }

            public void setEACCode(String value) {
                this.eacCode = value;
            }

            public String getMovementComments() {
                return movementComments;
            }

            public void setMovementComments(String value) {
                this.movementComments = value;
            }

            public ShippingPointStructure getShipTo() {
                return shipTo;
            }

            public void setShipTo(ShippingPointStructure value) {
                this.shipTo = value;
            }

            public ShippingPointStructure getShipFrom() {
                return shipFrom;
            }

            public void setShipFrom(ShippingPointStructure value) {
                this.shipFrom = value;
            }

            public XMLGregorianCalendar getMovementEndTime() {
                return movementEndTime;
            }

            public void setMovementEndTime(XMLGregorianCalendar value) {
                this.movementEndTime = value;
            }

            public XMLGregorianCalendar getMovementStartTime() {
                return movementStartTime;
            }

            public void setMovementStartTime(XMLGregorianCalendar value) {
                this.movementStartTime = value;
            }

            public String getATDocCodeID() {
                return atDocCodeID;
            }

            public void setATDocCodeID(String value) {
                this.atDocCodeID = value;
            }

            public List<SourceDocuments.MovementOfGoods.StockMovement.Line> getLine() {
                if (line == null) {
                    line = new ArrayList<SourceDocuments.MovementOfGoods.StockMovement.Line>();
                }
                return this.line;
            }

            public SourceDocuments.MovementOfGoods.StockMovement.DocumentTotals getDocumentTotals() {
                return documentTotals;
            }

            public void setDocumentTotals(SourceDocuments.MovementOfGoods.StockMovement.DocumentTotals value) {
                this.documentTotals = value;
            }

            @Override
            public String toString() {
                final StringBuilder buffer = new StringBuilder();
                append(null, buffer, null);
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
                    String theDocumentNumber;
                    theDocumentNumber = this.getDocumentNumber();
                    strategy.appendField(locator, this, "documentNumber", buffer, theDocumentNumber, (this.documentNumber != null));
                }
                {
                    SourceDocuments.MovementOfGoods.StockMovement.DocumentStatus theDocumentStatus;
                    theDocumentStatus = this.getDocumentStatus();
                    strategy.appendField(locator, this, "documentStatus", buffer, theDocumentStatus, (this.documentStatus != null));
                }
                {
                    String theHash;
                    theHash = this.getHash();
                    strategy.appendField(locator, this, "hash", buffer, theHash, (this.hash != null));
                }
                {
                    String theHashControl;
                    theHashControl = this.getHashControl();
                    strategy.appendField(locator, this, "hashControl", buffer, theHashControl, (this.hashControl != null));
                }
                {
                    Integer thePeriod;
                    thePeriod = this.getPeriod();
                    strategy.appendField(locator, this, "period", buffer, thePeriod, (this.period != null));
                }
                {
                    XMLGregorianCalendar theMovementDate;
                    theMovementDate = this.getMovementDate();
                    strategy.appendField(locator, this, "movementDate", buffer, theMovementDate, (this.movementDate != null));
                }
                {
                    String theMovementType;
                    theMovementType = this.getMovementType();
                    strategy.appendField(locator, this, "movementType", buffer, theMovementType, (this.movementType != null));
                }
                {
                    XMLGregorianCalendar theSystemEntryDate;
                    theSystemEntryDate = this.getSystemEntryDate();
                    strategy.appendField(locator, this, "systemEntryDate", buffer, theSystemEntryDate, (this.systemEntryDate != null));
                }
                {
                    String theTransactionID;
                    theTransactionID = this.getTransactionID();
                    strategy.appendField(locator, this, "transactionID", buffer, theTransactionID, (this.transactionID != null));
                }
                {
                    String theCustomerID;
                    theCustomerID = this.getCustomerID();
                    strategy.appendField(locator, this, "customerID", buffer, theCustomerID, (this.customerID != null));
                }
                {
                    ShippingPointStructure theShipTo;
                    theShipTo = this.getShipTo();
                    strategy.appendField(locator, this, "shipTo", buffer, theShipTo, (this.shipTo != null));
                }
                {
                    ShippingPointStructure theShipFrom;
                    theShipFrom = this.getShipFrom();
                    strategy.appendField(locator, this, "shipFrom", buffer, theShipFrom, (this.shipFrom != null));
                }
                {
                    XMLGregorianCalendar theMovementEndTime;
                    theMovementEndTime = this.getMovementEndTime();
                    strategy.appendField(locator, this, "movementEndTime", buffer, theMovementEndTime, (this.movementEndTime != null));
                }
                {
                    XMLGregorianCalendar theMovementStartTime;
                    theMovementStartTime = this.getMovementStartTime();
                    strategy.appendField(locator, this, "movementStartTime", buffer, theMovementStartTime, (this.movementStartTime != null));
                }
                {
                    String theATDocCodeID;
                    theATDocCodeID = this.getATDocCodeID();
                    strategy.appendField(locator, this, "atDocCodeID", buffer, theATDocCodeID, (this.atDocCodeID != null));
                }
                {
                    List<SourceDocuments.MovementOfGoods.StockMovement.Line> theLine;
                    theLine = (((this.line != null) && (!this.line.isEmpty())) ? this.getLine() : null);
                    strategy.appendField(locator, this, "line", buffer, theLine, ((this.line != null) && (!this.line.isEmpty())));
                }
                {
                    SourceDocuments.MovementOfGoods.StockMovement.DocumentTotals theDocumentTotals;
                    theDocumentTotals = this.getDocumentTotals();
                    strategy.appendField(locator, this, "documentTotals", buffer, theDocumentTotals, (this.documentTotals != null));
                }
                return buffer;
            }

            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {
                "workStatus",
                "workStatusDate",
                "reason",
                "sourceID"
            })
            public static class DocumentStatus implements ToString2
            {

                @XmlElement(name = "WorkStatus", required = true)
                protected String workStatus;
                @XmlElement(name = "WorkStatusDate", required = true)
                @XmlSchemaType(name = "dateTime")
                protected XMLGregorianCalendar workStatusDate;
                @XmlElement(name = "Reason")
                protected String reason;
                @XmlElement(name = "SourceID", required = true)
                protected String sourceID;

                public String getWorkStatus() {
                    return workStatus;
                }

                public void setWorkStatus(String value) {
                    this.workStatus = value;
                }

                public XMLGregorianCalendar getWorkStatusDate() {
                    return workStatusDate;
                }

                public void setWorkStatusDate(XMLGregorianCalendar value) {
                    this.workStatusDate = value;
                }

                public String getReason() {
                    return reason;
                }

                public void setReason(String value) {
                    this.reason = value;
                }

                public String getSourceID() {
                    return sourceID;
                }

                public void setSourceID(String value) {
                    this.sourceID = value;
                }

                @Override
                public String toString() {
                    final StringBuilder buffer = new StringBuilder();
                    append(null, buffer, null);
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
                        String theWorkStatus;
                        theWorkStatus = this.getWorkStatus();
                        strategy.appendField(locator, this, "workStatus", buffer, theWorkStatus, (this.workStatus != null));
                    }
                    {
                        XMLGregorianCalendar theWorkStatusDate;
                        theWorkStatusDate = this.getWorkStatusDate();
                        strategy.appendField(locator, this, "workStatusDate", buffer, theWorkStatusDate, (this.workStatusDate != null));
                    }
                    {
                        String theReason;
                        theReason = this.getReason();
                        strategy.appendField(locator, this, "reason", buffer, theReason, (this.reason != null));
                    }
                    {
                        String theSourceID;
                        theSourceID = this.getSourceID();
                        strategy.appendField(locator, this, "sourceID", buffer, theSourceID, (this.sourceID != null));
                    }
                    return buffer;
                }

            }

            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {
                "taxPayable",
                "netTotal",
                "grossTotal",
                "currency"
            })
            public static class DocumentTotals implements ToString2
            {

                @XmlElement(name = "TaxPayable", required = true)
                protected BigDecimal taxPayable;
                @XmlElement(name = "NetTotal", required = true)
                protected BigDecimal netTotal;
                @XmlElement(name = "GrossTotal", required = true)
                protected BigDecimal grossTotal;
                @XmlElement(name = "Currency")
                protected Currency currency;

                public BigDecimal getTaxPayable() {
                    return taxPayable;
                }

                public void setTaxPayable(BigDecimal value) {
                    this.taxPayable = value;
                }

                public BigDecimal getNetTotal() {
                    return netTotal;
                }

                public void setNetTotal(BigDecimal value) {
                    this.netTotal = value;
                }

                public BigDecimal getGrossTotal() {
                    return grossTotal;
                }

                public void setGrossTotal(BigDecimal value) {
                    this.grossTotal = value;
                }

                public Currency getCurrency() {
                    return currency;
                }

                public void setCurrency(Currency value) {
                    this.currency = value;
                }

                @Override
                public String toString() {
                    final StringBuilder buffer = new StringBuilder();
                    append(null, buffer, null);
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
                        BigDecimal theTaxPayable;
                        theTaxPayable = this.getTaxPayable();
                        strategy.appendField(locator, this, "taxPayable", buffer, theTaxPayable, (this.taxPayable != null));
                    }
                    {
                        BigDecimal theNetTotal;
                        theNetTotal = this.getNetTotal();
                        strategy.appendField(locator, this, "netTotal", buffer, theNetTotal, (this.netTotal != null));
                    }
                    {
                        BigDecimal theGrossTotal;
                        theGrossTotal = this.getGrossTotal();
                        strategy.appendField(locator, this, "grossTotal", buffer, theGrossTotal, (this.grossTotal != null));
                    }
                    {
                        Currency theCurrency;
                        theCurrency = this.getCurrency();
                        strategy.appendField(locator, this, "currency", buffer, theCurrency, (this.currency != null));
                    }
                    return buffer;
                }

            }

            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {
                "lineNumber",
                "orderReferences",
                "productCode",
                "productDescription",
                "quantity",
                "unitOfMeasure",
                "unitPrice",
                "taxPointDate",
                "description",
                "debitAmount",
                "creditAmount",
                "tax",
                "taxExemptionReason",
                "settlementAmount"
            })
            public static class Line implements ToString2
            {

                @XmlElement(name = "LineNumber", required = true)
                @XmlSchemaType(name = "nonNegativeInteger")
                protected BigInteger lineNumber;
                @XmlElement(name = "OrderReferences")
                protected List<OrderReferences> orderReferences;
                @XmlElement(name = "ProductCode", required = true)
                protected String productCode;
                @XmlElement(name = "ProductDescription", required = true)
                protected String productDescription;
                @XmlElement(name = "Quantity", required = true)
                protected BigDecimal quantity;
                @XmlElement(name = "UnitOfMeasure", required = true)
                protected String unitOfMeasure;
                @XmlElement(name = "UnitPrice", required = true)
                protected BigDecimal unitPrice;
                @XmlElement(name = "TaxPointDate", required = true)
                @XmlSchemaType(name = "date")
                protected XMLGregorianCalendar taxPointDate;
                @XmlElement(name = "Description", required = true)
                protected String description;
                @XmlElement(name = "DebitAmount")
                protected BigDecimal debitAmount;
                @XmlElement(name = "CreditAmount")
                protected BigDecimal creditAmount;
                @XmlElement(name = "Tax", required = true)
                protected Tax tax;
                @XmlElement(name = "TaxExemptionReason")
                protected String taxExemptionReason;
                @XmlElement(name = "SettlementAmount")
                protected BigDecimal settlementAmount;

                public BigInteger getLineNumber() {
                    return lineNumber;
                }

                public void setLineNumber(BigInteger value) {
                    this.lineNumber = value;
                }

                public List<OrderReferences> getOrderReferences() {
                    if (orderReferences == null) {
                        orderReferences = new ArrayList<OrderReferences>();
                    }
                    return this.orderReferences;
                }

                public String getProductCode() {
                    return productCode;
                }

                public void setProductCode(String value) {
                    this.productCode = value;
                }

                public String getProductDescription() {
                    return productDescription;
                }

                public void setProductDescription(String value) {
                    this.productDescription = value;
                }

                public BigDecimal getQuantity() {
                    return quantity;
                }

                public void setQuantity(BigDecimal value) {
                    this.quantity = value;
                }

                public String getUnitOfMeasure() {
                    return unitOfMeasure;
                }

                public void setUnitOfMeasure(String value) {
                    this.unitOfMeasure = value;
                }

                public BigDecimal getUnitPrice() {
                    return unitPrice;
                }

                public void setUnitPrice(BigDecimal value) {
                    this.unitPrice = value;
                }

                public XMLGregorianCalendar getTaxPointDate() {
                    return taxPointDate;
                }

                public void setTaxPointDate(XMLGregorianCalendar value) {
                    this.taxPointDate = value;
                }

                public String getDescription() {
                    return description;
                }

                public void setDescription(String value) {
                    this.description = value;
                }

                public BigDecimal getDebitAmount() {
                    return debitAmount;
                }

                public void setDebitAmount(BigDecimal value) {
                    this.debitAmount = value;
                }

                public BigDecimal getCreditAmount() {
                    return creditAmount;
                }

                public void setCreditAmount(BigDecimal value) {
                    this.creditAmount = value;
                }

                public Tax getTax() {
                    return tax;
                }

                public void setTax(Tax value) {
                    this.tax = value;
                }

                public String getTaxExemptionReason() {
                    return taxExemptionReason;
                }

                public void setTaxExemptionReason(String value) {
                    this.taxExemptionReason = value;
                }

                public BigDecimal getSettlementAmount() {
                    return settlementAmount;
                }

                public void setSettlementAmount(BigDecimal value) {
                    this.settlementAmount = value;
                }

                @Override
                public String toString() {
                    final StringBuilder buffer = new StringBuilder();
                    append(null, buffer, null);
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
                        BigInteger theLineNumber;
                        theLineNumber = this.getLineNumber();
                        strategy.appendField(locator, this, "lineNumber", buffer, theLineNumber, (this.lineNumber != null));
                    }
                    {
                        List<OrderReferences> theOrderReferences;
                        theOrderReferences = (((this.orderReferences != null) && (!this.orderReferences.isEmpty())) ? this.getOrderReferences() : null);
                        strategy.appendField(locator, this, "orderReferences", buffer, theOrderReferences, ((this.orderReferences != null) && (!this.orderReferences.isEmpty())));
                    }
                    {
                        String theProductCode;
                        theProductCode = this.getProductCode();
                        strategy.appendField(locator, this, "productCode", buffer, theProductCode, (this.productCode != null));
                    }
                    {
                        String theProductDescription;
                        theProductDescription = this.getProductDescription();
                        strategy.appendField(locator, this, "productDescription", buffer, theProductDescription, (this.productDescription != null));
                    }
                    {
                        BigDecimal theQuantity;
                        theQuantity = this.getQuantity();
                        strategy.appendField(locator, this, "quantity", buffer, theQuantity, (this.quantity != null));
                    }
                    {
                        String theUnitOfMeasure;
                        theUnitOfMeasure = this.getUnitOfMeasure();
                        strategy.appendField(locator, this, "unitOfMeasure", buffer, theUnitOfMeasure, (this.unitOfMeasure != null));
                    }
                    {
                        BigDecimal theUnitPrice;
                        theUnitPrice = this.getUnitPrice();
                        strategy.appendField(locator, this, "unitPrice", buffer, theUnitPrice, (this.unitPrice != null));
                    }
                    {
                        XMLGregorianCalendar theTaxPointDate;
                        theTaxPointDate = this.getTaxPointDate();
                        strategy.appendField(locator, this, "taxPointDate", buffer, theTaxPointDate, (this.taxPointDate != null));
                    }
                    {
                        String theDescription;
                        theDescription = this.getDescription();
                        strategy.appendField(locator, this, "description", buffer, theDescription, (this.description != null));
                    }
                    {
                        BigDecimal theDebitAmount;
                        theDebitAmount = this.getDebitAmount();
                        strategy.appendField(locator, this, "debitAmount", buffer, theDebitAmount, (this.debitAmount != null));
                    }
                    {
                        BigDecimal theCreditAmount;
                        theCreditAmount = this.getCreditAmount();
                        strategy.appendField(locator, this, "creditAmount", buffer, theCreditAmount, (this.creditAmount != null));
                    }
                    {
                        Tax theTax;
                        theTax = this.getTax();
                        strategy.appendField(locator, this, "tax", buffer, theTax, (this.tax != null));
                    }
                    {
                        String theTaxExemptionReason;
                        theTaxExemptionReason = this.getTaxExemptionReason();
                        strategy.appendField(locator, this, "taxExemptionReason", buffer, theTaxExemptionReason, (this.taxExemptionReason != null));
                    }
                    {
                        BigDecimal theSettlementAmount;
                        theSettlementAmount = this.getSettlementAmount();
                        strategy.appendField(locator, this, "settlementAmount", buffer, theSettlementAmount, (this.settlementAmount != null));
                    }
                    return buffer;
                }

            }

        }

    }

}