package com.byteme.app;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final BundlePostingRepository bundleRepo;
    private final ReservationRepository reservationRepo;
    private final IssueReportRepository issueRepo;
    private final SellerRepository sellerRepo;

    public AnalyticsController(BundlePostingRepository bundleRepo, ReservationRepository reservationRepo,
                                IssueReportRepository issueRepo, SellerRepository sellerRepo) {
        this.bundleRepo = bundleRepo;
        this.reservationRepo = reservationRepo;
        this.issueRepo = issueRepo;
        this.sellerRepo = sellerRepo;
    }

    @GetMapping("/dashboard/{sellerId}")
    public ResponseEntity<?> getDashboard(@PathVariable UUID sellerId) {
        var seller = sellerRepo.findById(sellerId).orElse(null);
        if (seller == null) return ResponseEntity.notFound().build();

        var bundles = bundleRepo.findBySeller_SellerId(sellerId);
        
        int totalPosted = bundles.size();
        int totalQuantity = bundles.stream().mapToInt(BundlePosting::getQuantityTotal).sum();
        
        var collected = reservationRepo.findBySellerAndStatus(sellerId, Reservation.Status.COLLECTED);
        var noShows = reservationRepo.findBySellerAndStatus(sellerId, Reservation.Status.NO_SHOW);
        var expired = reservationRepo.findBySellerAndStatus(sellerId, Reservation.Status.EXPIRED);
        
        int collectedCount = collected.size();
        int noShowCount = noShows.size();
        int expiredCount = expired.size();
        
        double sellThrough = totalQuantity > 0 ? (double) collectedCount / totalQuantity * 100 : 0;
        int wasteAvoidedGrams = collectedCount * 300;
        int openIssues = issueRepo.findOpenBySeller(sellerId).size();

        return ResponseEntity.ok(new DashboardResponse(
                seller.getName(), totalPosted, totalQuantity, collectedCount,
                noShowCount, expiredCount, Math.round(sellThrough * 10) / 10.0,
                wasteAvoidedGrams, openIssues
        ));
    }

    @GetMapping("/sell-through/{sellerId}")
    public ResponseEntity<?> getSellThrough(@PathVariable UUID sellerId) {
        var collected = reservationRepo.findBySellerAndStatus(sellerId, Reservation.Status.COLLECTED);
        var noShows = reservationRepo.findBySellerAndStatus(sellerId, Reservation.Status.NO_SHOW);
        var expired = reservationRepo.findBySellerAndStatus(sellerId, Reservation.Status.EXPIRED);
        
        int total = collected.size() + noShows.size() + expired.size();
        
        return ResponseEntity.ok(new SellThroughResponse(
                collected.size(), noShows.size(), expired.size(),
                total > 0 ? (double) collected.size() / total * 100 : 0,
                total > 0 ? (double) noShows.size() / total * 100 : 0
        ));
    }

    @GetMapping("/waste/{sellerId}")
    public ResponseEntity<?> getWasteMetrics(@PathVariable UUID sellerId) {
        var collected = reservationRepo.findBySellerAndStatus(sellerId, Reservation.Status.COLLECTED);
        
        int bundlesCollected = collected.size();
        int weightSavedGrams = bundlesCollected * 300;
        double co2eSavedKg = weightSavedGrams * 2.5 / 1000;
        int mealsSaved = bundlesCollected;

        return ResponseEntity.ok(new WasteResponse(
                bundlesCollected, weightSavedGrams, co2eSavedKg, mealsSaved,
                "Average bundle weight: 300g",
                "CO2e factor: 2.5 kg CO2e per kg food waste avoided",
                "Meals estimate: 1 meal per bundle"
        ));
    }

    // DTOs
    public static class DashboardResponse {
        private String sellerName;
        private int totalBundlesPosted;
        private int totalQuantity;
        private int collectedCount;
        private int noShowCount;
        private int expiredCount;
        private double sellThroughRate;
        private int wasteAvoidedGrams;
        private int openIssueCount;

        public DashboardResponse() {}

        public DashboardResponse(String sellerName, int totalBundlesPosted, int totalQuantity,
                                  int collectedCount, int noShowCount, int expiredCount,
                                  double sellThroughRate, int wasteAvoidedGrams, int openIssueCount) {
            this.sellerName = sellerName;
            this.totalBundlesPosted = totalBundlesPosted;
            this.totalQuantity = totalQuantity;
            this.collectedCount = collectedCount;
            this.noShowCount = noShowCount;
            this.expiredCount = expiredCount;
            this.sellThroughRate = sellThroughRate;
            this.wasteAvoidedGrams = wasteAvoidedGrams;
            this.openIssueCount = openIssueCount;
        }

        public String getSellerName() { return sellerName; }
        public void setSellerName(String sellerName) { this.sellerName = sellerName; }
        public int getTotalBundlesPosted() { return totalBundlesPosted; }
        public void setTotalBundlesPosted(int totalBundlesPosted) { this.totalBundlesPosted = totalBundlesPosted; }
        public int getTotalQuantity() { return totalQuantity; }
        public void setTotalQuantity(int totalQuantity) { this.totalQuantity = totalQuantity; }
        public int getCollectedCount() { return collectedCount; }
        public void setCollectedCount(int collectedCount) { this.collectedCount = collectedCount; }
        public int getNoShowCount() { return noShowCount; }
        public void setNoShowCount(int noShowCount) { this.noShowCount = noShowCount; }
        public int getExpiredCount() { return expiredCount; }
        public void setExpiredCount(int expiredCount) { this.expiredCount = expiredCount; }
        public double getSellThroughRate() { return sellThroughRate; }
        public void setSellThroughRate(double sellThroughRate) { this.sellThroughRate = sellThroughRate; }
        public int getWasteAvoidedGrams() { return wasteAvoidedGrams; }
        public void setWasteAvoidedGrams(int wasteAvoidedGrams) { this.wasteAvoidedGrams = wasteAvoidedGrams; }
        public int getOpenIssueCount() { return openIssueCount; }
        public void setOpenIssueCount(int openIssueCount) { this.openIssueCount = openIssueCount; }
    }

    public static class SellThroughResponse {
        private int collected;
        private int noShow;
        private int expired;
        private double collectionRate;
        private double noShowRate;

        public SellThroughResponse() {}

        public SellThroughResponse(int collected, int noShow, int expired, double collectionRate, double noShowRate) {
            this.collected = collected;
            this.noShow = noShow;
            this.expired = expired;
            this.collectionRate = collectionRate;
            this.noShowRate = noShowRate;
        }

        public int getCollected() { return collected; }
        public void setCollected(int collected) { this.collected = collected; }
        public int getNoShow() { return noShow; }
        public void setNoShow(int noShow) { this.noShow = noShow; }
        public int getExpired() { return expired; }
        public void setExpired(int expired) { this.expired = expired; }
        public double getCollectionRate() { return collectionRate; }
        public void setCollectionRate(double collectionRate) { this.collectionRate = collectionRate; }
        public double getNoShowRate() { return noShowRate; }
        public void setNoShowRate(double noShowRate) { this.noShowRate = noShowRate; }
    }

    public static class WasteResponse {
        private int bundlesCollected;
        private int weightSavedGrams;
        private double co2eSavedKg;
        private int mealsSaved;
        private String weightAssumption;
        private String co2eAssumption;
        private String mealsAssumption;

        public WasteResponse() {}

        public WasteResponse(int bundlesCollected, int weightSavedGrams, double co2eSavedKg,
                              int mealsSaved, String weightAssumption, String co2eAssumption, String mealsAssumption) {
            this.bundlesCollected = bundlesCollected;
            this.weightSavedGrams = weightSavedGrams;
            this.co2eSavedKg = co2eSavedKg;
            this.mealsSaved = mealsSaved;
            this.weightAssumption = weightAssumption;
            this.co2eAssumption = co2eAssumption;
            this.mealsAssumption = mealsAssumption;
        }

        public int getBundlesCollected() { return bundlesCollected; }
        public void setBundlesCollected(int bundlesCollected) { this.bundlesCollected = bundlesCollected; }
        public int getWeightSavedGrams() { return weightSavedGrams; }
        public void setWeightSavedGrams(int weightSavedGrams) { this.weightSavedGrams = weightSavedGrams; }
        public double getCo2eSavedKg() { return co2eSavedKg; }
        public void setCo2eSavedKg(double co2eSavedKg) { this.co2eSavedKg = co2eSavedKg; }
        public int getMealsSaved() { return mealsSaved; }
        public void setMealsSaved(int mealsSaved) { this.mealsSaved = mealsSaved; }
        public String getWeightAssumption() { return weightAssumption; }
        public void setWeightAssumption(String weightAssumption) { this.weightAssumption = weightAssumption; }
        public String getCo2eAssumption() { return co2eAssumption; }
        public void setCo2eAssumption(String co2eAssumption) { this.co2eAssumption = co2eAssumption; }
        public String getMealsAssumption() { return mealsAssumption; }
        public void setMealsAssumption(String mealsAssumption) { this.mealsAssumption = mealsAssumption; }
    }
}
