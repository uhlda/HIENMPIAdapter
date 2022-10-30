/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hienetworks.nwhin.mpi.adapter;

import gov.hhs.fha.nhinc.mpilib.Address;

/**
 * Patient Demographic Entity
 * @author Sir Alan of Uhl
 */
public class PatientInfo {
	private String fname;
	private String lname;
	private String dob;
	private String gender;
	private Address address;
    private String ssn;
    private String repositoryOID;
    private String callingOID;

    /**
     * Patient First Name Getter/Setter
     * @return property value
     */
    public String getFname() {
		return fname;
	}

    public void setFname(String fname) {
		this.fname = fname;
	}

    /**
     * Patient Last Name Getter/Setter
     * @return property value
     */
    public String getLname() {
		return lname;
	}

    public void setLname(String lname) {
		this.lname = lname;
	}

    /**
     * Patient Date of Birth (DOB) Getter/Setter
     * @return property value
     */
    public String getDob() {
		return dob;
	}

	public void setDob(String dob) {
		this.dob = dob;
	}

    /**
     * Patient Address Getter/Setter (container for address1/2, city, state...)
     * @return property value
     */
    public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

    /**
     * Patient Gender Getter/Setter
     * @return property value
     */
    public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}
        
    /**
     * Patient Social Security Number (SSN) Getter/Setter
     * @return property value
     */
    public String getSSN()
    {
        return this.ssn;
    }
    
    public void setSSN(String ssn)
    {
        this.ssn = ssn;
    }    

    /**
     * Patient Target Repository OID (HCID) Getter/Setter
     * @return property value
     */
    public String getRepositoryOID()
    {
        return this.repositoryOID;
    }
    
    public void setRepositoryOID(String repositoryOID)
    {
        this.repositoryOID = repositoryOID;
    }            
    
    /**
     * Patient Calling Repository OID (HCID) Getter/Setter
     * @return property value
     */
    public String getCallingOID()
    {
        return this.callingOID;
    }
    
    public void setCallingOID(String callingOID)
    {
        this.callingOID = callingOID;
    }                
}
