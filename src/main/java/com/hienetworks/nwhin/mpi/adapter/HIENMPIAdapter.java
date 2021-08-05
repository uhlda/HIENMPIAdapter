/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hienetworks.nwhin.mpi.adapter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.annotation.Resource;
import javax.xml.ws.BindingType;
import javax.xml.ws.WebServiceContext;

import org.apache.log4j.Logger;
import org.datacontract.schemas._2004._07.hielibrary.PatientEntity;
import org.hl7.v3.PRPAIN201305UV02;
import org.hl7.v3.PRPAIN201305UV02QUQIMT021001UV01ControlActProcess;
import org.hl7.v3.PRPAIN201306UV02;
import org.hl7.v3.PRPAMT201306UV02ParameterList;
import org.hl7.v3.RespondingGatewayPRPAIN201305UV02RequestType;

import gov.hhs.fha.nhinc.mpi.adapter.component.hl7parsers.HL7Parser201305;
import gov.hhs.fha.nhinc.mpi.adapter.component.hl7parsers.HL7Parser201306;
import gov.hhs.fha.nhinc.mpilib.Address;
import gov.hhs.fha.nhinc.mpilib.Addresses;
import gov.hhs.fha.nhinc.mpilib.Patient;
import gov.hhs.fha.nhinc.mpilib.Patients;
import gov.hhs.fha.nhinc.mpilib.PersonName;
import gov.hhs.fha.nhinc.mpilib.PersonNames;

/**
 *
 * @author Ekalavya-Wilmer
 */
@BindingType(value = javax.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING)
public class HIENMPIAdapter implements gov.hhs.fha.nhinc.adaptercomponentmpi.AdapterComponentMpiPortType {

    @Resource
    private WebServiceContext context;

    private static final Logger logger = Logger.getLogger(HIENMPIAdapter.class);

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
            //Read Values from request (Check CONNECT Code on how to read First Name, Last Name, DoB, Gender
            //Create JSON Object using these parameter to send RESTful call to get Patient details if Patient Found...
            //Read response from JSON Object and set response to PRPAIN201306UV02
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
                //send request to RHINWebService api
                
                logger.info("PatientDiscovery SOAP call.......");
                HIENPatientDiscoverySoapClient client = HIENPatientDiscoverySoapClient.getInstance();
                PatientEntity patientList = client.sendData(patientInfo);

                logger.info("Response msg :" + patientList);
                if (patientList != null) {
                    try {
                    	final Patients patList = new Patients();
                    	if(null == patientList.getError()) {
                    		Patient p = new Patient();
                    		//name
                    		PersonName pName = new PersonName();
                    		pName.setFirstName(patientList.getX003CFirstNameX003EKBackingField());
                    		pName.setFirstName(patientList.getX003CFirstNameX003EKBackingField());
                            pName.setLastName(patientList.getX003CLastNameX003EKBackingField());
                            pName.setMiddleName(patientList.getX003CMiddleNameX003EKBackingField());
                            PersonNames pNameList = new PersonNames();
                            pNameList.add(pName);
                            p.setNames(pNameList);
                          //address
                            gov.hhs.fha.nhinc.mpilib.Address pAddr = new gov.hhs.fha.nhinc.mpilib.Address();
                            pAddr.setStreet1(patientList.getX003CAddress1X003EKBackingField());
                            pAddr.setStreet2(patientList.getX003CAddress2X003EKBackingField());
                            pAddr.setCity(patientList.getX003CCityX003EKBackingField());
                            pAddr.setState(patientList.getX003CStateCodeX003EKBackingField());
                            pAddr.setZip(patientList.getX003CPostalCodeX003EKBackingField());
                            Addresses pAddrList = new Addresses();
                            pAddrList.add(pAddr);
                            p.setGender(patientList.getX003CGenderX003EKBackingField());
                            LocalDateTime parse = LocalDateTime.parse((patientList.getX003CDateOfBirthX003EKBackingField().toString()));
                            p.setDateOfBirth(parse.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
                          //PhoneNumber ph = new PhoneNumber(patientEntity.get);
                            //PhoneNumbers phoneNumbers = new PhoneNumbers();
                            //phoneNumbers.add(ph);
                            p.setAddresses(pAddrList);
                            p.setSSN(patientList.getX003CSSNX003EKBackingField());
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
        return oResponse;
    }

}
