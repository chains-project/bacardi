import org.apache.thrift.transport.TFramedTransport;
private final TFramedTransport framedTransport;
this.framedTransport = new TFramedTransport(new TIOStreamTransport(this
        .byteOffsetInputStream), maxMessageSize);