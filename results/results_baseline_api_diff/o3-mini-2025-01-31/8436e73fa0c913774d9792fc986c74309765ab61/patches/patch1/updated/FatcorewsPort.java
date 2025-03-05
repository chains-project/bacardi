package com.premiumminds.billy.portugal.webservices.documents;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;

@WebService(name = "fatcorewsPort", targetNamespace = "http://factemi.at.min_financas.pt/documents")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@XmlSeeAlso({
    ObjectFactory.class
})
public interface FatcorewsPort {

    @WebMethod(operationName = "RegisterInvoice")
    @WebResult(name = "RegisterInvoiceResponse", targetNamespace = "http://factemi.at.min_financas.pt/documents", partName = "RegisterInvoiceResponse")
    public RegisterInvoiceResponse registerInvoice(
        @WebParam(name = "RegisterInvoiceRequest", targetNamespace = "http://factemi.at.min_financas.pt/documents", partName = "RegisterInvoiceRequest")
        RegisterInvoiceRequest registerInvoiceRequest);

    @WebMethod(operationName = "ChangeInvoiceStatus")
    @WebResult(name = "ChangeInvoiceStatusResponse", targetNamespace = "http://factemi.at.min_financas.pt/documents", partName = "ChangeInvoiceStatusResponse")
    public ChangeInvoiceStatusResponse changeInvoiceStatus(
        @WebParam(name = "ChangeInvoiceStatusRequest", targetNamespace = "http://factemi.at.min_financas.pt/documents", partName = "ChangeInvoiceStatusRequest")
        ChangeInvoiceStatusRequest changeInvoiceStatusRequest);

    @WebMethod(operationName = "DeleteInvoice")
    @WebResult(name = "DeleteInvoiceResponse", targetNamespace = "http://factemi.at.min_financas.pt/documents", partName = "DeleteInvoiceResponse")
    public DeleteInvoiceResponse deleteInvoice(
        @WebParam(name = "DeleteInvoiceRequest", targetNamespace = "http://factemi.at.min_financas.pt/documents", partName = "DeleteInvoiceRequest")
        DeleteInvoiceRequest deleteInvoiceRequest);

    @WebMethod(operationName = "RegisterWork")
    @WebResult(name = "RegisterWorkResponse", targetNamespace = "http://factemi.at.min_financas.pt/documents", partName = "RegisterWorkResponse")
    public RegisterWorkResponse registerWork(
        @WebParam(name = "RegisterWorkRequest", targetNamespace = "http://factemi.at.min_financas.pt/documents", partName = "RegisterWorkRequest")
        RegisterWorkRequest registerWorkRequest);

    @WebMethod(operationName = "ChangeWorkStatus")
    @WebResult(name = "ChangeWorkStatusResponse", targetNamespace = "http://factemi.at.min_financas.pt/documents", partName = "ChangeWorkStatusResponse")
    public ChangeWorkStatusResponse changeWorkStatus(
        @WebParam(name = "ChangeWorkStatusRequest", targetNamespace = "http://factemi.at.min_financas.pt/documents", partName = "ChangeWorkStatusRequest")
        ChangeWorkStatusRequest changeWorkStatusRequest);

    @WebMethod(operationName = "DeleteWork")
    @WebResult(name = "DeleteWorkResponse", targetNamespace = "http://factemi.at.min_financas.pt/documents", partName = "DeleteWorkResponse")
    public DeleteWorkResponse deleteWork(
        @WebParam(name = "DeleteWorkRequest", targetNamespace = "http://factemi.at.min_financas.pt/documents", partName = "DeleteWorkRequest")
        DeleteWorkRequest deleteWorkRequest);

    @WebMethod(operationName = "RegisterPayment")
    @WebResult(name = "RegisterPaymentResponse", targetNamespace = "http://factemi.at.min_financas.pt/documents", partName = "RegisterPaymentResponse")
    public RegisterPaymentResponse registerPayment(
        @WebParam(name = "RegisterPaymentRequest", targetNamespace = "http://factemi.at.min_financas.pt/documents", partName = "RegisterPaymentRequest")
        RegisterPaymentRequest registerPaymentRequest);

    @WebMethod(operationName = "ChangePaymentStatus")
    @WebResult(name = "ChangePaymentStatusResponse", targetNamespace = "http://factemi.at.min_financas.pt/documents", partName = "ChangePaymentStatusResponse")
    public ChangePaymentStatusResponse changePaymentStatus(
        @WebParam(name = "ChangePaymentStatusRequest", targetNamespace = "http://factemi.at.min_financas.pt/documents", partName = "ChangePaymentStatusRequest")
        ChangePaymentStatusRequest changePaymentStatusRequest);

    @WebMethod(operationName = "DeletePayment")
    @WebResult(name = "DeletePaymentResponse", targetNamespace = "http://factemi.at.min_financas.pt/documents", partName = "DeletePaymentResponse")
    public DeletePaymentResponse deletePayment(
        @WebParam(name = "DeletePaymentRequest", targetNamespace = "http://factemi.at.min_financas.pt/documents", partName = "DeletePaymentRequest")
        DeletePaymentRequest deletePaymentRequest);
}