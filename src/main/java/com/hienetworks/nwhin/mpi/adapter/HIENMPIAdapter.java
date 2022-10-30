/*
 * Patient Discovery (PD) (PRPAIN201305UV02/PRPAIN201306UV02) CONNECT Adapter
 */
package com.hienetworks.nwhin.mpi.adapter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.annotation.Resource;
import javax.xml.ws.BindingType;
import javax.xml.ws.WebServiceContext;

import org.apache.log4j.Logger;
import org.hl7.v3.PRPAIN201305UV02;
import org.hl7.v3.PRPAIN201305UV02QUQIMT021001UV01ControlActProcess;
import org.hl7.v3.PRPAIN201306UV02;
import org.hl7.v3.PRPAMT201306UV02ParameterList;
import org.hl7.v3.RespondingGatewayPRPAIN201305UV02RequestType;

import gov.hhs.fha.nhinc.mpi.adapter.component.hl7parsers.HL7Parser201305;
import gov.hhs.fha.nhinc.mpi.adapter.component.hl7parsers.HL7Parser201306;
import gov.hhs.fha.nhinc.mpilib.Address;
import gov.hhs.fha.nhinc.mpilib.Addresses;
import gov.hhs.fha.nhinc.mpilib.Identifier;
import gov.hhs.fha.nhinc.mpilib.Identifiers;
import gov.hhs.fha.nhinc.mpilib.Patient;
import gov.hhs.fha.nhinc.mpilib.Patients;
import gov.hhs.fha.nhinc.mpilib.PersonName;
import gov.hhs.fha.nhinc.mpilib.PersonNames;
import gov.hhs.fha.nhinc.nhinclib.NhincConstants;
import gov.hhs.fha.nhinc.properties.PropertyAccessException;
import gov.hhs.fha.nhinc.properties.PropertyAccessor;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import org.hl7.v3.II;

/**
 * Patient Discovery (PD) / Master Patient Index (eMPI) CONNECT Adapter
 * <p>
 *  Find candidate patients ((PRPAIN201305UV02) as per the given PD request parameters  (lname, fname, ssn, .etc).
 *  Package and Return candidate patients (PRPAIN201306UV02) 
 * @author Ekalavya-Wilmer, Sir Alan of Uhl
 */
@BindingType(value = javax.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING)
public class HIENMPIAdapter implements gov.hhs.fha.nhinc.adaptercomponentmpi.AdapterComponentMpiPortType {

    @Resource
    private WebServiceContext context;
    
    private static final Logger logger = Logger.getLogger(HIENMPIAdapter.class);
    private PropertyAccessor propertyAccessor = PropertyAccessor.getInstance();
    private String FilterSenders = "";
    private String FilterAllowStates = "";
    private String SenderHCID = null;

    /**
     * Find documents (XDSDocumentEntry objects) in the registry for a given patientID with a matching 
     * availabilityStatus attribute. The other parameters can be used to restrict the set of XDSDocumentEntry objects 
     * returned.
     * @param request   Request prop container
     * @return 
     */
    @Override
    public PRPAIN201306UV02 findCandidates(RespondingGatewayPRPAIN201305UV02RequestType request) {
        PRPAIN201306UV02 oResponse = null;
        String fName = null;
        String lName = null;
        String gender = null;
        String dob = null;
        PatientInfo patientInfo = new PatientInfo();
            
        try {
            logger.info("In HIENMPIAdapter");

            // ******************************************************************************************************
            // 1.) Read Values from request / PRPAIN201305UV02 (Check CONNECT Code on how to read the demographics
            // 2.) Create JSON Object using the request parameters to send RESTful call to get Patient details
            // 3.) Read response from JSON response Object and build/package up PRPAIN201306UV02 response 
            // ******************************************************************************************************            
            oResponse = new PRPAIN201306UV02();
            PRPAIN201305UV02 oPRPAIN201305UV02 = request.getPRPAIN201305UV02();
            PRPAIN201305UV02QUQIMT021001UV01ControlActProcess controlActProcess = oPRPAIN201305UV02.getControlActProcess();
            if (controlActProcess != null) {
                PRPAMT201306UV02ParameterList parameterList = HL7Parser201305.extractHL7QueryParamsFromMessage(oPRPAIN201305UV02);
                
                if (runFilterRules(oPRPAIN201305UV02)) {
                
                    if (parameterList != null) {
                        PersonName personName = HL7Parser201305.extractPersonName(parameterList);
                        if (personName != null) {
                            fName = personName.getFirstName();
                            lName = personName.getLastName();
                        }

                        //extracting data from connect Patient discovery request
                        gender = HL7Parser201305.extractGender(parameterList);
                        dob = HL7Parser201305.extractBirthdate(parameterList);
                        Address personAddress = HL7Parser201305.extractPersonAddress(parameterList);

                        patientInfo.setFname(fName);
                        patientInfo.setLname(lName);
                        patientInfo.setGender(gender);
                        patientInfo.setDob(LocalDate.parse(dob, DateTimeFormatter.ofPattern("yyyyMMdd")).toString());
                        patientInfo.setAddress(personAddress);
                        patientInfo.setCallingOID(SenderHCID);
                    }

                    logger.info("PatientDiscovery SOAP call...");

                    PatientRMPIEntity patientResponse = null;
                    long before = Instant.now().toEpochMilli();

                    //**************************************************************************************
                    //SEND REQUEST to RHINWebService API
                    //**************************************************************************************
                    HIENPatientDiscoverySoapClient client = HIENPatientDiscoverySoapClient.getInstance();
                    if (client != null) {
                        patientResponse = client.sendData(patientInfo);
                    } else {
                        throw new PatientDiscoverySoapClientException("Null Client from instantiating client!");
                    }

                    long after = Instant.now().toEpochMilli();                    
                    logger.info("HIENMPIAdapter Transaction Duration: " + (after-before));
                    
                    if (null != patientResponse) {

                        logger.info("Response msg: " + patientResponse);

                        final Patients patList = new Patients();

                        String errorMsg = patientResponse.getError().getValue();

                        if("".equals(errorMsg)) {
                            logger.info("Adding patient to list.......");
                            Patient p = new Patient();

                            //name
                            logger.info("Processing Name.......");
                            PersonName pName = new PersonName();
                            pName.setFirstName(patientResponse.getX003CFirstNameX003EKBackingField());
                            pName.setLastName(patientResponse.getX003CLastNameX003EKBackingField());
                            pName.setMiddleName(patientResponse.getX003CMiddleNameX003EKBackingField());
                            PersonNames pNameList = new PersonNames();
                            pNameList.add(pName);
                            p.setNames(pNameList);

                            //address
                            logger.info("Processing Address.......");
                            gov.hhs.fha.nhinc.mpilib.Address pAddr = new gov.hhs.fha.nhinc.mpilib.Address();
                            pAddr.setStreet1(patientResponse.getX003CAddress1X003EKBackingField());
                            pAddr.setStreet2(patientResponse.getX003CAddress2X003EKBackingField());
                            pAddr.setCity(patientResponse.getX003CCityX003EKBackingField());
                            pAddr.setState(patientResponse.getX003CStateCodeX003EKBackingField());
                            pAddr.setZip(patientResponse.getX003CPostalCodeX003EKBackingField());        
                            Addresses pAddrList = new Addresses();
                            pAddrList.add(pAddr);
                            p.setAddresses(pAddrList);        

                            //identifiers
                            logger.info("Processing Identifiers.......");        
                            Identifier pPrimaryRMPINumber = new Identifier();
                            pPrimaryRMPINumber.setId(patientResponse.x003CPrimaryRMPINumberX003EKBackingField);
                            pPrimaryRMPINumber.setOrganizationId(HIENPatientDiscoverySoapClient.LocalHomeCommunityID);
                            Identifiers pIdentities = new Identifiers();
                            pIdentities.add(pPrimaryRMPINumber);
                            p.setIdentifiers(pIdentities);

                            //gender / SSN / DOB 'n such
                            logger.info("Processing Odds and Ends.......");        
                            p.setGender(patientResponse.getX003CGenderX003EKBackingField());
                            LocalDateTime parse = LocalDateTime.parse((patientResponse.getX003CDateOfBirthX003EKBackingField().toString()));
                            p.setDateOfBirth(parse.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
                            p.setSSN(patientResponse.getX003CSSNX003EKBackingField());        

                            //phone numbers - TBD, which should be sooner rather than later
                            //PhoneNumber ph = new PhoneNumber(patientEntity.get);
                            //PhoneNumbers phoneNumbers = new PhoneNumbers();
                            //phoneNumbers.add(ph);

                            patList.add(p);
                        } else {
                            logger.error("Error sending PD: "+errorMsg);
                        }
                        oResponse = HL7Parser201306.buildMessageFromMpiPatient(patList, oPRPAIN201305UV02);                        

                    } else {
                        logger.info("no content from womba patientDiscovery api");
                        Patients patListempty = new Patients();
                        oResponse = HL7Parser201306.buildMessageFromMpiPatient(patListempty, oPRPAIN201305UV02);
                    }
                } else {
                    logger.info("no content from womba patientDiscovery api");
                    Patients patListempty = new Patients();
                    oResponse = HL7Parser201306.buildMessageFromMpiPatient(patListempty, oPRPAIN201305UV02);
                }
            }
        } catch (Exception exp) {
            logger.error(exp);
        }
        
        logger.info("Returning Response: " + oResponse.getAcceptAckCode());
        return oResponse;
    }

    /**
     * Introduce throttling by filtering out PD Requests by given ACL properties (gateway.properties)
     * @param oPRPAIN201305UV02 PD Query Request Message Content
     * @return TRUE, if the PD is allowed to continue, FALSE if request
     */
    private boolean runFilterRules(PRPAIN201305UV02 oPRPAIN201305UV02) {
        
        boolean allowPassage = false;
        
        logger.info("In runFilterRules");
        
        try {
            
            /*
             * Load Filter Rules from gateway properties
             */
            loadFilterRules();

            logger.info("Allowed Sender: " + FilterSenders);

            /*
             * Run BLACK-LIST FILTER BY SENDER
             * Probably FL to begin with
             */
            List<II> senderIDs = oPRPAIN201305UV02.getSender().getDevice().getId();       
            SenderHCID = senderIDs.get(0).getRoot();
            logger.info("Request Sender: " + SenderHCID);
            if (!SenderHCID.equals(FilterSenders)) {
                logger.info("Sender not in ACL");
                return true;
            }

            /*
             * Get PD Request Parameters (e.g., LName, FName, Address, etc.)
             */
            PRPAMT201306UV02ParameterList parameterList = HL7Parser201305.extractHL7QueryParamsFromMessage(oPRPAIN201305UV02);
            logger.info("Examining Request Parameter List: "+parameterList);

            /*
             * Run WHITE-LIST FILTER BY STATES (comma separated list of allowable states)
             * Probably FL to begin with
             */

            if (!FilterAllowStates.isEmpty()) {
                logger.info("State Filter Started: "+FilterAllowStates);
                Address personAddress = HL7Parser201305.extractPersonAddress(parameterList);
                if (null != personAddress) {
                    String personState = personAddress.getState();
                    FilterAllowStates+=":";
                    String[] filterAllowStates = FilterAllowStates.split(":");
                    for(String filterAllowState : filterAllowStates) {
                        if (personState.equalsIgnoreCase(filterAllowState)) {
                            allowPassage = true;
                            break;
                        }
                    }
                }
                logger.info("State Filter Completed: "+allowPassage);
            }
        } catch (Exception e) {
            logger.error("Filter Rule Error: "+e);
        }
        
        logger.error("Filter Rules Completed: "+allowPassage);
        
        return allowPassage;
    }
    
    private void loadFilterRules() {
        try { 
            logger.info("Loading Gateway Propteries");
            FilterSenders = propertyAccessor.getProperty(NhincConstants.GATEWAY_PROPERTY_FILE, "FilterSenders");
            FilterAllowStates = propertyAccessor.getProperty(NhincConstants.GATEWAY_PROPERTY_FILE, "FilterAllowStates");
            logger.info("Gateway Propteries Loaded");
        } catch (PropertyAccessException ex) {
            java.util.logging.Logger.getLogger(HIENMPIAdapter.class.getName()).log(Level.SEVERE, null, ex);
            logger.error("Error Loading Filter Rules: " + ex);
        }
    }
    
    /**
     *
     */
    public class PatientDiscoverySoapClientException extends Exception {
        public PatientDiscoverySoapClientException(String message) {
            super(message);
        }
    }
}
