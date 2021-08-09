package com.hienetworks.nwhin.mpi.adapter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.datacontract.schemas._2004._07.hielibrary.PatientEntity;
import org.tempuri.IRHINWebService;

public class Demo {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		HIENPatientDiscoverySoapClient client =  HIENPatientDiscoverySoapClient.getInstance();
		PatientInfo patientInfo = new PatientInfo();
		patientInfo.setLname("jones");
        patientInfo.setFname("curtis");
        patientInfo.setGender("M");
        patientInfo.setSSN("265662107");
        patientInfo.setDob("1944-06-23");
        
        PatientEntity patientList = client.sendData(patientInfo);
        
        HIENPatientDiscoverySoapClient client2 =  HIENPatientDiscoverySoapClient.getInstance();
        PatientEntity patientList2 = client2.sendData(patientInfo);
        
        System.out.println("END");
	}
	

}
