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
@XmlType(name = "SupplierAddressStructure", propOrder = {
    "buildingNumber",
    "streetName",
    "addressDetail",
    "city",
    "postalCode",
    "region",
    "country"
})
public class SupplierAddressStructure implements ToString2
{

    @XmlElement(name = "BuildingNumber")
    protected String buildingNumber;
    @XmlElement(name = "StreetName")
    protected String streetName;
    @XmlElement(name = "AddressDetail", required = true)
    protected String addressDetail;
    @XmlElement(name = "City", required = true)
    protected String city;
    @XmlElement(name = "PostalCode", required = true)
    protected String postalCode;
    @XmlElement(name = "Region")
    protected String region;
    @XmlElement(name = "Country", required = true)
    protected String country;

    // ... (getters and setters remain unchanged)

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
            String theBuildingNumber;
            theBuildingNumber = this.getBuildingNumber();
            strategy.appendField(locator, this, "buildingNumber", buffer, theBuildingNumber, (this.buildingNumber!= null));
        }
        {
            String theStreetName;
            theStreetName = this.getStreetName();
            strategy.appendField(locator, this, "streetName", buffer, theStreetName, (this.streetName!= null));
        }
        {
            String theAddressDetail;
            theAddressDetail = this.getAddressDetail();
            strategy.appendField(locator, this, "addressDetail", buffer, theAddressDetail, (this.addressDetail!= null));
        }
        {
            String theCity;
            theCity = this.getCity();
            strategy.appendField(locator, this, "city", buffer, theCity, (this.city!= null));
        }
        {
            String thePostalCode;
            thePostalCode = this.getPostalCode();
            strategy.appendField(locator, this, "postalCode", buffer, thePostalCode, (this.postalCode!= null));
        }
        {
            String theRegion;
            theRegion = this.getRegion();
            strategy.appendField(locator, this, "region", buffer, theRegion, (this.region!= null));
        }
        {
            String theCountry;
            theCountry = this.getCountry();
            strategy.appendField(locator, this, "country", buffer, theCountry, (this.country!= null));
        }
        return buffer;
    }
}