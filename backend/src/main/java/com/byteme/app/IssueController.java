package com.byteme.app;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/issues")
public class IssueController {

    private final IssueReportRepository issueRepo;
    private final BundlePostingRepository bundleRepo;
    private final ReservationRepository reservationRepo;
    private final EmployeeRepository employeeRepo;

    public IssueController(IssueReportRepository issueRepo, BundlePostingRepository bundleRepo,
                           ReservationRepository reservationRepo, EmployeeRepository employeeRepo) {
        this.issueRepo = issueRepo;
        this.bundleRepo = bundleRepo;
        this.reservationRepo = reservationRepo;
        this.employeeRepo = employeeRepo;
    }

    @GetMapping("/seller/{sellerId}")
    public List<IssueReport> getBySeller(@PathVariable UUID sellerId) {
        return issueRepo.findBySeller(sellerId);
    }

    @GetMapping("/seller/{sellerId}/open")
    public List<IssueReport> getOpenBySeller(@PathVariable UUID sellerId) {
        return issueRepo.findOpenBySeller(sellerId);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateIssueRequest req) {
        IssueReport issue = new IssueReport();
        if (req.getPostingId() != null) {
            issue.setPosting(bundleRepo.findById(req.getPostingId()).orElse(null));
        }
        if (req.getReservationId() != null) {
            issue.setReservation(reservationRepo.findById(req.getReservationId()).orElse(null));
        }
        if (req.getEmployeeId() != null) {
            issue.setEmployee(employeeRepo.findById(req.getEmployeeId()).orElse(null));
        }
        issue.setType(req.getType());
        issue.setDescription(req.getDescription());

        return ResponseEntity.ok(issueRepo.save(issue));
    }

    @PostMapping("/{id}/respond")
    public ResponseEntity<?> respond(@PathVariable UUID id, @RequestBody RespondRequest req) {
        var issue = issueRepo.findById(id).orElse(null);
        if (issue == null) return ResponseEntity.notFound().build();

        issue.setSellerResponse(req.getResponse());
        issue.setStatus(IssueReport.Status.RESPONDED);
        
        if (req.isResolve()) {
            issue.setStatus(IssueReport.Status.RESOLVED);
            issue.setResolvedAt(Instant.now());
        }

        return ResponseEntity.ok(issueRepo.save(issue));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<?> resolve(@PathVariable UUID id) {
        var issue = issueRepo.findById(id).orElse(null);
        if (issue == null) return ResponseEntity.notFound().build();

        issue.setStatus(IssueReport.Status.RESOLVED);
        issue.setResolvedAt(Instant.now());

        return ResponseEntity.ok(issueRepo.save(issue));
    }

    // DTOs
    public static class CreateIssueRequest {
        private UUID postingId;
        private UUID reservationId;
        private UUID employeeId;
        private IssueReport.Type type;
        private String description;

        public CreateIssueRequest() {}

        public UUID getPostingId() { return postingId; }
        public void setPostingId(UUID postingId) { this.postingId = postingId; }
        public UUID getReservationId() { return reservationId; }
        public void setReservationId(UUID reservationId) { this.reservationId = reservationId; }
        public UUID getEmployeeId() { return employeeId; }
        public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
        public IssueReport.Type getType() { return type; }
        public void setType(IssueReport.Type type) { this.type = type; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class RespondRequest {
        private String response;
        private boolean resolve;

        public RespondRequest() {}

        public String getResponse() { return response; }
        public void setResponse(String response) { this.response = response; }
        public boolean isResolve() { return resolve; }
        public void setResolve(boolean resolve) { this.resolve = resolve; }
    }
}
