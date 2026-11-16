package com.example.chat.user;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("{ '$and': [ " +
            "{ '_id': { '$ne': ?1 } }, " +
            "{ '$or': [ " +
            "  { 'displayName': { '$regex': ?0, '$options': 'i' } }, " +
            "  { 'email': { '$regex': ?0, '$options': 'i' } } " +
            "] } " +
            "] }")
    List<User> searchUsers(String query, String excludeUserId);

    List<User> findByIdIn(List<String> ids);
}
