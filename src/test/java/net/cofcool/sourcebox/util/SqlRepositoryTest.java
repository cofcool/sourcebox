package net.cofcool.sourcebox.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.sql.JDBCType;
import java.time.LocalDateTime;
import java.util.List;
import net.cofcool.sourcebox.BaseTest;
import net.cofcool.sourcebox.logging.LoggerFactory;
import net.cofcool.sourcebox.util.TableInfoHelper.Column;
import net.cofcool.sourcebox.util.TableInfoHelper.Entity;
import net.cofcool.sourcebox.util.TableInfoHelper.ID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class SqlRepositoryTest extends BaseTest {

    static SqlRepository<User> repository;

    @BeforeAll
    static void create(Vertx vertx, VertxTestContext testContext) {
        LoggerFactory.setDebug(true);
        SqlRepository.init(vertx);
        repository = SqlRepository.create(User.class);
        repository.save(new User("1", "test1", "12345"))
            .onComplete(testContext.succeeding(t -> testContext.completeNow()));
    }

    @Test
    void save(Vertx vertx, VertxTestContext testContext) {
        repository
            .save(new User("2", "test2", "12345"))
            .onComplete(a -> repository.find("2"))
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                assertNotNull(r);
                testContext.completeNow();
            })));
    }

    @Test
    void save1(Vertx vertx, VertxTestContext testContext) {
        repository
            .save(new User("1", "test21", "12345"))
            .onComplete(a -> repository.find("1"))
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                assertNotNull(r);
                System.out.println(r);
                assertEquals("test21", r.name());
                testContext.completeNow();
            })));
    }

    @Test
    void saveAll(Vertx vertx, VertxTestContext testContext) {
        repository.save(List.of(new User("14", "test4", "asdasd"), new User("5", "test5", "asdasdasdasd")))
            .onComplete(testContext.succeeding(r -> testContext.verify(testContext::completeNow)));
    }

    @Test
    void delete(Vertx vertx, VertxTestContext testContext) {
        repository
            .save(new User("3", "test3", "12345"))
            .compose(u -> repository.delete(u.id))
            .compose(u -> repository.find("3"))
            .onFailure(r -> testContext.verify(testContext::completeNow));
    }

    @Test
    void find(Vertx vertx, VertxTestContext testContext) {
         repository
             .find()
             .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                 assertFalse(r.isEmpty());
                 testContext.completeNow();
             })));
    }

    @Test
    void find1(Vertx vertx, VertxTestContext testContext) {
        repository
            .find(new User("1", null, null))
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                assertFalse(r.isEmpty());
                testContext.completeNow();
            })));
    }

    @Test
    void testFind1(Vertx vertx, VertxTestContext testContext) {
        repository
            .find("1")
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                assertNotNull(r);
                testContext.completeNow();
            })));
    }

    @Test
    void testFindCondition(Vertx vertx, VertxTestContext testContext) {
        repository
            .find(QueryBuilder.builder()
                .select("*")
                .from("user")
                .and("name like 'test%'"))
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                assertNotNull(r);
                testContext.completeNow();
            })));
    }

    @Test
    void testFindCondition1(Vertx vertx, VertxTestContext testContext) {
        repository
            .find(QueryBuilder.builder()
                .select()
                .from(User.class))
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                assertNotNull(r);
                testContext.completeNow();
            })));
    }

    @Test
    void executeQuery(Vertx vertx, VertxTestContext testContext) {
        repository
            .count(QueryBuilder.builder().from("user").count())
            .onComplete(testContext.succeeding(r -> testContext.verify(() -> {
                assertEquals(1, r);
                testContext.completeNow();
            })));
    }

    @Entity(name = "user")
    record User(
        @ID
        @Column(name = "id", type = JDBCType.CHAR, length = 5)
        String id,
        @Column(name = "name", type = JDBCType.CHAR, length = 30)
        String name,
        @Column(name = "pwd", type = JDBCType.CHAR, length = 50)
        String pwd,
        @Column(name = "time", type = JDBCType.TIMESTAMP)
        LocalDateTime time
    ) implements EntityAction<User> {

        @Override
        public User beforeUpdate() {
            return new User(id, name, pwd, LocalDateTime.now());
        }

        @Override
        public User beforeInsert() {
            return new User(id, name, pwd, time == null ? LocalDateTime.now() : time);
        }

        public User(String id, String name, String pwd) {
            this(id, name, pwd, null);
        }
    }
}