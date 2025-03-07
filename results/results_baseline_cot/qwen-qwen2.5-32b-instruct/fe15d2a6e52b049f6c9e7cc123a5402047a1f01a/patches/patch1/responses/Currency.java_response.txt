package com.premiumminds.billy.portugal.services.export.saftpt.v1_02_01.schema;

import java.math.BigDecimal;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import org.jvnet.jaxb2_commons.lang.JAXBToStringStrategy;
import org.jvnet.jaxb2_commons.lang.ToString2;
import org.jvnet.jaxb2_commons.lang.ToStringStrategy2;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Currency", propOrder = {
    "currencyCode",
    "currencyAmount",
    "exchangeRate"
})
public class Currency implements ToString2 {

    @XmlElement(name = "CurrencyCode", required = true)
    protected String currencyCode;
    @XmlElement(name = "CurrencyAmount", required = true)
    protected BigDecimal currencyAmount;
    @XmlElement(name = "ExchangeRate")
    protected BigDecimal exchangeRate;

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String value) {
        this.currencyCode = value;
    }

    public BigDecimal getCurrencyAmount() {
        return currencyAmount;
    }

    public void setCurrencyAmount(BigDecimal value) {
        this.currencyAmount = value;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal value) {
        this.exchangeRate = value;
    }

    @Override
    public String toString() {
        final ToStringStrategy2 strategy = new JAXBToStringStrategy(); // Direct instantiation instead of getInstance()
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
            String theCurrencyCode;
            theCurrencyCode = this.getCurrencyCode();
            strategy.appendField(locator, this, "currencyCode", buffer, theCurrencyCode, (this.currencyCode != null));
        }
        {
            BigDecimal theCurrencyAmount;
            theCurrencyAmount = this.getCurrencyAmount();
            strategy.appendField(locator, this, "currencyAmount", buffer, theCurrencyAmount, (this.currencyAmount != null));
        }
        {
            BigDecimal theExchangeRate;
            theExchangeRate = this.getExchangeRate();
            strategy.appendField(locator, this, "exchangeRate", buffer, theExchangeRate, (this.exchangeRate != null));
        }
        return buffer;
    }
}