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
    private final ReservationRepository reservationRepo;
    private final OrganisationRepository orgRepo;

    public IssueController(IssueReportRepository issueRepo, ReservationRepository reservationRepo,
                           OrganisationRepository orgRepo) {
        this.issueRepo = issueRepo;
        this.reservationRepo = reservationRepo;
        this.orgRepo = orgRepo;
    }

    @GetMapping("/seller/{sellerId}")
    public List<IssueReport> getBySeller(@PathVariable UUID sellerId) {
        return issueRepo.findBySeller(sellerId);
    }

    @GetMapping("/seller/{sellerId}/open")
    public List<IssueReport> getOpenBySeller(@PathVariable UUID sellerId) {
        return issueRepo.findOpenBySeller(sellerId);
    }

    @GetMapping("/org/{orgId}")
    public List<IssueReport> getByOrg(@PathVariable UUID orgId) {
        return issueRepo.findByOrganisationOrgId(orgId);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateIssueRequest req) {
        IssueReport issue = new IssueReport();

        if (req.getReservationId() != null) {
            issue.setReservation(reservationRepo.findById(req.getReservationId()).orElse(null));
        }
        if (req.getOrgId() != null) {
            issue.setOrganisation(orgRepo.findById(req.getOrgId()).orElse(null));
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
        private UUID reservationId;
        private UUID orgId;
        private IssueReport.Type type;
        private String description;

        public UUID getReservationId() { return reservationId; }
        public void setReservationId(UUID reservationId) { this.reservationId = reservationId; }
        public UUID getOrgId() { return orgId; }
        public void setOrgId(UUID orgId) { this.orgId = orgId; }
        public IssueReport.Type getType() { return type; }
        public void setType(IssueReport.Type type) { this.type = type; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class RespondRequest {
        private String response;
        private boolean resolve;

        public String getResponse() { return response; }
        public void setResponse(String response) { this.response = response; }
        public boolean isResolve() { return resolve; }
        public void setResolve(boolean resolve) { this.resolve = resolve; }
    }
}
