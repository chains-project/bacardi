package com.premiumminds.billy.portugal.webservices.series;

import java.math.BigInteger;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.Action;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "SeriesWS", targetNamespace = "http://at.gov.pt/")
@XmlSeeAlso({
    ObjectFactory.class
})
public interface SeriesWS {

    @WebMethod
    @WebResult(name = "registarSerieResp", targetNamespace = "")
    @RequestWrapper(localName = "registarSerie", targetNamespace = "http://at.gov.pt/", className = "com.premiumminds.billy.portugal.webservices.series.RegistarSerie")
    @ResponseWrapper(localName = "registarSerieResponse", targetNamespace = "http://at.gov.pt/", className = "com.premiumminds.billy.portugal.webservices.series.RegistarSerieResponse")
    @Action(input = "http://at.gov.pt/SeriesWS/registarSerieRequest", output = "http://at.gov.pt/SeriesWS/registarSerieResponse")
    public SeriesResp registarSerie(
        @WebParam(name = "serie", targetNamespace = "")
        String serie,
        @WebParam(name = "tipoSerie", targetNamespace = "")
        String tipoSerie,
        @WebParam(name = "classeDoc", targetNamespace = "")
        String classeDoc,
        @WebParam(name = "tipoDoc", targetNamespace = "")
        String tipoDoc,
        @WebParam(name = "numInicialSeq", targetNamespace = "")
        BigInteger numInicialSeq,
        @WebParam(name = "dataInicioPrevUtiliz", targetNamespace = "")
        XMLGregorianCalendar dataInicioPrevUtiliz,
        @WebParam(name = "numCertSWFatur", targetNamespace = "")
        BigInteger numCertSWFatur,
        @WebParam(name = "meioProcessamento", targetNamespace = "")
        String meioProcessamento);

    @WebMethod
    @WebResult(name = "finalizarSerieResp", targetNamespace = "")
    @RequestWrapper(localName = "finalizarSerie", targetNamespace = "http://at.gov.pt/", className = "com.premiumminds.billy.portugal.webservices.series.FinalizarSerie")
    @ResponseWrapper(localName = "finalizarSerieResponse", targetNamespace = "http://at.gov.pt/", className = "com.premiumminds.billy.portugal.webservices.series.FinalizarSerieResponse")
    @Action(input = "http://at.gov.pt/SeriesWS/finalizarSerieRequest", output = "http://at.gov.pt/SeriesWS/finalizarSerieResponse")
    public SeriesResp finalizarSerie(
        @WebParam(name = "serie", targetNamespace = "")
        String serie,
        @WebParam(name = "classeDoc", targetNamespace = "")
        String classeDoc,
        @WebParam(name = "tipoDoc", targetNamespace = "")
        String tipoDoc,
        @WebParam(name = "codValidacaoSerie", targetNamespace = "")
        String codValidacaoSerie,
        @WebParam(name = "seqUltimoDocEmitido", targetNamespace = "")
        BigInteger seqUltimoDocEmitido,
        @WebParam(name = "justificacao", targetNamespace = "")
        String justificacao);

    @WebMethod
    @WebResult(name = "consultarSeriesResp", targetNamespace = "")
    @RequestWrapper(localName = "consultarSeries", targetNamespace = "http://at.gov.pt/", className = "com.premiumminds.billy.portugal.webservices.series.ConsultarSeries")
    @ResponseWrapper(localName = "consultarSeriesResponse", targetNamespace = "http://at.gov.pt/", className = "com.premiumminds.billy.portugal.webservices.series.ConsultarSeriesResponse")
    @Action(input = "http://at.gov.pt/SeriesWS/consultarSeriesRequest", output = "http://at.gov.pt/SeriesWS/consultarSeriesResponse")
    public ConsultSeriesResp consultarSeries(
        @WebParam(name = "serie", targetNamespace = "")
        String serie,
        @WebParam(name = "tipoSerie", targetNamespace = "")
        String tipoSerie,
        @WebParam(name = "classeDoc", targetNamespace = "")
        String classeDoc,
        @WebParam(name = "tipoDoc", targetNamespace = "")
        String tipoDoc,
        @WebParam(name = "codValidacaoSerie", targetNamespace = "")
        String codValidacaoSerie,
        @WebParam(name = "dataRegistoDe", targetNamespace = "")
        XMLGregorianCalendar dataRegistoDe,
        @WebParam(name = "dataRegistoAte", targetNamespace = "")
        XMLGregorianCalendar dataRegistoAte,
        @WebParam(name = "estado", targetNamespace = "")
        String estado,
        @WebParam(name = "meioProcessamento", targetNamespace = "")
        String meioProcessamento);

    @WebMethod
    @WebResult(name = "anularSerieResp", targetNamespace = "")
    @RequestWrapper(localName = "anularSerie", targetNamespace = "http://at.gov.pt/", className = "com.premiumminds.billy.portugal.webservices.series.AnularSerie")
    @ResponseWrapper(localName = "anularSerieResponse", targetNamespace = "http://at.gov.pt/", className = "com.premiumminds.billy.portugal.webservices.series.AnularSerieResponse")
    @Action(input = "http://at.gov.pt/SeriesWS/anularSerieRequest", output = "http://at.gov.pt/SeriesWS/anularSerieResponse")
    public SeriesResp anularSerie(
        @WebParam(name = "serie", targetNamespace = "")
        String serie,
        @WebParam(name = "classeDoc", targetNamespace = "")
        String classeDoc,
        @WebParam(name = "tipoDoc", targetNamespace = "")
        String tipoDoc,
        @WebParam(name = "codValidacaoSerie", targetNamespace = "")
        String codValidacaoSerie,
        @WebParam(name = "motivo", targetNamespace = "")
        String motivo,
        @WebParam(name = "declaracaoNaoEmissao", targetNamespace = "")
        boolean declaracaoNaoEmissao);
}