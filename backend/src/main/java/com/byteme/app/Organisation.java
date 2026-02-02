package com.byteme.app;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organisation")
public class Organisation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID orgId;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserAccount user;

    @Column(nullable = false)
    private String name;

    private String locationText;
    private String billingEmail;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // Getters
    public UUID getOrgId() { return orgId; }
    public UserAccount getUser() { return user; }
    public String getName() { return name; }
    public String getLocationText() { return locationText; }
    public String getBillingEmail() { return billingEmail; }
    public Instant getCreatedAt() { return createdAt; }

    // Setters
    public void setOrgId(UUID orgId) { this.orgId = orgId; }
    public void setUser(UserAccount user) { this.user = user; }
    public void setName(String name) { this.name = name; }
    public void setLocationText(String locationText) { this.locationText = locationText; }
    public void setBillingEmail(String billingEmail) { this.billingEmail = billingEmail; }
}
