package com.premiumminds.billy.portugal.services.export.saftpt.v1_03_01.schema;

import java.math.BigDecimal;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.jvnet.jaxb2_commons.lang.ToString2;
import org.jvnet.jaxb2_commons.lang.ToStringStrategy2;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "accountID",
    "accountDescription",
    "openingDebitBalance",
    "openingCreditBalance",
    "closingDebitBalance",
    "closingCreditBalance",
    "groupingCategory",
    "groupingCode"
})
@XmlRootElement(name = "GeneralLedger")
public class GeneralLedger implements ToString2 {

    @XmlElement(name = "AccountID", required = true)
    protected String accountID;
    @XmlElement(name = "AccountDescription", required = true)
    protected String accountDescription;
    @XmlElement(name = "OpeningDebitBalance", required = true)
    protected BigDecimal openingDebitBalance;
    @XmlElement(name = "OpeningCreditBalance", required = true)
    protected BigDecimal openingCreditBalance;
    @XmlElement(name = "ClosingDebitBalance", required = true)
    protected BigDecimal closingDebitBalance;
    @XmlElement(name = "ClosingCreditBalance", required = true)
    protected BigDecimal closingCreditBalance;
    @XmlElement(name = "GroupingCategory", required = true)
    protected String groupingCategory;
    @XmlElement(name = "GroupingCode")
    protected String groupingCode;

    // ... (getters and setters remain unchanged)

    @Override
    public String toString() {
        final ToStringStrategy2 strategy = new ToStringStrategy2(); // Assuming a default constructor is available
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
            String theAccountID;
            theAccountID = this.getAccountID();
            strategy.appendField(locator, this, "accountID", buffer, theAccountID, (this.accountID != null));
        }
        {
            String theAccountDescription;
            theAccountDescription = this.getAccountDescription();
            strategy.appendField(locator, this, "accountDescription", buffer, theAccountDescription, (this.accountDescription != null));
        }
        {
            BigDecimal theOpeningDebitBalance;
            theOpeningDebitBalance = this.getOpeningDebitBalance();
            strategy.appendField(locator, this, "openingDebitBalance", buffer, theOpeningDebitBalance, (this.openingDebitBalance != null));
        }
        {
            BigDecimal theOpeningCreditBalance;
            theOpeningCreditBalance = this.getOpeningCreditBalance();
            strategy.appendField(locator, this, "openingCreditBalance", buffer, theOpeningCreditBalance, (this.openingCreditBalance != null));
        }
        {
            BigDecimal theClosingDebitBalance;
            theClosingDebitBalance = this.getClosingDebitBalance();
            strategy.appendField(locator, this, "closingDebitBalance", buffer, theClosingDebitBalance, (this.closingDebitBalance != null));
        }
        {
            BigDecimal theClosingCreditBalance;
            theClosingCreditBalance = this.getClosingCreditBalance();
            strategy.appendField(locator, this, "closingCreditBalance", buffer, theClosingCreditBalance, (this.closingCreditBalance != null));
        }
        {
            String theGroupingCategory;
            theGroupingCategory = this.getGroupingCategory();
            strategy.appendField(locator, this, "groupingCategory", buffer, theGroupingCategory, (this.groupingCategory != null));
        }
        {
            String theGroupingCode;
            theGroupingCode = this.getGroupingCode();
            strategy.appendField(locator, this, "groupingCode", buffer, theGroupingCode, (this.groupingCode != null));
        }
        return buffer;
    }
}