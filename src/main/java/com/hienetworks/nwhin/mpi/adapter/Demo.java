package com.hienetworks.nwhin.mpi.adapter;

//import org.datacontract.schemas._2004._07.hielibrary.PatientEntity;

public class Demo {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
        try
        {
            HIENPatientDiscoverySoapClient client =  HIENPatientDiscoverySoapClient.getInstance();
            PatientInfo patientInfo = new PatientInfo();
            patientInfo.setLname("jones");
            patientInfo.setFname("curtis");
            patientInfo.setGender("M");
            patientInfo.setSSN("265662107");
            patientInfo.setDob("1944-06-23");

            PatientRMPIEntity patientList = client.sendData(patientInfo);

            HIENPatientDiscoverySoapClient client2 =  HIENPatientDiscoverySoapClient.getInstance();
            PatientRMPIEntity patientList2 = client2.sendData(patientInfo);

            System.out.println("END");
        }
        catch(Exception e)
        {
            System.out.println("ERROR: " + e.getMessage());
        }
    }
}
