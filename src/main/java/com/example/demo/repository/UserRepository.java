package com.example.demo.repository;

import com.example.demo.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class UserRepository {
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public UserRepository(DynamoDbClient dynamoDbClient,
                          @Value("${aws.dynamodb.user-table-name}") String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    public void save(User user) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("userId", AttributeValue.fromS(user.getUserId()));
        item.put("email", AttributeValue.fromS(user.getEmail()));
        item.put("username", AttributeValue.fromS(user.getUsername()));
        item.put("passwordHash", AttributeValue.fromS(user.getPasswordHash()));
        item.put("role", AttributeValue.fromS(user.getRole()));
        item.put("firstName", AttributeValue.fromS(user.getFirstName()));
        item.put("lastName", AttributeValue.fromS(user.getLastName()));
        item.put("phone", AttributeValue.fromS(user.getPhone()));
        item.put("dateOfBirth", AttributeValue.fromS(user.getDateOfBirth()));
        item.put("riskAppetite", AttributeValue.fromS(user.getRiskAppetite()));
        item.put("experience", AttributeValue.fromS(user.getExperience()));
        item.put("investmentGoal", AttributeValue.fromS(user.getInvestmentGoal()));
        item.put("createdAt", AttributeValue.fromS(user.getCreatedAt()));
        item.put("updatedAt", AttributeValue.fromS(user.getUpdatedAt()));

        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();
        dynamoDbClient.putItem(request);
    }

    // ✅ GSI QUERY - UNLIMITED USERS!
    public Optional<User> findByEmail(String email) {
        try {
            QueryRequest request = QueryRequest.builder()
                    .tableName(tableName)
                    .indexName("EmailIndex")
                    .keyConditionExpression("email = :email")
                    .expressionAttributeValues(Map.of(":email", AttributeValue.fromS(email)))
                    .limit(1)
                    .build();

            QueryResponse response = dynamoDbClient.query(request);
            
            if (response.items().isEmpty()) {
                System.out.println("🔍 User not found in EmailIndex: " + email);
                return Optional.empty();
            }

            Map<String, AttributeValue> item = response.items().get(0);
            User user = mapToUser(item);
            System.out.println("✅ User found via GSI: " + user.getEmail());
            return Optional.of(user);

        } catch (Exception e) {
            System.err.println("❌ GSI Query failed, using SCAN fallback: " + e.getMessage());
            return findByEmailScanFallback(email);
        }
    }

    // ✅ GSI existsByEmail
    public boolean existsByEmail(String email) {
        try {
            QueryRequest request = QueryRequest.builder()
                    .tableName(tableName)
                    .indexName("EmailIndex")
                    .keyConditionExpression("email = :email")
                    .expressionAttributeValues(Map.of(":email", AttributeValue.fromS(email)))
                    .limit(1)
                    .build();

            QueryResponse response = dynamoDbClient.query(request);
            boolean exists = !response.items().isEmpty();
            System.out.println("🔍 Email exists check: " + email + " → " + exists);
            return exists;

        } catch (Exception e) {
            System.err.println("❌ GSI existsByEmail failed, using SCAN: " + e.getMessage());
            return existsByEmailScan(email);
        }
    }

    public boolean existsByUsername(String username) {
        return existsByEmailScan(username.replaceAll("username", "username")); // Reuse SCAN
    }

    // 🔧 FALLBACK SCAN METHODS
    private Optional<User> findByEmailScanFallback(String email) {
        ScanRequest request = ScanRequest.builder()
                .tableName(tableName)
                .filterExpression("email = :email")
                .expressionAttributeValues(Map.of(":email", AttributeValue.fromS(email)))
                .limit(1)
                .build();
        ScanResponse response = dynamoDbClient.scan(request);
        if (response.items().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapToUser(response.items().get(0)));
    }

    private boolean existsByEmailScan(String value) {
        ScanRequest request = ScanRequest.builder()
                .tableName(tableName)
                .filterExpression("email = :email")
                .expressionAttributeValues(Map.of(":email", AttributeValue.fromS(value)))
                .limit(1)
                .build();
        return !dynamoDbClient.scan(request).items().isEmpty();
    }

    // ✅ FIXED SAFE MAPPING
    private User mapToUser(Map<String, AttributeValue> item) {
        User user = new User();
        user.setUserId(getStringOrEmpty(item, "userId"));
        user.setEmail(getStringOrEmpty(item, "email"));
        user.setUsername(getStringOrEmpty(item, "username"));
        user.setPasswordHash(getStringOrEmpty(item, "passwordHash"));
        user.setRole(getStringOrEmpty(item, "role"));
        user.setFirstName(getStringOrEmpty(item, "firstName"));
        user.setLastName(getStringOrEmpty(item, "lastName"));
        user.setPhone(getStringOrEmpty(item, "phone"));
        user.setDateOfBirth(getStringOrEmpty(item, "dateOfBirth"));
        user.setRiskAppetite(getStringOrEmpty(item, "riskAppetite"));
        user.setExperience(getStringOrEmpty(item, "experience"));
        user.setInvestmentGoal(getStringOrEmpty(item, "investmentGoal"));
        user.setCreatedAt(getStringOrEmpty(item, "createdAt"));
        user.setUpdatedAt(getStringOrEmpty(item, "updatedAt"));
        return user;
    }

    // ✅ FIXED: No hasValue() - Simple null check
    private String getStringOrEmpty(Map<String, AttributeValue> item, String key) {
        AttributeValue value = item.get(key);
        return value != null && value.s() != null ? value.s() : "";
    }
}