package ru.qatools.mongodb;

import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.function.Consumer;

import static com.mongodb.CursorType.TailableAwait;
import static java.lang.Thread.sleep;
import static java.util.stream.StreamSupport.stream;
import static ru.qatools.mongodb.util.SerializeUtil.serializeToBytes;

/**
 * @author Ilya Sadykov
 */
public class MongoTailingQueue<T extends Serializable> implements TailingQueue<T>, MongoBasicStorage<T> {
    public static final long DEFAULT_MAX_SIZE = 1000L;
    public static final long ASSUMED_MAX_DOC_SIZE = 1024L * 1024L; // 1mb
    public static final int BATCH_SIZE = 100;
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoTailingQueue.class);
    public static final int SLEEP_BETWEEN_FAILURES_MS = 500;
    final Class<T> entityClass;
    final MongoClient mongo;
    final String dbName;
    final String queueName;
    final long maxSize;
    volatile boolean stopped = false;

    public MongoTailingQueue(Class<T> entityClass, MongoClient mongo, String dbName, String queueName) {
        this(entityClass, mongo, dbName, queueName, DEFAULT_MAX_SIZE);
    }

    public MongoTailingQueue(Class<T> entityClass, MongoClient mongo, String dbName, String queueName, long maxSize) {
        this.entityClass = entityClass;
        this.mongo = mongo;
        this.dbName = dbName;
        this.queueName = queueName;
        this.maxSize = maxSize;
    }

    @Override
    public void drop() {
        collection().drop();
    }

    @Override
    public void init() {
        if (stream(db().listCollectionNames().spliterator(), false)
                .filter(s -> s.equals(queueName)).count() == 0) {
            db().createCollection(queueName,
                    new CreateCollectionOptions().capped(true)
                            .maxDocuments(maxSize)
                            .sizeInBytes(ASSUMED_MAX_DOC_SIZE * maxSize)
                            .autoIndex(true));
        }
    }

    @Override
    public void stop() {
        this.stopped = true;
    }

    @Override
    public void poll(Consumer<T> consumer) {
        if (stopped) {
            LOGGER.warn("Could not stopped queue {}.{}", dbName, queueName);
        }
        while (!stopped) {
            try {
                stream(collection().find()
                        .cursorType(TailableAwait)
                        .noCursorTimeout(true)
                        .batchSize(BATCH_SIZE).spliterator(), false)
                        .forEach(doc -> consumer.accept(getObject(doc)));
            } catch (MongoException e) {
                LOGGER.debug("Failed to iterate on queue cursor for {}.{}", dbName, queueName, e);
                try {
                    sleep(SLEEP_BETWEEN_FAILURES_MS);
                } catch (InterruptedException e1) {
                    LOGGER.warn("Poll sleeping after cursor failure was interrupted", e);
                }
            }
        }
    }

    @Override
    public void add(T object) {
        collection().insertOne(new Document("object", serializeToBytes(object)));
    }

    @Override
    public long size() {
        return collection().count();
    }

    private MongoCollection<Document> collection() {
        return db().getCollection(queueName);
    }

    private MongoDatabase db() {
        return mongo.getDatabase(dbName);
    }
}
