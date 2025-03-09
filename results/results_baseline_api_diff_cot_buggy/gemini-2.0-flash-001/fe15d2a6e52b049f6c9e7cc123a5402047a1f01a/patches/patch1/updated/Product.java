@Override
public String toString() {
    final ToStringStrategy2 strategy = JAXBToStringStrategy.getInstance();
    final StringBuilder buffer = new StringBuilder();
    append(null, buffer, strategy);
    return buffer.toString();
}