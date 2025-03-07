package com.premiumminds.billy.portugal.services.export.saftpt.v1_03_01.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import org.jvnet.jaxb2_commons.lang.JAXBToStringStrategy;
import org.jvnet.jaxb2_commons.lang.ToString2;
import org.jvnet.jaxb2_commons.lang.ToStringStrategy2;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SpecialRegimes", propOrder = {
    "selfBillingIndicator",
    "cashVATSchemeIndicator",
    "thirdPartiesBillingIndicator"
})
public class SpecialRegimes implements ToString2 {

    @XmlElement(name = "SelfBillingIndicator")
    protected int selfBillingIndicator;
    @XmlElement(name = "CashVATSchemeIndicator")
    protected int cashVATSchemeIndicator;
    @XmlElement(name = "ThirdPartiesBillingIndicator")
    protected int thirdPartiesBillingIndicator;

    public int getSelfBillingIndicator() {
        return selfBillingIndicator;
    }

    public void setSelfBillingIndicator(int value) {
        this.selfBillingIndicator = value;
    }

    public int getCashVATSchemeIndicator() {
        return cashVATSchemeIndicator;
    }

    public void setCashVATSchemeIndicator(int value) {
        this.cashVATSchemeIndicator = value;
    }

    public int getThirdPartiesBillingIndicator() {
        return thirdPartiesBillingIndicator;
    }

    public void setThirdPartiesBillingIndicator(int value) {
        this.thirdPartiesBillingIndicator = value;
    }

    @Override
    public String toString() {
        final ToStringStrategy2 strategy = new JAXBToStringStrategy(); // Direct instantiation
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
            int theSelfBillingIndicator;
            theSelfBillingIndicator = this.getSelfBillingIndicator();
            strategy.appendField(locator, this, "selfBillingIndicator", buffer, theSelfBillingIndicator, true);
        }
        {
            int theCashVATSchemeIndicator;
            theCashVATSchemeIndicator = this.getCashVATSchemeIndicator();
            strategy.appendField(locator, this, "cashVATSchemeIndicator", buffer, theCashVATSchemeIndicator, true);
        }
        {
            int theThirdPartiesBillingIndicator;
            theThirdPartiesBillingIndicator = this.getThirdPartiesBillingIndicator();
            strategy.appendField(locator, this, "thirdPartiesBillingIndicator", buffer, theThirdPartiesBillingIndicator, true);
        }
        return buffer;
    }
}