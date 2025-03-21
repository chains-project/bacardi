public static class LogMessageEncoder extends EncoderBase<LogMessage> {

    private TTransport framedTransport;
    private TProtocol protocol;
    private OutputStream os;

    @Override
    public void init(OutputStream os) {
        this.os = os;
        final int bufferCapacity = 10;
        framedTransport = new TFastFramedTransport(new TIOStreamTransport(os), bufferCapacity);
        protocol = new TBinaryProtocol(framedTransport);
    }

    @Override
    public void doEncode(LogMessage logMessage) throws IOException {
        try {
            logMessage.write(protocol);
            framedTransport.flush();
        } catch (TException e) {
            throw new IOException(e);
        }
    }

    public byte[] footerBytes() {
        return new byte[0]; // Implement footerBytes method as required by the new API
    }

    @Override
    public void close() throws IOException {
        framedTransport.close();
    }
}
