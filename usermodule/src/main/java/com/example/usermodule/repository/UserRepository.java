package com.example.usermodule.repository;

import com.example.usermodule.data.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailCustom(String email);

    // join user + profile
    @Query(value = """
        SELECT u.*, p.name, p.avatar
        FROM users u
        JOIN user_profiles p ON u.id = p.user_id
        WHERE u.id = :userId
        """, nativeQuery = true)
    Object findUserWithProfile(Long userId);
}


