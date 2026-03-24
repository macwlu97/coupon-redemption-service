package com.redemption.usage.application;

import com.redemption.usage.domain.exception.UsageException;
import com.redemption.usage.domain.model.UsageHistory;
import com.redemption.usage.domain.repository.UsageHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsageHistoryService {

    private final UsageHistoryRepository repository;

    /**
     * Persists the usage record in a new transaction.
     * Handles potential race conditions using DB unique constraints.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistUsage(String code, String userId) {
        try {
            log.debug("Persisting usage history | Code: {} | User: {}", code, userId);
            repository.save(new UsageHistory(code, userId));
        } catch (DataIntegrityViolationException e) {
            // This happens if two threads pass the initial check but one saves first.
            // DB UniqueConstraint is our ultimate source of truth.
            log.error("Race condition detected: Duplicate usage for {} by {}", code, userId);
            throw new UsageException.AlreadyRedeemed(code, userId);
        }
    }
}