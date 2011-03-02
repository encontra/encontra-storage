package pt.inevo.encontra.storage;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

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
    private CriteriaBuilder builder;
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
        return (T) entityManager.find(clazz, id);
    }

    @SuppressWarnings(value = "unchecked")
    @Override
    public T get(I id, String criteria) {
        if (criteria == null) {
            return (T) entityManager.find(clazz, id);
        } else {
            CriteriaBuilder builder = getStorageCriteriaBuilder();
            CriteriaQuery q = builder.createQuery(clazz);
            Root root = q.from(clazz);    //get the main root

            Class idType = root.getModel().getIdType().getJavaType();
            String idName = root.getModel().getId(idType).getName();

            String query = "select a from " + clazz.getName() + " a where " + idName + "=:id and ";
            query += criteria;
            TypedQuery criteriaQuery = entityManager.createQuery(query, clazz);
            criteriaQuery.setParameter("id", id);

            List result = criteriaQuery.getResultList();
            if (result.size() > 0) {
                return (T)result.get(0);
            }
//            //can't find the object
            return null;
        }
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

    public CriteriaBuilder getStorageCriteriaBuilder() {
        if (builder == null){
            builder = entityManager.getCriteriaBuilder();
        }
        return builder;
    }
}
