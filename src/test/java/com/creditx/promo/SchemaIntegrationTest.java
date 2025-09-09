package com.creditx.promo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;

@Testcontainers
@JdbcTest
@ActiveProfiles("test")
class SchemaIntegrationTest {

  @SuppressWarnings("resource")
  @Container
  static final OracleContainer oracle = new OracleContainer(
      "gvenzl/oracle-free:latest-faststart").withUsername("testuser").withPassword("testpassword");
  @Autowired
  private JdbcTemplate jdbcTemplate;

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", oracle::getJdbcUrl);
    registry.add("spring.datasource.username", oracle::getUsername);
    registry.add("spring.datasource.password", oracle::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "oracle.jdbc.OracleDriver");
  }

  @Test
  void testFlywayAppliedSchema() {
    // Core tables existence
    assertThat(tableExists("CPRS_PROMOTIONS")).isEqualTo(1);
    assertThat(tableExists("CPRS_PROMO_APPLICATIONS")).isEqualTo(1);
    assertThat(tableExists("CPRS_OUTBOX_EVENTS")).isEqualTo(1);
    assertThat(tableExists("CPRS_PROCESSED_EVENTS")).isEqualTo(1);

    // Insert a promotion (UUID generated here for simplicity)
    String promoId = "11111111-1111-1111-1111-111111111111";
    jdbcTemplate.update("""
        INSERT INTO CPRS_PROMOTIONS (PROMO_ID, NAME, DESCRIPTION, START_DATE, EXPIRY_DATE, ELIGIBILITY_RULES, REWARD_FORMULA, STATUS)
        VALUES (?, 'Welcome Bonus', 'Desc', SYSTIMESTAMP, SYSTIMESTAMP + INTERVAL '30' DAY, '{"minAmount":100}', '{"cashbackPercent":5}', 'ACTIVE')
        """, promoId);

    Integer promoCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM CPRS_PROMOTIONS WHERE PROMO_ID = ?", Integer.class, promoId);
    assertThat(promoCount).isEqualTo(1);

    // Insert a promo application referencing the promo
    String applicationId = "22222222-2222-2222-2222-222222222222";
    String idempotencyKey = "txn-123:" + promoId;
    jdbcTemplate.update("""
        INSERT INTO CPRS_PROMO_APPLICATIONS (APPLICATION_ID, PROMO_ID, TRANSACTION_ID, ISSUER_ID, MERCHANT_ID, INITIATED_AT, CASHBACK_AMOUNT, STATUS, IDEMPOTENCY_KEY)
        VALUES (?, ?, 'txn-123', 'issuer-1', 'merchant-9', SYSTIMESTAMP, 10.50, 'APPLIED', ?)
        """, applicationId, promoId, idempotencyKey);

    Integer appCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM CPRS_PROMO_APPLICATIONS WHERE APPLICATION_ID = ?", Integer.class,
        applicationId);
    assertThat(appCount).isEqualTo(1);

    // Insert processed event
    jdbcTemplate.update("""
        INSERT INTO CPRS_PROCESSED_EVENTS (EVENT_ID, PAYLOAD_HASH, STATUS, PROCESSED_AT)
        VALUES ('event-1', 'hash-abc', 'PROCESSED', SYSTIMESTAMP)
        """);
    Integer processedCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM CPRS_PROCESSED_EVENTS WHERE EVENT_ID = 'event-1'", Integer.class);
    assertThat(processedCount).isEqualTo(1);

    // Insert outbox event (trigger should assign EVENT_ID sequence if null). Provide null to test trigger.
    String outboxPayload = '{' + "\"promoId\":\"" + promoId + "\"" + '}';
    jdbcTemplate.update("""
        INSERT INTO CPRS_OUTBOX_EVENTS (EVENT_ID, EVENT_TYPE, AGGREGATE_ID, PAYLOAD, STATUS)
        VALUES (NULL, 'PromoCreated', ?, ?, 'PENDING')
        """, promoId, outboxPayload);

    Integer outboxCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM CPRS_OUTBOX_EVENTS WHERE EVENT_TYPE = 'PromoCreated'", Integer.class);
    assertThat(outboxCount).isEqualTo(1);
  }

  private Integer tableExists(String name) {
    return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_tables WHERE table_name = ?",
        Integer.class, name);
  }
}
