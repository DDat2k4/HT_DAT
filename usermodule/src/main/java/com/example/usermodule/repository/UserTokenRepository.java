package com.example.usermodule.repository;

import com.example.usermodule.data.entity.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, Long> {

    @Query("SELECT t FROM UserToken t WHERE t.userId = :userId AND t.revoked = false")
    List<UserToken> findActiveTokensByUserId(Long userId);

    Optional<UserToken> findByRefreshTokenAndRevokedFalse(String refreshToken);

    @Modifying
    @Query("UPDATE UserToken t SET t.revoked = true WHERE t.userId = :userId AND t.revoked = false")
    void revokeAllTokensByUserId(Long userId);
}
