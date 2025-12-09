package khanh.careercoach.backend.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lớp cha Generic Repository cho DynamoDB.
 * Cung cấp các thao tác CRUD chuẩn, log và validate.
 * @param <T> Loại Entity (Ví dụ: UserEntity)
 */
public abstract class AbstractDynamoRepository<T> {

    // Logger chuẩn cho môi trường Production
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final DynamoDbTable<T> table;
    protected final String tableName;

    public AbstractDynamoRepository(DynamoDbEnhancedClient client, Class<T> type) {
        this.tableName = System.getenv("TABLE_NAME");

        // 1. Validate Config ngay khi khởi động
        if (this.tableName == null || this.tableName.isEmpty()) {
            logger.error("CRITICAL: Biến môi trường TABLE_NAME chưa được cấu hình!");
            throw new RuntimeException("Missing TABLE_NAME environment variable");
        }

        this.table = client.table(tableName, TableSchema.fromBean(type));
        logger.info("Initialized Repository for entity {} with table {}", type.getSimpleName(), tableName);
    }

    // ==================================================================================
    // 1. CREATE / UPDATE (Upsert)
    // ==================================================================================

    /**
     * Lưu hoặc cập nhật một Item.
     * Trong DynamoDB, putItem sẽ ghi đè toàn bộ item nếu PK/SK trùng.
     */
    public void save(T item) {
        if (item == null) {
            throw new IllegalArgumentException("Entity to save cannot be null");
        }

        try {
            logger.debug("Saving item to table {}: {}", tableName, item);
            table.putItem(item);
            logger.info("Successfully saved item.");
        } catch (DynamoDbException e) {
            logger.error("Failed to save item to DynamoDB: {}", e.getMessage(), e);
            throw new RuntimeException("Database Error: Could not save item", e);
        }
    }

    /**
     * Cập nhật item (Chỉ cập nhật các trường có giá trị, giữ nguyên các trường khác).
     * Yêu cầu Entity phải có đủ PK và SK.
     */
    public T update(T item) {
        if (item == null) {
            throw new IllegalArgumentException("Entity to update cannot be null");
        }
        try {
            logger.debug("Updating item in table {}: {}", tableName, item);
            // updateItem sẽ trả về item đã được update
            T updatedItem = table.updateItem(item);
            logger.info("Successfully updated item.");
            return updatedItem;
        } catch (DynamoDbException e) {
            logger.error("Failed to update item: {}", e.getMessage(), e);
            throw new RuntimeException("Database Error: Could not update item", e);
        }
    }

    // ==================================================================================
    // 2. READ (Find)
    // ==================================================================================

    public T findById(String pk, String sk) {
        // Validate input
        if (pk == null || pk.isEmpty() || sk == null || sk.isEmpty()) {
            logger.warn("FindById called with empty keys. PK: {}, SK: {}", pk, sk);
            return null; // Hoặc throw Exception tùy logic nghiệp vụ
        }

        try {
            Key key = Key.builder().partitionValue(pk).sortValue(sk).build();
            logger.debug("Fetching item with PK: {}, SK: {}", pk, sk);

            T item = table.getItem(key);

            if (item == null) {
                logger.info("Item not found for PK: {}, SK: {}", pk, sk);
            } else {
                logger.debug("Item found: {}", item);
            }
            return item;
        } catch (DynamoDbException e) {
            logger.error("Failed to find item: {}", e.getMessage(), e);
            throw new RuntimeException("Database Error: Could not fetch item", e);
        }
    }

    /**
     * Tìm tất cả các Item có cùng Partition Key (PK).
     * Đây là thao tác QUERY (Hiệu năng cao, rẻ tiền).
     * Ví dụ: Lấy tất cả Resume, Letter, Assessment của User X (PK=USER#X)
     */
    public List<T> findAllByPartitionKey(String pk) {
        if (pk == null || pk.isEmpty()) return new ArrayList<>();

        try {
            QueryConditional queryConditional = QueryConditional.keyEqualTo(
                    Key.builder().partitionValue(pk).build()
            );

            logger.debug("Querying items with PK: {}", pk);

            // SDK trả về Iterable (Lazy load), ta convert sang List để dễ dùng
            PageIterable<T> result = table.query(queryConditional);
            List<T> items = result.items().stream().collect(Collectors.toList());

            logger.info("Found {} items for PK: {}", items.size(), pk);
            return items;
        } catch (DynamoDbException e) {
            logger.error("Failed to query items by PK: {}", e.getMessage(), e);
            throw new RuntimeException("Database Error: Could not query items", e);
        }
    }

    /**
     * ⚠️ CẢNH BÁO: Quét toàn bộ bảng (SCAN).
     * Rất tốn kém Read Capacity Unit (RCU) và chậm nếu bảng lớn.
     * Chỉ dùng cho các bảng nhỏ (như Industry) hoặc debug.
     */
    public List<T> findAll() {
        try {
            logger.warn("PERFORMANCE WARNING: Executing full table SCAN on {}", tableName);
            return table.scan().items().stream().collect(Collectors.toList());
        } catch (DynamoDbException e) {
            logger.error("Failed to scan table: {}", e.getMessage(), e);
            throw new RuntimeException("Database Error: Could not scan table", e);
        }
    }

    // ==================================================================================
    // 3. DELETE
    // ==================================================================================

    public T delete(String pk, String sk) {
        if (pk == null || sk == null) {
            throw new IllegalArgumentException("Keys cannot be null for deletion");
        }

        try {
            Key key = Key.builder().partitionValue(pk).sortValue(sk).build();
            logger.info("Deleting item with PK: {}, SK: {}", pk, sk);

            // deleteItem trả về item cũ trước khi xóa (nếu có)
            return table.deleteItem(key);
        } catch (DynamoDbException e) {
            logger.error("Failed to delete item: {}", e.getMessage(), e);
            throw new RuntimeException("Database Error: Could not delete item", e);
        }
    }
}