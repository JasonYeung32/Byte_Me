package com.byteme.app;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    List<Reservation> findByOrganisationOrgId(UUID orgId);
    List<Reservation> findByPostingSellerSellerId(UUID sellerId);
    List<Reservation> findByPostingSellerSellerIdAndStatus(UUID sellerId, Reservation.Status status);
    List<Reservation> findByStatusAndPostingPickupEndAtBefore(Reservation.Status status, Instant cutoff);

    @Query("SELECT COUNT(DISTINCT r.posting.seller.sellerId) FROM Reservation r WHERE r.organisation.orgId = :orgId AND r.status = 'COLLECTED'")
    long countDistinctSellersByOrgId(UUID orgId);

    @Query("SELECT COUNT(DISTINCT r.posting.category.categoryId) FROM Reservation r WHERE r.organisation.orgId = :orgId AND r.status = 'COLLECTED' AND r.posting.category IS NOT NULL")
    long countDistinctCategoriesByOrgId(UUID orgId);
}
