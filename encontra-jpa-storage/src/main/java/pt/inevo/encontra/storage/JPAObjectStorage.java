package pt.inevo.encontra.storage;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;


/**
 * Abstract implementation of generic DAO.
 * 
 * @param <T> entity type, it must implements at least <code>IEntity</code>
 * @param <I> entity's primary key, it must be serializable
 * @see IEntity
 */
public class JPAObjectStorage<I extends Serializable,T extends IEntity<I>> implements IDao<T, I> {

  private EntityManager entityManager;

  private Class<IEntity<I>> clazz;

  private boolean isDefaultable;

  private boolean isActivable;

  private boolean isHiddenable;

  private boolean isInheritable;

  /**
   * Default constructor. Use for extend this class.
   */
  @SuppressWarnings(value = "unchecked")
  public GenericDao() {

    Type[] types = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments();

    if (types[0] instanceof ParameterizedType) {
      // If the class has parameterized types, it takes the raw type.
      ParameterizedType type = (ParameterizedType) types[0];
      clazz = (Class<IEntity<I>>) type.getRawType();
    } else {
      clazz = (Class<IEntity<I>>) types[0];
    }
    checkGenericClass();
  }

  /**
   * Constructor with given {@link IEntity} implementation. Use for creting DAO without extending
   * this class.
   * 
   * @param clazz class with will be accessed by DAO methods
   */
  @SuppressWarnings(value = "unchecked")
  public GenericDao(Class<IEntity<I>> clazz) {
    this.clazz = clazz;
    checkGenericClass();
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
  public T load(I id) throws EntityNotFoundException {
    T entity = get(id);
    if (entity == null) {
      throw new EntityNotFoundException("entity " + clazz + "#" + id + " was not found");
    }
    return entity;
  }

  @SuppressWarnings(value = "unchecked")
  @Override
  public T get(I id) {
    return (T) entityManager.find(clazz, id);
  }

  @SuppressWarnings(value = "unchecked")
  @Override
  public List<T> get(I... ids) {
    return findByCriteria(Criteria.forClass(clazz).add(Restrictions.in(IEntity.P_ID, ids)));
  }

  @SuppressWarnings(value = "unchecked")
  @Override
  public List<T> get(IInheritable<T> parent) {
    if (parent == null) {
      return findByCriteria(Criteria.forClass(clazz)
                                    .add(Restrictions.isNull(IInheritable.P_PARENT)));
    } else {
      return findByCriteria(Criteria.forClass(clazz).add(Restrictions.eq(IInheritable.P_PARENT,
                                                                         parent)));
    }
  }

  @SuppressWarnings(value = "unchecked")
  @Override
  public List<T> getAll() {
    return findByCriteria(Criteria.forClass(clazz));
  }

  @SuppressWarnings(value = "unchecked")
  @Override
  public List<T> findByExample(T example) {
    Criteria criteria = Criteria.forClass(clazz);

    Field[] fields = example.getClass().getDeclaredFields();

    for (Field field : fields) {
      if (field.getName().equals(IEntity.P_ID)) {
        continue;
      }
      if (field.getName().equals(IActivable.P_IS_ACTIVE)) {
        continue;
      }
      if (field.getName().equals(IDefaultable.P_IS_DEFAULT)) {
        continue;
      }
      if (field.getName().equals(IHiddenable.P_IS_HIDDEN)) {
        continue;
      }
      if (!field.isAnnotationPresent(Column.class) && !field.isAnnotationPresent(Basic.class)) {
        continue;
      }

      Object value = null;

      try {
        field.setAccessible(true);
        value = field.get(example);
      } catch (IllegalArgumentException e) {
        continue;
      } catch (IllegalAccessException e) {
        continue;
      }

      if (value == null) {
        continue;
      }

      criteria.add(Restrictions.eq(field.getName(), value));
    }

    if (example instanceof IHiddenable) {
      if (((IInheritable) example).getParent() == null) {
        criteria.add(Restrictions.isNull(IInheritable.P_PARENT));
      } else {
        criteria.add(Restrictions.eq(IInheritable.P_PARENT, ((IInheritable) example).getParent()));
      }
    }

    return findByCriteria(criteria);
  }

  @SuppressWarnings(value = "unchecked")
  @Override
  public void setAsDefault(IDefaultable object) {
    if (object.getExample() != null) {
      List<T> objects = findByExample((T) object.getExample());
      for (T o : objects) {
        if (object != o) {
          ((IDefaultable) o).setDefault(false);
          entityManager.merge(o);
        }
      }
    }
    object.setDefault(true);

    if (((T) object).getId() != null) {
      entityManager.merge(object);
    } else {
      entityManager.persist(object);
    }
  }

  @Override
  public void save(final T object) {
    if (object.getId() != null) {
      entityManager.merge(object);
    } else {
      entityManager.persist(object);
    }
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
  public void delete(final I id) throws UnsupportedOperationException {
    delete(load(id));
  }

  @Override
  public void delete(final I... ids) throws UnsupportedOperationException {
    deleteAll(get(ids), true);
  }

  @Override
  public void delete(final T object) throws UnsupportedOperationException {
    if (isDefaultable) {
      checkIfDefault(object);
    }
    if (isHiddenable) {
      ((IHiddenable) object).setHidden(true);
      entityManager.merge(object);
    } else {
      entityManager.remove(object);
    }
  }

  @Override
  public void delete(final T... objects) throws UnsupportedOperationException {
    deleteAll(Arrays.asList(objects), true);
  }

  @Override
  public void deleteAll() throws UnsupportedOperationException {
    deleteAll(getAll(), false);
  }

  private void deleteAll(final Collection<T> objects, boolean checkIdDefault)
    throws UnsupportedOperationException
  {
    if (checkIdDefault) {
      if (isDefaultable) {
        for (T object : objects) {
          checkIfDefault(object);
        }
      }
    }
    if (isHiddenable) {
      for (T object : objects) {
        ((IHiddenable) object).setHidden(true);
        entityManager.merge(object);
      }
    } else {
      for (T object : objects) {
        entityManager.remove(object);
      }
    }
  }

  private void checkIfDefault(T entity) {
    if (((IDefaultable) entity).isDefault()) {
      throw new UnsupportedOperationException("can't delete default entity " + clazz + "#"
                                              + entity.getId());
    }
  }

  private void checkGenericClass() {
    for (Class i : clazz.getInterfaces()) {
      if (i == IDefaultable.class) {
        isDefaultable = true;
      } else if (i == IActivable.class) {
        isActivable = true;
      } else if (i == IHiddenable.class) {
        isHiddenable = true;
      } else if (i == IInheritable.class) {
        isInheritable = true;
      }
    }
  }

  @Override
  public void refresh(final T entity) {
    entityManager.refresh(entity);
  }

  @Override
  public void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }

  /**
   * Retrieve objects using criteria. It is equivalent to <code>criteria.list(entityManager)</code>.
   * 
   * @param criteria criteria which will be executed
   * @return list of founded objects
   * @see javax.persistence.Query#getResultList()
   */
  @SuppressWarnings(value = "unchecked")
  protected List findByCriteria(Criteria criteria) {
    return criteria.list(entityManager);
  }

  /**
   * Retrieve an unique object using criteria. It is equivalent to
   * <code>criteria.uniqueResult(entityManager)</code>.
   * 
   * @param criteria criteria which will be executed
   * @return retrieved object
   * @throws NoResultException - if there is no result
   * @throws NonUniqueResultException - if more than one result
   * @see javax.persistence.Query#getSingleResult()
   */
  protected Object findUniqueByCriteria(Criteria criteria)
    throws NonUniqueResultException,
      NoResultException
  {
    return criteria.uniqueResult(entityManager);
  }

  @Override
  public boolean isActivable() {
    return isActivable;
  }

  @Override
  public boolean isDefaultable() {
    return isDefaultable;
  }

  @Override
  public boolean isHiddenable() {
    return isHiddenable;
  }

  @Override
  public boolean isInheritable() {
    return isInheritable;
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
