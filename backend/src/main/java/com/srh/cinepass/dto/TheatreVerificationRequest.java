package com.srh.cinepass.dto;

public class TheatreVerificationRequest {

    private String theatreName;
    private String ownerName;
    private String gstin;
    private String panNumber;
    private String registrationNumber;
    private String mobileNumber;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private Integer numberOfScreens;
    private Integer totalSeats;
    private String theatreType;
    private Long locationId;
    private String gstDocumentUrl;
    private String businessDocumentUrl;
    private String theatrePhotoUrl;

    public TheatreVerificationRequest() {}

    public String getTheatreName() { return theatreName; }
    public void setTheatreName(String theatreName) { this.theatreName = theatreName; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }

    public String getPanNumber() { return panNumber; }
    public void setPanNumber(String panNumber) { this.panNumber = panNumber; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }

    public Integer getNumberOfScreens() { return numberOfScreens; }
    public void setNumberOfScreens(Integer numberOfScreens) { this.numberOfScreens = numberOfScreens; }

    public Integer getTotalSeats() { return totalSeats; }
    public void setTotalSeats(Integer totalSeats) { this.totalSeats = totalSeats; }

    public String getTheatreType() { return theatreType; }
    public void setTheatreType(String theatreType) { this.theatreType = theatreType; }

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }

    public String getGstDocumentUrl() { return gstDocumentUrl; }
    public void setGstDocumentUrl(String gstDocumentUrl) { this.gstDocumentUrl = gstDocumentUrl; }

    public String getBusinessDocumentUrl() { return businessDocumentUrl; }
    public void setBusinessDocumentUrl(String businessDocumentUrl) { this.businessDocumentUrl = businessDocumentUrl; }

    public String getTheatrePhotoUrl() { return theatrePhotoUrl; }
    public void setTheatrePhotoUrl(String theatrePhotoUrl) { this.theatrePhotoUrl = theatrePhotoUrl; }
}