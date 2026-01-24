package com.byteme.app;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/gamification")
public class GamificationController {

    private final EmployeeRepository employeeRepo;
    private final RescueEventRepository rescueEventRepo;
    private final BadgeRepository badgeRepo;
    private final EmployeeBadgeRepository employeeBadgeRepo;

    public GamificationController(EmployeeRepository employeeRepo, RescueEventRepository rescueEventRepo,
                                   BadgeRepository badgeRepo, EmployeeBadgeRepository employeeBadgeRepo) {
        this.employeeRepo = employeeRepo;
        this.rescueEventRepo = rescueEventRepo;
        this.badgeRepo = badgeRepo;
        this.employeeBadgeRepo = employeeBadgeRepo;
    }

    @GetMapping("/streak/{employeeId}")
    public ResponseEntity<?> getStreak(@PathVariable UUID employeeId) {
        var employee = employeeRepo.findById(employeeId).orElse(null);
        if (employee == null) return ResponseEntity.notFound().build();

        return ResponseEntity.ok(new StreakResponse(
                employee.getCurrentStreakWeeks(),
                employee.getBestStreakWeeks(),
                employee.getLastRescueWeekStart()
        ));
    }

    @GetMapping("/impact/{employeeId}")
    public ResponseEntity<?> getImpact(@PathVariable UUID employeeId) {
        var employee = employeeRepo.findById(employeeId).orElse(null);
        if (employee == null) return ResponseEntity.notFound().build();

        long totalRescues = rescueEventRepo.countByEmployee_EmployeeId(employeeId);
        long totalMeals = rescueEventRepo.sumMealsByEmployee(employeeId);
        long totalCo2eGrams = rescueEventRepo.sumCo2eByEmployee(employeeId);
        int badgeCount = employeeBadgeRepo.findByEmployeeId(employeeId).size();

        return ResponseEntity.ok(new ImpactResponse(
                (int) totalRescues,
                (int) totalMeals,
                totalCo2eGrams / 1000.0,
                employee.getCurrentStreakWeeks(),
                badgeCount
        ));
    }

    @GetMapping("/badges/{employeeId}")
    public List<EmployeeBadge> getEmployeeBadges(@PathVariable UUID employeeId) {
        return employeeBadgeRepo.findByEmployeeId(employeeId);
    }

    @GetMapping("/badges")
    public List<Badge> getAllBadges() {
        return badgeRepo.findAll();
    }

    // DTOs
    public static class StreakResponse {
        private int currentStreakWeeks;
        private int bestStreakWeeks;
        private LocalDate lastRescueWeekStart;

        public StreakResponse() {}

        public StreakResponse(int currentStreakWeeks, int bestStreakWeeks, LocalDate lastRescueWeekStart) {
            this.currentStreakWeeks = currentStreakWeeks;
            this.bestStreakWeeks = bestStreakWeeks;
            this.lastRescueWeekStart = lastRescueWeekStart;
        }

        public int getCurrentStreakWeeks() { return currentStreakWeeks; }
        public void setCurrentStreakWeeks(int currentStreakWeeks) { this.currentStreakWeeks = currentStreakWeeks; }
        public int getBestStreakWeeks() { return bestStreakWeeks; }
        public void setBestStreakWeeks(int bestStreakWeeks) { this.bestStreakWeeks = bestStreakWeeks; }
        public LocalDate getLastRescueWeekStart() { return lastRescueWeekStart; }
        public void setLastRescueWeekStart(LocalDate lastRescueWeekStart) { this.lastRescueWeekStart = lastRescueWeekStart; }
    }

    public static class ImpactResponse {
        private int totalRescues;
        private int totalMealsSaved;
        private double totalCo2eSavedKg;
        private int currentStreakWeeks;
        private int badgesEarned;

        public ImpactResponse() {}

        public ImpactResponse(int totalRescues, int totalMealsSaved, double totalCo2eSavedKg, int currentStreakWeeks, int badgesEarned) {
            this.totalRescues = totalRescues;
            this.totalMealsSaved = totalMealsSaved;
            this.totalCo2eSavedKg = totalCo2eSavedKg;
            this.currentStreakWeeks = currentStreakWeeks;
            this.badgesEarned = badgesEarned;
        }

        public int getTotalRescues() { return totalRescues; }
        public void setTotalRescues(int totalRescues) { this.totalRescues = totalRescues; }
        public int getTotalMealsSaved() { return totalMealsSaved; }
        public void setTotalMealsSaved(int totalMealsSaved) { this.totalMealsSaved = totalMealsSaved; }
        public double getTotalCo2eSavedKg() { return totalCo2eSavedKg; }
        public void setTotalCo2eSavedKg(double totalCo2eSavedKg) { this.totalCo2eSavedKg = totalCo2eSavedKg; }
        public int getCurrentStreakWeeks() { return currentStreakWeeks; }
        public void setCurrentStreakWeeks(int currentStreakWeeks) { this.currentStreakWeeks = currentStreakWeeks; }
        public int getBadgesEarned() { return badgesEarned; }
        public void setBadgesEarned(int badgesEarned) { this.badgesEarned = badgesEarned; }
    }
}
