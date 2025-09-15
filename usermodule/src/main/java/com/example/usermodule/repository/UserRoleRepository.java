package com.example.usermodule.repository;

import com.example.usermodule.data.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    @Query(value = """
        SELECT r.*
        FROM roles r
        JOIN user_roles ur ON r.id = ur.role_id
        WHERE ur.user_id = :userId
        """, nativeQuery = true)
    List<Object[]> findRolesByUserId(Long userId);
}


