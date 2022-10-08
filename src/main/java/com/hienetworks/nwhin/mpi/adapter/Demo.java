package com.hienetworks.nwhin.mpi.adapter;

// import com.hienetworks.nwhin.mpi.adapter;

import org.hl7.v3.RespondingGatewayPRPAIN201305UV02RequestType;

// TODO:Refernces to auto gen'd code, so will be transient and likely need to be nixed
// import org.datacontract.schemas._2004._07.hielibrary.PatientEntity;
// import gov.hhs.fha.nhinc.mpilib.Patient;
 
public class Demo {

    public static void main(String[] args) {
        
        // TODO Auto-generated method stub
        try
        {
            HIENPatientDiscoverySoapClient client =  HIENPatientDiscoverySoapClient.getInstance();
            

           PatientInfo patientInfo = new PatientInfo();

//         Note: Real/existing Patient from real repository                    
//            patientInfo.setLname("jones");
//            patientInfo.setFname("curtis");
//            patientInfo.setGender("M");
//            patientInfo.setSSN("265662107");
//            patientInfo.setDob("1944-06-23");

//         Note: Test patient from a test repo
            patientInfo.setLname("davidson");
            patientInfo.setFname("amy");
            patientInfo.setGender("f");
            patientInfo.setDob("1983-10-17");

            PatientRMPIEntity patientList = null; //client.sendData(patientInfo);
            HIENMPIAdapter adapter = new HIENMPIAdapter();
            RespondingGatewayPRPAIN201305UV02RequestType request = null;
//            adapter.FetchPatient(patientList, patientInfo);
            
            // Testing that the singleton / static instance continues to work after the first run
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
