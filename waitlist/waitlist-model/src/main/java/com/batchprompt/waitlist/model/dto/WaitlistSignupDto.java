package com.batchprompt.waitlist.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class WaitlistSignupDto {
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;
    
    @Size(max = 255, message = "Name must be less than 255 characters")
    private String name;
    
    @Size(max = 255, message = "Company must be less than 255 characters")
    private String company;
    
    @Size(max = 1000, message = "Use case must be less than 1000 characters")
    private String useCase;

    public WaitlistSignupDto() {}

    public WaitlistSignupDto(String email, String name, String company, String useCase) {
        this.email = email;
        this.name = name;
        this.company = company;
        this.useCase = useCase;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getUseCase() {
        return useCase;
    }

    public void setUseCase(String useCase) {
        this.useCase = useCase;
    }
}