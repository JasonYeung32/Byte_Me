package com.byteme.app;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "employee_badge")
 
@IdClass(EmployeeBadge.Key.class)
public class EmployeeBadge {

    @Id
    private UUID employeeId;

    @Id
    private UUID badgeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", insertable = false, updatable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_id", insertable = false, updatable = false)
    private Badge badge;

    @Column(nullable = false)
    private Instant awardedAt = Instant.now();

	// Getters
    public UUID getEmployeeId() { return employeeId; }
    public UUID getBadgeId() { return badgeId; }
    public Employee getEmployee() { return employee; }
    public Badge getBadge() { return badge; }
    public Instant getAwardedAt() { return awardedAt; }

    // Setters
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
    public void setBadgeId(UUID badgeId) { this.badgeId = badgeId; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public void setBadge(Badge badge) { this.badge = badge; }
    public void setAwardedAt(Instant awardedAt) { this.awardedAt = awardedAt; }

    
    public static class Key implements java.io.Serializable {
        private UUID employeeId;
        private UUID badgeId;

		/// Getters
        public UUID getEmployeeId() { return employeeId; }
        public UUID getBadgeId() { return badgeId; }

        // Setters
        public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
        public void setBadgeId(UUID badgeId) { this.badgeId = badgeId; }
    }
}
