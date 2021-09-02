/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hienetworks.nwhin.mpi.adapter;

import gov.hhs.fha.nhinc.mpilib.Address;

/**
 *
 * @author Ekalavya-Wilmer
 */
public class PatientInfo {
	private String fname;
	private String lname;
	private String dob;
	private String gender;
	private Address address;
        private String ssn;
        private String repositoryOID;

	public String getFname() {
		return fname;
	}

	public void setFname(String fname) {
		this.fname = fname;
	}

	public String getLname() {
		return lname;
	}

	public void setLname(String lname) {
		this.lname = lname;
	}

	public String getDob() {
		return dob;
	}

	public void setDob(String dob) {
		this.dob = dob;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}
        
    public String getSSN()
    {
        return this.ssn;
    }
    
    public void setSSN(String ssn)
    {
        this.ssn = ssn;
    }    

    public String getRepositoryOID()
    {
        return this.repositoryOID;
    }
    
    public void setRepositoryOID(String ssn)
    {
        this.ssn = repositoryOID;
    }            
}
