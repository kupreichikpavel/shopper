package by.innowise.userservice.repository;

import by.innowise.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmailIgnoreCase(String email);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE User user
            SET user.active = :active
            WHERE user.id = :id
            """)
    int updateActiveById(@Param("id") Long id, @Param("active") boolean active);
}
