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

    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        append(null, buffer, null);
        return buffer.toString();
    }

    public StringBuilder append(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
        strategy.appendStart(locator, this, buffer);
        appendFields(locator, buffer, strategy);
        strategy.appendEnd(locator, this, buffer);
        return buffer;
    }

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

        public String toString() {
            final StringBuilder buffer = new StringBuilder();
            append(null, buffer, null);
            return buffer.toString();
        }

        public StringBuilder append(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
            strategy.appendStart(locator, this, buffer);
            appendFields(locator, buffer, strategy);
            strategy.appendEnd(locator, this, buffer);
            return buffer;
        }

        public StringBuilder appendFields(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
            {
                BigInteger theNumberOfMovementLines;
                theNumberOfMovementLines = this.getNumberOfMovementLines();
                strategy.appendField(locator, this, "numberOfMovementLines", buffer, theNumberOfMovementLines, (this.numberOfMovementLines!= null));
            }
            {
                BigDecimal theTotalQuantityIssued;
                theTotalQuantityIssued = this.getTotalQuantityIssued();
                strategy.appendField(locator, this, "totalQuantityIssued", buffer, theTotalQuantityIssued, (this.totalQuantityIssued!= null));
            }
            {
                List<SourceDocuments.MovementOfGoods.StockMovement> theStockMovement;
                theStockMovement = (((this.stockMovement!= null)&&(!this.stockMovement.isEmpty()))?this.getStockMovement():null);
                strategy.appendField(locator, this, "stockMovement", buffer, theStockMovement, ((this.stockMovement!= null)&&(!this.stockMovement.isEmpty())));
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
            "sourceID",
            "eacCode",
            "movementComments",
            "shipTo",
            "shipFrom",
            "movementEndTime",
            "movementStartTime",
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

            public String toString() {
                final StringBuilder buffer = new StringBuilder();
                append(null, buffer, null);
                return buffer.toString();
            }

            public StringBuilder append(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
                strategy.appendStart(locator, this, buffer);
                appendFields(locator, buffer, strategy);
                strategy.appendEnd(locator, this, buffer);
                return buffer;
            }

            public StringBuilder appendFields(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
                {
                    String theDocumentNumber;
                    theDocumentNumber = this.getDocumentNumber();
                    strategy.appendField(locator, this, "documentNumber", buffer, theDocumentNumber, (this.documentNumber!= null));
                }
                {
                    SourceDocuments.MovementOfGoods.StockMovement.DocumentStatus theDocumentStatus;
                    theDocumentStatus = this.getDocumentStatus();
                    strategy.appendField(locator, this, "documentStatus", buffer, theDocumentStatus, (this.documentStatus!= null));
                }
                {
                    String theHash;
                    theHash = this.getHash();
                    strategy.appendField(locator, this, "hash", buffer, theHash, (this.hash!= null));
                }
                {
                    String theHashControl;
                    theHashControl = this.getHashControl();
                    strategy.appendField(locator, this, "hashControl", buffer, theHashControl, (this.hashControl!= null));
                }
                {
                    Integer thePeriod;
                    thePeriod = this.getPeriod();
                    strategy.appendField(locator, this, "period", buffer, thePeriod, (this.period!= null));
                }
                {
                    XMLGregorianCalendar theMovementDate;
                    theMovementDate = this.getMovementDate();
                    strategy.appendField(locator, this, "movementDate", buffer, theMovementDate, (this.movementDate!= null));
                }
                {
                    String theMovementType;
                    theMovementType = this.getMovementType();
                    strategy.appendField(locator, this, "movementType", buffer, theMovementType, (this.movementType!= null));
                }
                {
                    String theSourceID;
                    theSourceID = this.getSourceID();
                    strategy.appendField(locator, this, "sourceID", buffer, theSourceID, (this.sourceID!= null));
                }
                {
                    String theEACCode;
                    theEACCode = this.getEACCode();
                    strategy.appendField(locator, this, "eacCode", buffer, theEACCode, (this.eacCode!= null));
                }
                {
                    String theMovementComments;
                    theMovementComments = this.getMovementComments();
                    strategy.appendField(locator, this, "movementComments", buffer, theMovementComments, (this.movementComments!= null));
                }
                {
                    ShippingPointStructure theShipTo;
                    theShipTo = this.getShipTo();
                    strategy.appendField(locator, this, "shipTo", buffer, theShipTo, (this.shipTo!= null));
                }
                {
                    ShippingPointStructure theShipFrom;
                    theShipFrom = this.getShipFrom();
                    strategy.appendField(locator, this, "shipFrom", buffer, theShipFrom, (this.shipFrom!= null));
                }
                {
                    XMLGregorianCalendar theMovementEndTime;
                    theMovementEndTime = this.getMovementEndTime();
                    strategy.appendField(locator, this, "movementEndTime", buffer, theMovementEndTime, (this.movementEndTime!= null));
                }
                {
                    XMLGregorianCalendar theMovementStartTime;
                    theMovementStartTime = this.getMovementStartTime();
                    strategy.appendField(locator, this, "movementStartTime", buffer, theMovementStartTime, (this.movementStartTime!= null));
                }
                {
                    List<SourceDocuments.MovementOfGoods.StockMovement.Line> theLine;
                    theLine = (((this.line!= null)&&(!this.line.isEmpty()))?this.getLine():null);
                    strategy.appendField(locator, this, "line", buffer, theLine, ((this.line!= null)&&(!this.line.isEmpty())));
                }
                {
                    SourceDocuments.MovementOfGoods.StockMovement.DocumentTotals theDocumentTotals;
                    theDocumentTotals = this.getDocumentTotals();
                    strategy.appendField(locator, this, "documentTotals", buffer, theDocumentTotals, (this.documentTotals!= null));
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

                public String toString() {
                    final StringBuilder buffer = new StringBuilder();
                    append(null, buffer, null);
                    return buffer.toString();
                }

                public StringBuilder append(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
                    strategy.appendStart(locator, this, buffer);
                    appendFields(locator, buffer, strategy);
                    strategy.appendEnd(locator, this, buffer);
                    return buffer;
                }

                public StringBuilder appendFields(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
                    {
                        String theWorkStatus;
                        theWorkStatus = this.getWorkStatus();
                        strategy.appendField(locator, this, "workStatus", buffer, theWorkStatus, (this.workStatus!= null));
                    }
                    {
                        XMLGregorianCalendar theWorkStatusDate;
                        theWorkStatusDate = this.getWorkStatusDate();
                        strategy.appendField(locator, this, "workStatusDate", buffer, theWorkStatusDate, (this.workStatusDate!= null));
                    }
                    {
                        String theReason;
                        theReason = this.getReason();
                        strategy.appendField(locator, this, "reason", buffer, theReason, (this.reason!= null));
                    }
                    {
                        String theSourceID;
                        theSourceID = this.getSourceID();
                        strategy.appendField(locator, this, "sourceID", buffer, theSourceID, (this.sourceID!= null));
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

                public String toString() {
                    final StringBuilder buffer = new StringBuilder();
                    append(null, buffer, null);
                    return buffer.toString();
                }

                public StringBuilder append(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
                    strategy.appendStart(locator, this, buffer);
                    appendFields(locator, buffer, strategy);
                    strategy.appendEnd(locator, this, buffer);
                    return buffer;
                }

                public StringBuilder appendFields(ObjectLocator locator, StringBuilder buffer, ToStringStrategy2 strategy) {
                    {
                        BigDecimal theTaxPayable;
                        theTaxPayable = this.getTaxPayable();
                        strategy.appendField(locator, this, "taxPayable", buffer, theTaxPayable, (this.taxPayable!= null));
                    }
                    {
                        BigDecimal theNetTotal;
                        theNetTotal = this.getNetTotal();
                        strategy.appendField(locator, this, "netTotal", buffer, theNetTotal, (this.netTotal!= null));
                    }
                    {
                        BigDecimal theGrossTotal;
                        theGrossTotal = this.getGrossTotal();
                        strategy.appendField(locator, this, "grossTotal", buffer, theGrossTotal, (this.grossTotal!= null));
                    }
                    {
                        Currency theCurrency;
                        theCurrency = this.getCurrency();
                        strategy.appendField(locator, this, "currency", buffer, theCurrency, (this.currency!= null));
                    }
                    return buffer;
                }

            }

        }

    }

}