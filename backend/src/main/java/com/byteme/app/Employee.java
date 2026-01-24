package com.byteme.app;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "employee")
 
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID employeeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organisation organisation;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @Column(nullable = false)
    private String displayName;

    // Streak tracking (denormalized for simplicity)
    @Column(nullable = false)
    private Integer currentStreakWeeks = 0;

    @Column(nullable = false)
    private Integer bestStreakWeeks = 0;

    private LocalDate lastRescueWeekStart;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Employee() {}

    public UUID getEmployeeId() {return employeeId;}
    public void setEmployeeId(UUID employeeId) {this.employeeId = employeeId;}
    public Organisation getOrganisation() {return organisation;}
    public void setOrganisation(Organisation organisation) {this.organisation = organisation;}
    public UserAccount getUser() {return user;}
    public void setUser(UserAccount user) {this.user = user;}
    public String getDisplayName() {return displayName;}
    public void setDisplayName(String displayName) {this.displayName = displayName;}
    public Integer getCurrentStreakWeeks() {return currentStreakWeeks;}
    public void setCurrentStreakWeeks(Integer currentStreakWeeks) {this.currentStreakWeeks = currentStreakWeeks;}
    public Integer getBestStreakWeeks() {return bestStreakWeeks;}
    public void setBestStreakWeeks(Integer bestStreakWeeks) {this.bestStreakWeeks = bestStreakWeeks;}
    public LocalDate getLastRescueWeekStart() {return lastRescueWeekStart;}
    public void setLastRescueWeekStart(LocalDate lastRescueWeekStart) {this.lastRescueWeekStart = lastRescueWeekStart;}
    public Instant getCreatedAt() {return createdAt;}
}
