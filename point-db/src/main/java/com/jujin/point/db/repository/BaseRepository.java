package com.jujin.point.db.repository;

import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.Orm;
import com.jujin.freeway.db.Query;

import java.util.List;
import java.util.Optional;

/**
 * Base repository providing common CRUD operations backed by freeway's Database + Orm.
 *
 * @param <T> the entity type
 */
public abstract class BaseRepository<T> {

    protected final Database db;
    protected final Orm orm;
    protected final Class<T> entityType;

    protected BaseRepository(Database db, Class<T> entityType) {
        this.db = db;
        this.orm = Orm.of(db);
        this.entityType = entityType;
    }

    /** Find by ID */
    public Optional<T> findById(Object id) {
        return orm.findById(entityType, id);
    }

    /** Find all records */
    public List<T> findAll() {
        return orm.findAll(entityType);
    }

    /** Insert a new record */
    public void insert(T entity) {
        orm.insert(entity);
    }

    /** Update an existing record */
    public void update(T entity) {
        orm.update(entity);
    }

    /** Delete by ID */
    public void deleteById(Object id) {
        orm.deleteById(entityType, id);
    }

    /** Create a raw query */
    protected Query query(String sql, Object... params) {
        return db.query(sql, params);
    }

    /** Execute raw SQL */
    protected int execute(String sql, Object... params) {
        var result = db.execute(sql, params);
        return (int) result.rows();
    }

    /** Count records */
    public long count() {
        var list = orm.findAll(entityType);
        return list.size();
    }
}
