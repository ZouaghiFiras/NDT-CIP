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
 * Entity representing an insurance policy.
 */
@Entity
@Table(name = "policies",
       indexes = {
           @Index(name = "idx_policies_name", columnList = "name"),
           @Index(name = "idx_policies_policy_number", columnList = "policy_number"),
           @Index(name = "idx_policies_status", columnList = "status"),
           @Index(name = "idx_policies_policy_type", columnList = "policy_type")
       })
public class Policy extends BaseEntity {

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "policy_number", unique = true)
    private String policyNumber;

    @Column(name = "description")
    private String description;

    @Column(name = "policy_type", nullable = false)
    private String policyType; // e.g., "CYBER_LIABILITY", "FIRST_PARTY", "ERRORS_AND_OMISSIONS"

    @Column(name = "status", nullable = false)
    private String status = "DRAFT"; // DRAFT, ACTIVE, EXPIRED, CANCELLED, PENDING_RENEWAL

    @Column(name = "coverage_amount")
    private BigDecimal coverageAmount;

    @Column(name = "premium_amount")
    private BigDecimal premiumAmount;

    @Column(name = "deductible")
    private BigDecimal deductible;

    @Column(name = "policy_start_date")
    private LocalDate policyStartDate;

    @Column(name = "policy_end_date")
    private LocalDate policyEndDate;

    @Column(name = "renewal_date")
    private LocalDate renewalDate;

    @Column(name = "grace_period_days")
    private Integer gracePeriodDays = 30;

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "coverage_details", columnDefinition = "jsonb")
    private Map<String, Object> coverageDetails = new HashMap<>();

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "exclusions", columnDefinition = "jsonb")
    private Map<String, Object> exclusions = new HashMap<>();

    @JdbcTypeCode(java.sql.Types.JAVA_OBJECT)
    @Column(name = "terms_conditions", columnDefinition = "jsonb")
    private Map<String, Object> termsConditions = new HashMap<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_holder_id")
    private User policyHolder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private User agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "underwriter_id")
    private User underwriter;

    @Column(name = "brokerage_fees")
    private BigDecimal brokerageFees = BigDecimal.ZERO;

    @Column(name = "administrative_fees")
    private BigDecimal administrativeFees = BigDecimal.ZERO;

    @Column(name = "tax_rate")
    private BigDecimal taxRate = new BigDecimal("0.08"); // 8% default tax rate

    @Column(name = "currency")
    private String currency = "USD";

    @Column(name = "policy_document_url")
    private String policyDocumentUrl;

    @Column(name = "last_renewed_at")
    private Instant lastRenewedAt;

    @Column(name = "renewal_count")
    private Integer renewalCount = 0;

    @Column(name = "notes")
    private String notes;

    // Constructors
    public Policy() {
    }

    public Policy(String name, String policyType, User policyHolder) {
        this.name = name;
        this.policyType = policyType;
        this.policyHolder = policyHolder;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPolicyType() {
        return policyType;
    }

    public void setPolicyType(String policyType) {
        this.policyType = policyType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getCoverageAmount() {
        return coverageAmount;
    }

    public void setCoverageAmount(BigDecimal coverageAmount) {
        this.coverageAmount = coverageAmount;
    }

    public BigDecimal getPremiumAmount() {
        return premiumAmount;
    }

    public void setPremiumAmount(BigDecimal premiumAmount) {
        this.premiumAmount = premiumAmount;
    }

    public BigDecimal getDeductible() {
        return deductible;
    }

    public void setDeductible(BigDecimal deductible) {
        this.deductible = deductible;
    }

    public LocalDate getPolicyStartDate() {
        return policyStartDate;
    }

    public void setPolicyStartDate(LocalDate policyStartDate) {
        this.policyStartDate = policyStartDate;
    }

    public LocalDate getPolicyEndDate() {
        return policyEndDate;
    }

    public void setPolicyEndDate(LocalDate policyEndDate) {
        this.policyEndDate = policyEndDate;
    }

    public LocalDate getRenewalDate() {
        return renewalDate;
    }

    public void setRenewalDate(LocalDate renewalDate) {
        this.renewalDate = renewalDate;
    }

    public Integer getGracePeriodDays() {
        return gracePeriodDays;
    }

    public void setGracePeriodDays(Integer gracePeriodDays) {
        this.gracePeriodDays = gracePeriodDays;
    }

    public Map<String, Object> getCoverageDetails() {
        return coverageDetails;
    }

    public void setCoverageDetails(Map<String, Object> coverageDetails) {
        this.coverageDetails = coverageDetails;
    }

    public Map<String, Object> getExclusions() {
        return exclusions;
    }

    public void setExclusions(Map<String, Object> exclusions) {
        this.exclusions = exclusions;
    }

    public Map<String, Object> getTermsConditions() {
        return termsConditions;
    }

    public void setTermsConditions(Map<String, Object> termsConditions) {
        this.termsConditions = termsConditions;
    }

    public User getPolicyHolder() {
        return policyHolder;
    }

    public void setPolicyHolder(User policyHolder) {
        this.policyHolder = policyHolder;
    }

    public User getAgent() {
        return agent;
    }

    public void setAgent(User agent) {
        this.agent = agent;
    }

    public User getUnderwriter() {
        return underwriter;
    }

    public void setUnderwriter(User underwriter) {
        this.underwriter = underwriter;
    }

    public BigDecimal getBrokerageFees() {
        return brokerageFees;
    }

    public void setBrokerageFees(BigDecimal brokerageFees) {
        this.brokerageFees = brokerageFees;
    }

    public BigDecimal getAdministrativeFees() {
        return administrativeFees;
    }

    public void setAdministrativeFees(BigDecimal administrativeFees) {
        this.administrativeFees = administrativeFees;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPolicyDocumentUrl() {
        return policyDocumentUrl;
    }

    public void setPolicyDocumentUrl(String policyDocumentUrl) {
        this.policyDocumentUrl = policyDocumentUrl;
    }

    public Instant getLastRenewedAt() {
        return lastRenewedAt;
    }

    public void setLastRenewedAt(Instant lastRenewedAt) {
        this.lastRenewedAt = lastRenewedAt;
    }

    public Integer getRenewalCount() {
        return renewalCount;
    }

    public void setRenewalCount(Integer renewalCount) {
        this.renewalCount = renewalCount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // Convenience methods
    public void addCoverageDetail(String key, Object value) {
        this.coverageDetails.put(key, value);
    }

    public void addExclusion(String key, Object value) {
        this.exclusions.put(key, value);
    }

    public void addTermCondition(String key, Object value) {
        this.termsConditions.put(key, value);
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

    public void markForRenewal() {
        this.status = "PENDING_RENEWAL";
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

    public boolean isPendingRenewal() {
        return "PENDING_RENEWAL".equals(status);
    }

    public boolean isDraft() {
        return "DRAFT".equals(status);
    }

    public boolean isWithinGracePeriod() {
        if (policyEndDate == null || renewalDate == null) {
            return false;
        }

        LocalDate today = LocalDate.now();
        LocalDate gracePeriodEnd = policyEndDate.plusDays(gracePeriodDays);
        return !today.isAfter(gracePeriodEnd);
    }

    public void recordRenewal() {
        this.lastRenewedAt = Instant.now();
        this.renewalCount++;
        this.policyStartDate = policyEndDate;
        this.policyEndDate = policyEndDate.plusYears(1); // Assuming 1-year policies
        this.renewalDate = policyEndDate;
        this.status = "ACTIVE";
        this.updatedAt = Instant.now();
    }

    public BigDecimal calculateTotalPremium() {
        BigDecimal total = premiumAmount != null ? premiumAmount : BigDecimal.ZERO;

        if (brokerageFees != null) {
            total = total.add(brokerageFees);
        }

        if (administrativeFees != null) {
            total = total.add(administrativeFees);
        }

        if (taxRate != null && premiumAmount != null) {
            BigDecimal tax = premiumAmount.multiply(taxRate);
            total = total.add(tax);
        }

        return total;
    }

    @Override
    public String toString() {
        return "Policy{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", policyNumber='" + policyNumber + '\'' +
                ", policyType='" + policyType + '\'' +
                ", status='" + status + '\'' +
                ", policyHolder=" + (policyHolder != null ? policyHolder.getUsername() : "null") +
                '}';
    }
}
