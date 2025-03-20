/*
<repair_strategy>
1. The error occurs because the getInstance() method has been removed from the updated JAXBToStringStrategy API.
2. The fix replaces the call to getInstance() with the INSTANCE constant provided by JAXBToStringStrategy.
3. This minimal change adapts the client code to the new dependency version while preserving all functionality.
</repair_strategy>
*/
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
public class Currency implements ToString2
{

    @XmlElement(name = "CurrencyCode", required = true)
    protected String currencyCode;
    @XmlElement(name = "CurrencyAmount", required = true)
    protected BigDecimal currencyAmount;
    @XmlElement(name = "ExchangeRate")
    protected BigDecimal exchangeRate;

    /**
     * Gets the value of the currencyCode property.
     * 
     * @return possible object is {@link String }
     */
    public String getCurrencyCode() {
        return currencyCode;
    }

    /**
     * Sets the value of the currencyCode property.
     * 
     * @param value allowed object is {@link String }
     */
    public void setCurrencyCode(String value) {
        this.currencyCode = value;
    }

    /**
     * Gets the value of the currencyAmount property.
     * 
     * @return possible object is {@link BigDecimal }
     */
    public BigDecimal getCurrencyAmount() {
        return currencyAmount;
    }

    /**
     * Sets the value of the currencyAmount property.
     * 
     * @param value allowed object is {@link BigDecimal }
     */
    public void setCurrencyAmount(BigDecimal value) {
        this.currencyAmount = value;
    }

    /**
     * Gets the value of the exchangeRate property.
     * 
     * @return possible object is {@link BigDecimal }
     */
    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    /**
     * Sets the value of the exchangeRate property.
     * 
     * @param value allowed object is {@link BigDecimal }
     */
    public void setExchangeRate(BigDecimal value) {
        this.exchangeRate = value;
    }

    @Override
    public String toString() {
        final ToStringStrategy2 strategy = JAXBToStringStrategy.INSTANCE;
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