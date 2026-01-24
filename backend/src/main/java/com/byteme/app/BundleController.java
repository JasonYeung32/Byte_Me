package com.byteme.app;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bundles")
public class BundleController {

    private final BundlePostingRepository bundleRepo;
    private final SellerRepository sellerRepo;
    private final CategoryRepository categoryRepo;

    public BundleController(BundlePostingRepository bundleRepo, SellerRepository sellerRepo, CategoryRepository categoryRepo) {
        this.bundleRepo = bundleRepo;
        this.sellerRepo = sellerRepo;
        this.categoryRepo = categoryRepo;
    }

    @GetMapping
    public Page<BundlePosting> getAvailable(Pageable pageable) {
        return bundleRepo.findAvailable(Instant.now(), pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BundlePosting> getById(@PathVariable UUID id) {
        return bundleRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/seller/{sellerId}")
    public List<BundlePosting> getBySeller(@PathVariable UUID sellerId) {
        return bundleRepo.findBySeller_SellerId(sellerId);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateBundleRequest req) {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var seller = sellerRepo.findByUser_UserId(userId)
                .orElseThrow(() -> new RuntimeException("Not a seller"));

        BundlePosting bundle = new BundlePosting();
        bundle.setSeller(seller);
        if (req.getCategoryId() != null) {
            bundle.setCategory(categoryRepo.findById(req.getCategoryId()).orElse(null));
        }
        bundle.setPickupStartAt(req.getPickupStartAt());
        bundle.setPickupEndAt(req.getPickupEndAt());
        bundle.setQuantityTotal(req.getQuantityTotal());
        bundle.setQuantityReserved(0);
        bundle.setPriceCents(req.getPriceCents());
        bundle.setDiscountPct(req.getDiscountPct());
        bundle.setContentsText(req.getContentsText());
        bundle.setAllergensText(req.getAllergensText());
        bundle.setEstimatedWeightGrams(req.getEstimatedWeightGrams());
        bundle.setStatus(req.isActivate() ? BundlePosting.Status.ACTIVE : BundlePosting.Status.DRAFT);

        return ResponseEntity.ok(bundleRepo.save(bundle));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody UpdateBundleRequest req) {
        var bundle = bundleRepo.findById(id).orElse(null);
        if (bundle == null) return ResponseEntity.notFound().build();

        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!bundle.getSeller().getUser().getUserId().equals(userId)) {
            return ResponseEntity.status(403).body("Not your bundle");
        }

        if (req.getQuantityTotal() != null) bundle.setQuantityTotal(req.getQuantityTotal());
        if (req.getPriceCents() != null) bundle.setPriceCents(req.getPriceCents());
        if (req.getDiscountPct() != null) bundle.setDiscountPct(req.getDiscountPct());
        if (req.getContentsText() != null) bundle.setContentsText(req.getContentsText());
        if (req.getAllergensText() != null) bundle.setAllergensText(req.getAllergensText());

        return ResponseEntity.ok(bundleRepo.save(bundle));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<?> activate(@PathVariable UUID id) {
        var bundle = bundleRepo.findById(id).orElse(null);
        if (bundle == null) return ResponseEntity.notFound().build();
        bundle.setStatus(BundlePosting.Status.ACTIVE);
        return ResponseEntity.ok(bundleRepo.save(bundle));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<?> close(@PathVariable UUID id) {
        var bundle = bundleRepo.findById(id).orElse(null);
        if (bundle == null) return ResponseEntity.notFound().build();
        bundle.setStatus(BundlePosting.Status.CLOSED);
        return ResponseEntity.ok(bundleRepo.save(bundle));
    }

    // DTOs
    public static class CreateBundleRequest {
        private UUID categoryId;
        private Instant pickupStartAt;
        private Instant pickupEndAt;
        private Integer quantityTotal;
        private Integer priceCents;
        private Integer discountPct;
        private String contentsText;
        private String allergensText;
        private Integer estimatedWeightGrams;
        private boolean activate;

        public CreateBundleRequest() {}

        public UUID getCategoryId() { return categoryId; }
        public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
        public Instant getPickupStartAt() { return pickupStartAt; }
        public void setPickupStartAt(Instant pickupStartAt) { this.pickupStartAt = pickupStartAt; }
        public Instant getPickupEndAt() { return pickupEndAt; }
        public void setPickupEndAt(Instant pickupEndAt) { this.pickupEndAt = pickupEndAt; }
        public Integer getQuantityTotal() { return quantityTotal; }
        public void setQuantityTotal(Integer quantityTotal) { this.quantityTotal = quantityTotal; }
        public Integer getPriceCents() { return priceCents; }
        public void setPriceCents(Integer priceCents) { this.priceCents = priceCents; }
        public Integer getDiscountPct() { return discountPct; }
        public void setDiscountPct(Integer discountPct) { this.discountPct = discountPct; }
        public String getContentsText() { return contentsText; }
        public void setContentsText(String contentsText) { this.contentsText = contentsText; }
        public String getAllergensText() { return allergensText; }
        public void setAllergensText(String allergensText) { this.allergensText = allergensText; }
        public Integer getEstimatedWeightGrams() { return estimatedWeightGrams; }
        public void setEstimatedWeightGrams(Integer estimatedWeightGrams) { this.estimatedWeightGrams = estimatedWeightGrams; }
        public boolean isActivate() { return activate; }
        public void setActivate(boolean activate) { this.activate = activate; }
    }

    public static class UpdateBundleRequest {
        private Integer quantityTotal;
        private Integer priceCents;
        private Integer discountPct;
        private String contentsText;
        private String allergensText;

        public UpdateBundleRequest() {}

        public Integer getQuantityTotal() { return quantityTotal; }
        public void setQuantityTotal(Integer quantityTotal) { this.quantityTotal = quantityTotal; }
        public Integer getPriceCents() { return priceCents; }
        public void setPriceCents(Integer priceCents) { this.priceCents = priceCents; }
        public Integer getDiscountPct() { return discountPct; }
        public void setDiscountPct(Integer discountPct) { this.discountPct = discountPct; }
        public String getContentsText() { return contentsText; }
        public void setContentsText(String contentsText) { this.contentsText = contentsText; }
        public String getAllergensText() { return allergensText; }
        public void setAllergensText(String allergensText) { this.allergensText = allergensText; }
    }
}
