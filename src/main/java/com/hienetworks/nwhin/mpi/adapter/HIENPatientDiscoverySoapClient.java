package com.hienetworks.nwhin.mpi.adapter;

import java.io.IOException;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.BindingProvider;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import gov.hhs.fha.nhinc.nhinclib.NhincConstants;
import gov.hhs.fha.nhinc.properties.PropertyAccessException;
import gov.hhs.fha.nhinc.properties.PropertyAccessor;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.datatype.DatatypeConfigurationException;

public class HIENPatientDiscoverySoapClient {
 
    public static String AuthToken = "";
    public static String LocalHomeCommunityID = "";
    
    private final static Resource pdWsdl = new DefaultResourceLoader()
                    .getResource("classpath:wsdl/RHINWebService1.wsdl");
    private final static int CONNECTION_TIMEOUT = 60000;
    private final static String CENTRALIS_AUTHENTICATION_TOKEN = "9DB9082B-68F3-4A7B-8B11-1BA0649945DE";
    
    private static IRHINWebService pdPort;
    private static HIENPatientDiscoverySoapClient client = null;    
    private PropertyAccessor propertyAccessor = PropertyAccessor.getInstance();	
     
    private HIENPatientDiscoverySoapClient() {
        try {
            init();
        } catch (IOException ei) {
            Logger.getLogger(HIENPatientDiscoverySoapClient.class.getName()).log(Level.SEVERE, null, ei);
        } catch (PropertyAccessException ep) {
            Logger.getLogger(HIENPatientDiscoverySoapClient.class.getName()).log(Level.SEVERE, null, ep);
        } catch (Exception e) {
            Logger.getLogger(HIENPatientDiscoverySoapClient.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public static HIENPatientDiscoverySoapClient getInstance() {
        if(null == client) {
            client = new HIENPatientDiscoverySoapClient();
        }
        return client;
    }
    
    // TODO: Determine whether we really need to use the below checked PostConstruct attribute.
    // @PostConstruct
    public void init() throws IOException, PropertyAccessException, MissingURLException {

        // Get RESTful RHINWebService URL and Centralis application authentication token
        String urlString = propertyAccessor.getProperty(NhincConstants.GATEWAY_PROPERTY_FILE, "RHINWebServiceUrl");    
        if (com.google.common.base.Strings.isNullOrEmpty(urlString))
        {
            throw new PropertyAccessException("Centralis RHIN Web Service URL Empty or Not Found");
        }
        
        // Get Centralis HCID
        LocalHomeCommunityID = propertyAccessor.getProperty(NhincConstants.GATEWAY_PROPERTY_FILE, "localHomeCommunityId");    
        if (com.google.common.base.Strings.isNullOrEmpty(LocalHomeCommunityID))
        {
            throw new PropertyAccessException("Centralis HCID Empty or Not Found");
        }        

        // Try getting Centralis app/session authentication token from properties file, if not use hard coded literal
        AuthToken = propertyAccessor.getProperty(NhincConstants.GATEWAY_PROPERTY_FILE, "CentralisAuthenticationToken");            
        if (com.google.common.base.Strings.isNullOrEmpty(AuthToken))
        {
            AuthToken = CENTRALIS_AUTHENTICATION_TOKEN;
        }

        // Create RHINWebService service from RHINWebService1 service section
        RHINWebService pdService = new RHINWebService(pdWsdl.getURL());

        // Create the RHINWebService service endpoint(s)
        pdPort = pdService.getPort(IRHINWebService.class);

        // Bind service port to endpoint schema
        BindingProvider pdBindingProvider = (BindingProvider) pdPort;

        // Add RHINWebService URL to endpoint/service context 
        pdBindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, urlString);

        // Set endpoint/service timeouts
        Map<String, Object> pdContext = pdBindingProvider.getRequestContext();
        pdContext.put("com.sun.xml.internal.ws.connect.timeout", CONNECTION_TIMEOUT);
        pdContext.put("com.sun.xml.internal.ws.request.timeout", CONNECTION_TIMEOUT);
    }

    public PatientRMPIEntity sendData(PatientInfo patientInfo) {		
        XMLGregorianCalendar dob = null;
        PatientRMPIEntity response = null;
        
        try {
            if (patientInfo.getDob() != null)
            {
                dob = DatatypeFactory.newInstance().newXMLGregorianCalendar(patientInfo.getDob());
            }
            response = pdPort.getCONNECTPatient(
                AuthToken, 
                patientInfo.getLname(), 
                patientInfo.getFname(), 
                patientInfo.getGender(), 
                dob, 
                patientInfo.getSSN(),
                patientInfo.getRepositoryOID());
        } catch (DatatypeConfigurationException e) {
            Logger.getLogger(HIENPatientDiscoverySoapClient.class.getName()).log(Level.SEVERE, null, "PatientDiscoverySoapClient Send DataTypeConfig Failure: " + e);
        } catch (Exception e) {
            Logger.getLogger(HIENPatientDiscoverySoapClient.class.getName()).log(Level.SEVERE, null, "PatientDiscoverySoapClient Send General Failure: " + e);
        }

        return response;
    }
       
    public class MissingURLException extends Exception {
        public MissingURLException(String message) {
            super(message);
        }
    }
}
