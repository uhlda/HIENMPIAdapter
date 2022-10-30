package com.hienetworks.nwhin.mpi.adapter;

import java.io.IOException;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.BindingProvider;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import gov.hhs.fha.nhinc.nhinclib.NhincConstants;
import gov.hhs.fha.nhinc.properties.PropertyAccessException;
import gov.hhs.fha.nhinc.properties.PropertyAccessor;
import java.time.Instant;


/**
 *
 * @author auhl
 */
public class HIENPatientDiscoverySoapClient {

    private final static Resource pdWsdl = new DefaultResourceLoader()
                    .getResource("classpath:wsdl/RHINWebService1.wsdl");
    private final static int CONNECTION_TIMEOUT = 60000;
    private final static String CENTRALIS_AUTHENTICATION_TOKEN = "9DB9082B-68F3-4A7B-8B11-1BA0649945DE";
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HIENPatientDiscoverySoapClient.class);
    
    /**
     *
     */
    public static String AuthToken = "";

    /**
     *
     */
    public static String LocalHomeCommunityID = "";
    
    private static IRHINWebService pdPort;
    private static HIENPatientDiscoverySoapClient client = null;    
    private PropertyAccessor propertyAccessor = PropertyAccessor.getInstance();	
     
    private HIENPatientDiscoverySoapClient() {
        try {
            init();
        } catch (IOException ei) {
            logger.error( ei);
            logger.error(ei);
        } catch (PropertyAccessException ep) {
            logger.error( ep);
        } catch (Exception e) {
            logger.error( e);
        }
    }

    /**
     *
     * @return
     */
    public static HIENPatientDiscoverySoapClient getInstance() {
        if(null == client) {
            client = new HIENPatientDiscoverySoapClient();
        }
        return client;
    }
    
    // TODO: Determine whether we really need to use the below checked PostConstruct attribute.
    // @PostConstruct

    /**
     *
     * @throws IOException
     * @throws PropertyAccessException
     * @throws MissingURLException
     */
    public void init() throws IOException, PropertyAccessException, MissingURLException {

         logger.info("HIENPatientDiscoverySoapClient Init Start");
         
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

        logger.info("HIENPatientDiscoverySoapClient Port Assigned: "+pdPort.toString());

        // Bind service port to endpoint schema
        BindingProvider pdBindingProvider = (BindingProvider) pdPort;

        // Add RHINWebService URL to endpoint/service context 
        pdBindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, urlString);

        // Set endpoint/service timeouts
        Map<String, Object> pdContext = pdBindingProvider.getRequestContext();
        pdContext.put("com.sun.xml.internal.ws.connect.timeout", CONNECTION_TIMEOUT);
        pdContext.put("com.sun.xml.internal.ws.request.timeout", CONNECTION_TIMEOUT);

         logger.info("HIENPatientDiscoverySoapClient Init Completed");
    }

    /**
     *
     * @param patientInfo
     * @return
     */
    public PatientRMPIEntity sendData(PatientInfo patientInfo) {		
        XMLGregorianCalendar dob = null;
        PatientRMPIEntity response = null;
        
        logger.info("HIENPatientDiscoverySoapClient sendData Start");        
        
        try {
            if (patientInfo.getDob() != null)
            {
                dob = DatatypeFactory.newInstance().newXMLGregorianCalendar(patientInfo.getDob());
            }
            
            long before = Instant.now().toEpochMilli();
            
            logger.info("Pre getCONNECTPatient: " + patientInfo.getLname() + ", " + patientInfo.getFname());
  
            /**
             * IRHINWebService Interface Snippet with API Call profile
             * There were significant changes (additional args) due to a 
             * RHINWebService.wsdl update
             * (2022-10-27)
             * 
                public PatientRMPIEntity getCONNECTPatient(
                    @WebParam(name = "SessionToken", targetNamespace = "http://tempuri.org/")
                    String sessionToken,
                    @WebParam(name = "LastName", targetNamespace = "http://tempuri.org/")
                    String lastName,
                    @WebParam(name = "FirstName", targetNamespace = "http://tempuri.org/")
                    String firstName,
                    @WebParam(name = "Gender", targetNamespace = "http://tempuri.org/")
                    String gender,
                    @WebParam(name = "DOB", targetNamespace = "http://tempuri.org/")
                    XMLGregorianCalendar dob,
                    @WebParam(name = "SSN", targetNamespace = "http://tempuri.org/")
                    String ssn,
                    @WebParam(name = "Address", targetNamespace = "http://tempuri.org/")
                    String address,
                    @WebParam(name = "City", targetNamespace = "http://tempuri.org/")
                    String city,
                    @WebParam(name = "State", targetNamespace = "http://tempuri.org/")
                    String state,
                    @WebParam(name = "PostalCode", targetNamespace = "http://tempuri.org/")
                    String postalCode,
                    @WebParam(name = "RepositoryOID", targetNamespace = "http://tempuri.org/")
                    String repositoryOID,
                    @WebParam(name = "CallingOID", targetNamespace = "http://tempuri.org/")
                    String callingOID);
            */
            
            response = pdPort.getCONNECTPatient(
                AuthToken, 
                patientInfo.getLname(), 
                patientInfo.getFname(), 
                patientInfo.getGender(), 
                dob, 
                patientInfo.getSSN(),
                patientInfo.getAddress().getStreet1(),
                patientInfo.getAddress().getCity(),
                patientInfo.getAddress().getState(),
                patientInfo.getAddress().getZip(),
                patientInfo.getRepositoryOID(),
                patientInfo.getCallingOID());

            long after = Instant.now().toEpochMilli();
            logger.info("Post getCONNECTPatient: "+(after-before));
            logger.info("Caller HCID: "+patientInfo.getCallingOID());
            
        } catch (DatatypeConfigurationException e) {
            logger.error( "PatientDiscoverySoapClient Send DataTypeConfig Failure: " + e);
    
        } catch (Exception e) {
            logger.error( "PatientDiscoverySoapClient Send General Failure: " + e);
        }

        logger.info("HIENPatientDiscoverySoapClient sendData Completed");

        return response;
    }
       
    /**
     *
     */
    public class MissingURLException extends Exception {

        /**
         *
         * @param message
         */
        public MissingURLException(String message) {
            super(message);
        }
    }
}
