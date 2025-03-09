public ModelRepresenter() {
        this.representers.put(Xpp3Dom.class, new RepresentXpp3Dom());
        Represent stringRepresenter = this.representers.get(String.class);
        this.representers.put(Boolean.class, stringRepresenter);
        this.multiRepresenters.put(Number.class, stringRepresenter);
        this.multiRepresenters.put(Date.class, stringRepresenter);
        this.multiRepresenters.put(Enum.class, stringRepresenter);
        this.multiRepresenters.put(Calendar.class, stringRepresenter);
    }