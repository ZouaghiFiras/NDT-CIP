package com.network.digitaltwin.model;

import com.network.digitaltwin.model.auth.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity representing a policy activation event.
 */
@Entity
@Table(name = "policy_activations",
       indexes = {
           @Index(name = "idx_policy_activations_policy_id", columnList = "policy_id"),
           @Index(name = "idx_policy_activations_activation_date", columnList = "activation_date"),
           @Index(name = "idx_policy_activations_status", columnList = "status")
       })
public class PolicyActivation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Column(name = "activation_number", unique = true)
    private String activationNumber;

    @NotNull
    @Column(name = "activation_date", nullable = false)
    private LocalDate activationDate;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "status", nullable = false)
    private String status = "PENDING"; // PENDING, ACTIVE, EXPIRED, CANCELLED, CLAIM filed

    @Column(name = "premium_amount")
    private BigDecimal premiumAmount;

    @Column(name = "coverage_amount")
    private BigDecimal coverageAmount;

    @Column(name = "deductible")
    private BigDecimal deductible;

    @Column(name = "payment_status")
    private String paymentStatus = "PENDING"; // PENDING, PAID, PARTIAL, REFUNDED

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "transaction_id")
    private String transactionId;

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "activation_details", columnDefinition = "jsonb")
    private Map<String, Object> activationDetails = new HashMap<>();

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "claim_details", columnDefinition = "jsonb")
    private Map<String, Object> claimDetails = new HashMap<>();

    @Column(name = "claim_filed_at")
    private Instant claimFiledAt;

    @Column(name = "claim_amount")
    private BigDecimal claimAmount;

    @Column(name = "claim_status")
    private String claimStatus; // FILED, INVESTIGATING, APPROVED, REJECTED, SETTLED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activated_by")
    private User activatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "notes")
    private String notes;

    // Constructors
    public PolicyActivation() {
    }

    public PolicyActivation(Policy policy, LocalDate activationDate, User activatedBy) {
        this.policy = policy;
        this.activationDate = activationDate;
        this.activatedBy = activatedBy;
        this.effectiveDate = activationDate;
        this.expiryDate = activationDate.plusYears(1); // Assuming 1-year policies
        this.activationNumber = generateActivationNumber();
    }

    // Getters and Setters
    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    public String getActivationNumber() {
        return activationNumber;
    }

    public void setActivationNumber(String activationNumber) {
        this.activationNumber = activationNumber;
    }

    public LocalDate getActivationDate() {
        return activationDate;
    }

    public void setActivationDate(LocalDate activationDate) {
        this.activationDate = activationDate;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getPremiumAmount() {
        return premiumAmount;
    }

    public void setPremiumAmount(BigDecimal premiumAmount) {
        this.premiumAmount = premiumAmount;
    }

    public BigDecimal getCoverageAmount() {
        return coverageAmount;
    }

    public void setCoverageAmount(BigDecimal coverageAmount) {
        this.coverageAmount = coverageAmount;
    }

    public BigDecimal getDeductible() {
        return deductible;
    }

    public void setDeductible(BigDecimal deductible) {
        this.deductible = deductible;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Map<String, Object> getActivationDetails() {
        return activationDetails;
    }

    public void setActivationDetails(Map<String, Object> activationDetails) {
        this.activationDetails = activationDetails;
    }

    public Map<String, Object> getClaimDetails() {
        return claimDetails;
    }

    public void setClaimDetails(Map<String, Object> claimDetails) {
        this.claimDetails = claimDetails;
    }

    public Instant getClaimFiledAt() {
        return claimFiledAt;
    }

    public void setClaimFiledAt(Instant claimFiledAt) {
        this.claimFiledAt = claimFiledAt;
    }

    public BigDecimal getClaimAmount() {
        return claimAmount;
    }

    public void setClaimAmount(BigDecimal claimAmount) {
        this.claimAmount = claimAmount;
    }

    public String getClaimStatus() {
        return claimStatus;
    }

    public void setClaimStatus(String claimStatus) {
        this.claimStatus = claimStatus;
    }

    public User getActivatedBy() {
        return activatedBy;
    }

    public void setActivatedBy(User activatedBy) {
        this.activatedBy = activatedBy;
    }

    public User getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(User approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // Convenience methods
    public void addActivationDetail(String key, Object value) {
        this.activationDetails.put(key, value);
    }

    public void addClaimDetail(String key, Object value) {
        this.claimDetails.put(key, value);
    }

    public void activate() {
        this.status = "ACTIVE";
        this.updatedAt = Instant.now();
    }

    public void expire() {
        this.status = "EXPIRED";
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        this.status = "CANCELLED";
        this.updatedAt = Instant.now();
    }

    public void markPaymentPaid() {
        this.paymentStatus = "PAID";
        this.updatedAt = Instant.now();
    }

    public void markPaymentPartial(BigDecimal amount) {
        this.paymentStatus = "PARTIAL";
        this.updatedAt = Instant.now();
    }

    public void markPaymentRefunded() {
        this.paymentStatus = "REFUNDED";
        this.updatedAt = Instant.now();
    }

    public void fileClaim(BigDecimal amount, Map<String, Object> details) {
        this.status = "CLAIM filed";
        this.claimFiledAt = Instant.now();
        this.claimAmount = amount;
        this.claimDetails = details;
        this.updatedAt = Instant.now();
    }

    public void approveClaim(User user) {
        this.claimStatus = "APPROVED";
        this.approvedBy = user;
        this.approvedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void rejectClaim(String reason) {
        this.claimStatus = "REJECTED";
        this.addClaimDetail("rejection_reason", reason);
        this.updatedAt = Instant.now();
    }

    public void settleClaim(BigDecimal amount, User user) {
        this.claimStatus = "SETTLED";
        this.approvedBy = user;
        this.approvedAt = Instant.now();
        this.addClaimDetail("settlement_amount", amount);
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public boolean isExpired() {
        return "EXPIRED".equals(status);
    }

    public boolean isCancelled() {
        return "CANCELLED".equals(status);
    }

    public boolean isClaimFiled() {
        return "CLAIM filed".equals(status);
    }

    public boolean isPaymentPaid() {
        return "PAID".equals(paymentStatus);
    }

    public boolean isPaymentPending() {
        return "PENDING".equals(paymentStatus);
    }

    public boolean isPaymentPartial() {
        return "PARTIAL".equals(paymentStatus);
    }

    public boolean isPaymentRefunded() {
        return "REFUNDED".equals(paymentStatus);
    }

    public boolean isClaimApproved() {
        return "APPROVED".equals(claimStatus);
    }

    public boolean isClaimRejected() {
        return "REJECTED".equals(claimStatus);
    }

    public boolean isClaimSettled() {
        return "SETTLED".equals(claimStatus);
    }

    public boolean isClaimFiled() {
        return claimFiledAt != null;
    }

    public boolean isClaimInProgress() {
        return ("FILED".equals(claimStatus) || "INVESTIGATING".equals(claimStatus)) && !isClaimApproved() && !isClaimRejected();
    }

    public boolean isWithinCoveragePeriod() {
        LocalDate today = LocalDate.now();
        return !today.isBefore(effectiveDate) && !today.isAfter(expiryDate);
    }

    private String generateActivationNumber() {
        // Generate a unique activation number (e.g., ACT-YYYYMMDD-XXXXX)
        return String.format("ACT-%s-%05d", 
            LocalDate.now().toString().replace("-", ""), 
            (int)(Math.random() * 100000));
    }

    @Override
    public String toString() {
        return "PolicyActivation{" +
                "id=" + getId() +
                ", policy=" + (policy != null ? policy.getName() : "null") +
                ", activationNumber='" + activationNumber + '\'' +
                ", status='" + status + '\'' +
                ", activationDate=" + activationDate +
                '}';
    }
}
