package pt.inevo.encontra.storage;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import javax.persistence.*;
import javax.persistence.criteria.*;


/**
 * Abstract implementation of generic DAO.
 *
 * @param <T> entity type, it must implements at least <code>IEntity</code>
 * @param <I> entity's primary key, it must be serializable
 * @see IEntity
 */
public class JPAObjectStorage<I extends Serializable,T extends IEntity<I>> implements ObjectStorage<I,T> {

    private EntityManager entityManager;

    private Class<IEntity<I>> clazz;


    /**
     * Default constructor. Use for extend this class.
     */
    @SuppressWarnings(value = "unchecked")
    public JPAObjectStorage() {

        Type[] types = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments();

        if (types[1] instanceof ParameterizedType) {
            // If the class has parameterized types, it takes the raw type.
            ParameterizedType type = (ParameterizedType) types[1];
            clazz = (Class<IEntity<I>>) type.getRawType();
        } else {
            clazz = (Class<IEntity<I>>) types[1];
        }
    }

    /**
     * Constructor with given {@link IEntity} implementation. Use for creting DAO without extending
     * this class.
     *
     * @param clazz class with will be accessed by DAO methods
     */
    @SuppressWarnings(value = "unchecked")
    public JPAObjectStorage(Class<IEntity<I>> clazz) {
        this.clazz = clazz;
    }

    /**
     * Set entity manager.
     *
     * @param entityManager entity manager
     */
    @PersistenceContext
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @SuppressWarnings(value = "unchecked")
    @Override
    public T get(I id) {
        //        return (T) entityManager.find(clazz, id);

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery criteria = builder.createQuery(clazz);
        Root root=criteria.from(clazz);
        criteria.select(root);
        criteria.where(builder.equal(root.get("id"), id));

        List result = entityManager.createQuery(criteria).getResultList();
        if (result.size() > 0) {
            return (T)result.get(0);
        }
        //can't find the object
        return null;
    }

    @Override
    public T save(final T object) {
        T res=null;
        EntityTransaction tx = entityManager.getTransaction();
        tx.begin();

        res=entityManager.merge(object);

        tx.commit();

        return res;
    }

    @Override
    public void save(final T... objects) {
        for (T object : objects) {
            if (object.getId() != null) {
                entityManager.merge(object);
            } else {
                entityManager.persist(object);
            }
        }
    }


    @Override
    public void delete(final T object)  {
        entityManager.remove(object);
    }



    public void refresh(final T entity) {
        entityManager.refresh(entity);
    }


    public void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * Get entity manager.
     *
     * @return entity manager
     */
    protected EntityManager getEntityManager() {
        return entityManager;
    }
}
