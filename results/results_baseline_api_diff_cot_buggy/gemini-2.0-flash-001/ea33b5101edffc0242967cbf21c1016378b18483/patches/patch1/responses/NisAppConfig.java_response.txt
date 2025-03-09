String[] locations = prop.getProperty("flyway.locations").split(",");
        org.flywaydb.core.api.Location[] locationArray = new org.flywaydb.core.api.Location[locations.length];
        for (int i = 0; i < locations.length; i++) {
            locationArray[i] = new org.flywaydb.core.api.Location(locations[i]);
        }
        configuration.setLocations(locationArray);