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
                }

                logger.info("PatientDiscovery SOAP call.......");

                //send request to RHINWebService api                                
                PatientRMPIEntity patientList = null;
                HIENPatientDiscoverySoapClient client = HIENPatientDiscoverySoapClient.getInstance();
                if (client != null) {
                    patientList = client.sendData(patientInfo);
                } else {
                    throw new PatientDiscoverySoapClientException("Null Client from getInstance!");
                }
                
                logger.info("Response msg: " + patientList);
                if (patientList != null) {
                    try {
                        
                        logger.info("Received response");                                                
                        final Patients patList = new Patients();
                        if("".equals(patientList.getError().getValue())) {
                            logger.info("Adding patient to list.......");
                            Patient p = new Patient();
                            
                            //name
                            logger.info("Processing Name.......");
                            PersonName pName = new PersonName();
                            pName.setFirstName(patientList.getX003CFirstNameX003EKBackingField());
                            pName.setLastName(patientList.getX003CLastNameX003EKBackingField());
                            pName.setMiddleName(patientList.getX003CMiddleNameX003EKBackingField());
                            PersonNames pNameList = new PersonNames();
                            pNameList.add(pName);
                            p.setNames(pNameList);

                            //address
                            logger.info("Processing Address.......");
                            gov.hhs.fha.nhinc.mpilib.Address pAddr = new gov.hhs.fha.nhinc.mpilib.Address();
                            pAddr.setStreet1(patientList.getX003CAddress1X003EKBackingField());
                            pAddr.setStreet2(patientList.getX003CAddress2X003EKBackingField());
                            pAddr.setCity(patientList.getX003CCityX003EKBackingField());
                            pAddr.setState(patientList.getX003CStateCodeX003EKBackingField());
                            pAddr.setZip(patientList.getX003CPostalCodeX003EKBackingField());        
                            Addresses pAddrList = new Addresses();
                            pAddrList.add(pAddr);
                            p.setAddresses(pAddrList);        

                            //identifiers
                            logger.info("Processing Identifiers.......");        
                            Identifier pPrimaryRMPINumber = new Identifier();
                            pPrimaryRMPINumber.setId(patientList.x003CPrimaryRMPINumberX003EKBackingField);
                            pPrimaryRMPINumber.setOrganizationId(HIENPatientDiscoverySoapClient.LocalHomeCommunityID);
                            Identifiers pIdentities = new Identifiers();
                            pIdentities.add(pPrimaryRMPINumber);
                            p.setIdentifiers(pIdentities);

                            //gender / SSN / DOB 'n such
                            logger.info("Processing Odds and Ends.......");        
                            p.setGender(patientList.getX003CGenderX003EKBackingField());
                            LocalDateTime parse = LocalDateTime.parse((patientList.getX003CDateOfBirthX003EKBackingField().toString()));
                            p.setDateOfBirth(parse.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
                            p.setSSN(patientList.getX003CSSNX003EKBackingField());        

                            //phone numbers - TBD, which should be sooner rather than later
                            //PhoneNumber ph = new PhoneNumber(patientEntity.get);
                            //PhoneNumbers phoneNumbers = new PhoneNumbers();
                            //phoneNumbers.add(ph);
                            
                            patList.add(p);
                        }
                        oResponse = HL7Parser201306.buildMessageFromMpiPatient(patList, oPRPAIN201305UV02);                        

                    } catch (Exception e) {
                        logger.error(e);
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

//    private Patients ProcessPatientList(PatientRMPIEntity patientList) {
//        final Patients patList = new Patients();
//        if("".equals(patientList.getError().getValue())) {
//            logger.info("Adding patient to list.......");
//            Patient p = new Patient();
//            ProcessPatient(patientList, p);
//            patList.add(p);
//        }
//        return patList;
//    }

//    private void ProcessPatient(PatientRMPIEntity patientList, Patient p) {
//        //name
//        logger.info("Processing Name.......");
//        PersonName pName = new PersonName();
//        pName.setFirstName(patientList.getX003CFirstNameX003EKBackingField());
//        pName.setFirstName(patientList.getX003CFirstNameX003EKBackingField());
//        pName.setLastName(patientList.getX003CLastNameX003EKBackingField());
//        pName.setMiddleName(patientList.getX003CMiddleNameX003EKBackingField());
//        PersonNames pNameList = new PersonNames();
//        pNameList.add(pName);
//        p.setNames(pNameList);
//        
//        //address
//        logger.info("Processing Address.......");
//        gov.hhs.fha.nhinc.mpilib.Address pAddr = new gov.hhs.fha.nhinc.mpilib.Address();
//        pAddr.setStreet1(patientList.getX003CAddress1X003EKBackingField());
//        pAddr.setStreet2(patientList.getX003CAddress2X003EKBackingField());
//        pAddr.setCity(patientList.getX003CCityX003EKBackingField());
//        pAddr.setState(patientList.getX003CStateCodeX003EKBackingField());
//        pAddr.setZip(patientList.getX003CPostalCodeX003EKBackingField());        
//        Addresses pAddrList = new Addresses();
//        pAddrList.add(pAddr);
//        p.setAddresses(pAddrList);        
//        
//        //identifiers
//        logger.info("Processing Identifiers.......");        
//        Identifier pPrimaryRMPINumber = new Identifier();
//        pPrimaryRMPINumber.setId(patientList.x003CPrimaryRMPINumberX003EKBackingField);
//        pPrimaryRMPINumber.setOrganizationId(HIENPatientDiscoverySoapClient.LocalHomeCommunityID);
//        Identifiers pIdentities = new Identifiers();
//        pIdentities.add(pPrimaryRMPINumber);
//        p.setIdentifiers(pIdentities);
//
//        //gender / SSN / DOB 'n such
//        logger.info("Processing Odds and Ends.......");        
//        p.setGender(patientList.getX003CGenderX003EKBackingField());
//        LocalDateTime parse = LocalDateTime.parse((patientList.getX003CDateOfBirthX003EKBackingField().toString()));
//        p.setDateOfBirth(parse.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
//        p.setSSN(patientList.getX003CSSNX003EKBackingField());        
//        
//        //phone numbers - TBD, which should be sooner rather than later
//        //PhoneNumber ph = new PhoneNumber(patientEntity.get);
//        //PhoneNumbers phoneNumbers = new PhoneNumbers();
//        //phoneNumbers.add(ph);
//    }
    
    public class PatientDiscoverySoapClientException extends Exception {
        public PatientDiscoverySoapClientException(String message) {
            super(message);
        }
    }
}
