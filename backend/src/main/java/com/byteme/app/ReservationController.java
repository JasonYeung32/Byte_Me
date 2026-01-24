package com.byteme.app;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationRepository reservationRepo;
    private final BundlePostingRepository bundleRepo;
    private final OrganisationRepository orgRepo;
    private final EmployeeRepository employeeRepo;
    private final RescueEventRepository rescueEventRepo;
    private final PasswordEncoder passwordEncoder;

    public ReservationController(ReservationRepository reservationRepo, BundlePostingRepository bundleRepo,
                                  OrganisationRepository orgRepo, EmployeeRepository employeeRepo,
                                  RescueEventRepository rescueEventRepo, PasswordEncoder passwordEncoder) {
        this.reservationRepo = reservationRepo;
        this.bundleRepo = bundleRepo;
        this.orgRepo = orgRepo;
        this.employeeRepo = employeeRepo;
        this.rescueEventRepo = rescueEventRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/org/{orgId}")
    public List<Reservation> getByOrg(@PathVariable UUID orgId) {
        return reservationRepo.findByOrganisation_OrgId(orgId);
    }

    @GetMapping("/employee/{employeeId}")
    public List<Reservation> getByEmployee(@PathVariable UUID employeeId) {
        return reservationRepo.findByEmployee_EmployeeId(employeeId);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateReservationRequest req) {
        var bundle = bundleRepo.findById(req.getPostingId()).orElse(null);
        if (bundle == null) return ResponseEntity.notFound().build();

        int qty = req.getQuantity() != null ? req.getQuantity() : 1;
        if (!bundle.canReserve(qty)) {
            return ResponseEntity.badRequest().body("Not enough bundles available");
        }

        var org = orgRepo.findById(req.getOrganisationId()).orElse(null);
        var employee = req.getEmployeeId() != null ? employeeRepo.findById(req.getEmployeeId()).orElse(null) : null;

        String claimCode = generateClaimCode();
        String claimCodeHash = passwordEncoder.encode(claimCode);
        String claimCodeLast4 = claimCode.substring(claimCode.length() - 4);

        Reservation reservation = new Reservation();
        reservation.setPosting(bundle);
        reservation.setOrganisation(org);
        reservation.setEmployee(employee);
        reservation.setClaimCodeHash(claimCodeHash);
        reservation.setClaimCodeLast4(claimCodeLast4);

        bundle.setQuantityReserved(bundle.getQuantityReserved() + qty);
        bundleRepo.save(bundle);

        var saved = reservationRepo.save(reservation);

        return ResponseEntity.ok(new ReservationResponse(
                saved.getReservationId(),
                claimCode,
                claimCodeLast4,
                bundle.getPickupStartAt(),
                bundle.getPickupEndAt(),
                bundle.getSeller().getName(),
                bundle.getSeller().getLocationText()
        ));
    }

    @PostMapping("/{id}/verify")
    public ResponseEntity<?> verify(@PathVariable UUID id, @RequestBody VerifyRequest req) {
        var reservation = reservationRepo.findById(id).orElse(null);
        if (reservation == null) return ResponseEntity.notFound().build();

        if (reservation.getStatus() != Reservation.Status.RESERVED) {
            return ResponseEntity.badRequest().body("Reservation not in RESERVED status");
        }

        if (!passwordEncoder.matches(req.getClaimCode(), reservation.getClaimCodeHash())) {
            return ResponseEntity.badRequest().body("Invalid claim code");
        }

        reservation.setStatus(Reservation.Status.COLLECTED);
        reservation.setCollectedAt(Instant.now());
        reservationRepo.save(reservation);

        if (reservation.getEmployee() != null) {
            var employee = reservation.getEmployee();
            var bundle = reservation.getPosting();

            RescueEvent event = new RescueEvent();
            event.setEmployee(employee);
            event.setReservation(reservation);
            event.setCollectedAt(Instant.now());
            event.setMealsEstimate(1);
            event.setCo2eEstimateGrams(bundle.getEstimatedWeightGrams() != null ?
                    (int)(bundle.getEstimatedWeightGrams() * 2.5) : 500);
            rescueEventRepo.save(event);

            updateStreak(employee);
        }

        return ResponseEntity.ok(new VerifyResponse(true, "Bundle collected successfully"));
    }

    @PostMapping("/{id}/no-show")
    public ResponseEntity<?> markNoShow(@PathVariable UUID id) {
        var reservation = reservationRepo.findById(id).orElse(null);
        if (reservation == null) return ResponseEntity.notFound().build();

        reservation.setStatus(Reservation.Status.NO_SHOW);
        reservation.setNoShowMarkedAt(Instant.now());

        var bundle = reservation.getPosting();
        bundle.setQuantityReserved(bundle.getQuantityReserved() - 1);
        bundleRepo.save(bundle);

        return ResponseEntity.ok(reservationRepo.save(reservation));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable UUID id) {
        var reservation = reservationRepo.findById(id).orElse(null);
        if (reservation == null) return ResponseEntity.notFound().build();

        if (reservation.getStatus() != Reservation.Status.RESERVED) {
            return ResponseEntity.badRequest().body("Can only cancel RESERVED reservations");
        }

        reservation.setStatus(Reservation.Status.CANCELLED);

        var bundle = reservation.getPosting();
        bundle.setQuantityReserved(bundle.getQuantityReserved() - 1);
        bundleRepo.save(bundle);

        return ResponseEntity.ok(reservationRepo.save(reservation));
    }

    @PostMapping("/{id}/assign/{employeeId}")
    public ResponseEntity<?> assignEmployee(@PathVariable UUID id, @PathVariable UUID employeeId) {
        var reservation = reservationRepo.findById(id).orElse(null);
        if (reservation == null) return ResponseEntity.notFound().build();

        var employee = employeeRepo.findById(employeeId).orElse(null);
        if (employee == null) return ResponseEntity.badRequest().body("Employee not found");

        reservation.setEmployee(employee);
        return ResponseEntity.ok(reservationRepo.save(reservation));
    }

    private String generateClaimCode() {
        var random = new SecureRandom();
        var code = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    private void updateStreak(Employee employee) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1);

        if (employee.getLastRescueWeekStart() == null) {
            employee.setCurrentStreakWeeks(1);
        } else if (employee.getLastRescueWeekStart().equals(weekStart)) {
            // Same week, no change
        } else if (employee.getLastRescueWeekStart().plusWeeks(1).equals(weekStart)) {
            employee.setCurrentStreakWeeks(employee.getCurrentStreakWeeks() + 1);
        } else {
            employee.setCurrentStreakWeeks(1);
        }

        employee.setLastRescueWeekStart(weekStart);
        if (employee.getCurrentStreakWeeks() > employee.getBestStreakWeeks()) {
            employee.setBestStreakWeeks(employee.getCurrentStreakWeeks());
        }
        employeeRepo.save(employee);
    }

    // DTOs
    public static class CreateReservationRequest {
        private UUID postingId;
        private UUID organisationId;
        private UUID employeeId;
        private Integer quantity;

        public CreateReservationRequest() {}

        public UUID getPostingId() { return postingId; }
        public void setPostingId(UUID postingId) { this.postingId = postingId; }
        public UUID getOrganisationId() { return organisationId; }
        public void setOrganisationId(UUID organisationId) { this.organisationId = organisationId; }
        public UUID getEmployeeId() { return employeeId; }
        public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    public static class ReservationResponse {
        private UUID reservationId;
        private String claimCode;
        private String claimCodeLast4;
        private Instant pickupStartAt;
        private Instant pickupEndAt;
        private String sellerName;
        private String sellerLocation;

        public ReservationResponse() {}

        public ReservationResponse(UUID reservationId, String claimCode, String claimCodeLast4,
                                    Instant pickupStartAt, Instant pickupEndAt, String sellerName, String sellerLocation) {
            this.reservationId = reservationId;
            this.claimCode = claimCode;
            this.claimCodeLast4 = claimCodeLast4;
            this.pickupStartAt = pickupStartAt;
            this.pickupEndAt = pickupEndAt;
            this.sellerName = sellerName;
            this.sellerLocation = sellerLocation;
        }

        public UUID getReservationId() { return reservationId; }
        public void setReservationId(UUID reservationId) { this.reservationId = reservationId; }
        public String getClaimCode() { return claimCode; }
        public void setClaimCode(String claimCode) { this.claimCode = claimCode; }
        public String getClaimCodeLast4() { return claimCodeLast4; }
        public void setClaimCodeLast4(String claimCodeLast4) { this.claimCodeLast4 = claimCodeLast4; }
        public Instant getPickupStartAt() { return pickupStartAt; }
        public void setPickupStartAt(Instant pickupStartAt) { this.pickupStartAt = pickupStartAt; }
        public Instant getPickupEndAt() { return pickupEndAt; }
        public void setPickupEndAt(Instant pickupEndAt) { this.pickupEndAt = pickupEndAt; }
        public String getSellerName() { return sellerName; }
        public void setSellerName(String sellerName) { this.sellerName = sellerName; }
        public String getSellerLocation() { return sellerLocation; }
        public void setSellerLocation(String sellerLocation) { this.sellerLocation = sellerLocation; }
    }

    public static class VerifyRequest {
        private String claimCode;

        public VerifyRequest() {}

        public String getClaimCode() { return claimCode; }
        public void setClaimCode(String claimCode) { this.claimCode = claimCode; }
    }

    public static class VerifyResponse {
        private boolean success;
        private String message;

        public VerifyResponse() {}

        public VerifyResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
