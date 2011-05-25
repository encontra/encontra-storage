package pt.inevo.encontra.storage;

import org.apache.commons.io.IOUtils;
import org.apache.chemistry.opencmis.client.api.*;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import pt.inevo.encontra.query.criteria.StorageCriteria;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.criteria.*;

/**
 * Abstract implementation of generic DAO.
 *
 * @param <T> entity type, it must implements at least <code>IEntity</code>
 * @see IEntity
 */
public class CMISObjectStorage<T extends CmisObject> implements ObjectStorage<String, T> {

    private CriteriaBuilder builder;
    private Class<T> clazz;
    private static Session session;
    private Folder cmisObjectStorage;

    static {
        // default factory implementation
        SessionFactory factory = SessionFactoryImpl.newInstance();
        Map<String, String> parameter = new HashMap<String, String>();

        // user credentials
        parameter.put(SessionParameter.USER, "");
        parameter.put(SessionParameter.PASSWORD, "");

        // connection settings
        parameter.put(SessionParameter.ATOMPUB_URL, "http://localhost:8080/chemistry-opencmis-server-inmemory-0.3.0/atom");
        parameter.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
        parameter.put(SessionParameter.REPOSITORY_ID, "A1");

        // create session
        session = factory.createSession(parameter);
    }

    /**
     * Default constructor. Use for extend this class.
     */
    @SuppressWarnings(value = "unchecked")
    public CMISObjectStorage() {

        Type[] types = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments();

        if (types[0] instanceof ParameterizedType) {
            // If the class has parameterized types, it takes the raw type.
            ParameterizedType type = (ParameterizedType) types[0];
            clazz = (Class<T>) type.getRawType();
        } else {
            clazz = (Class<T>) types[0];
        }
        init();
    }

    /**
     * Constructor with given {@link IEntity} implementation. Use for creating DAO without extending
     * this class.
     *
     * @param clazz class with will be accessed by DAO methods
     */
    @SuppressWarnings(value = "unchecked")
    public CMISObjectStorage(Class<T> clazz) {
        this.clazz = clazz;
        init();
    }

    /**
     * Creates a folder to hold the CmisObjects that will be created in the CMIS.
     */
    private void init() {
        cmisObjectStorage = session.getRootFolder();

        // (minimal set: name and object type id)
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:folder");
        properties.put(PropertyIds.NAME, "CMISObjectStorage");

        // create the folder
        cmisObjectStorage = cmisObjectStorage.createFolder(properties);
    }

    @SuppressWarnings(value = "unchecked")
    @Override
    public T get(String id) {

        ObjectId objId = session.createObjectId(id);
        Document document = (Document)session.getObject(objId);

        InputStream contentStream = document.getContentStream().getStream();

        long length = 0;
        try {
            byte [] bytes = IOUtils.toByteArray(contentStream);
            T object = clazz.newInstance();
            object.setContent(bytes);
            object.setId(id);

            return object;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean validate(String id, StorageCriteria criteria){
        //TO DO
        return false;
    }

    @Override
    public List<String> getValidIds(StorageCriteria criteria){
        //TO DO
        return null;
    }

    @Override
    public T save(final T object) {
        //saving the object on the Cmis
        // properties
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
        properties.put(PropertyIds.NAME, "dummyName");

        //set the object properties
        Map<String, Object> objectProperties = object.getProperties();
        Set<Map.Entry<String, Object>> entries = objectProperties.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            properties.put(entry.getKey(), entry.getValue());
        }

        byte [] content = object.getContent();
        InputStream stream = new ByteArrayInputStream(content);
        ContentStream contentStream = session.getObjectFactory().createContentStream("dummyName", content.length, "application/x-binary", stream);

        //get the folder id
        ObjectId folderId = session.createObjectId(cmisObjectStorage.getId());

        //TO DO - document must be versionable
        ObjectId objectId = session.createDocument(properties, folderId, contentStream, VersioningState.NONE);
        String id = objectId.getId();

        //update the name of the newly saved cmis object
        org.apache.chemistry.opencmis.client.api.CmisObject cmisObject = session.getObject(objectId);
        Map<String, Object> updateproperties = new HashMap<String, Object>();
        updateproperties.put(PropertyIds.NAME, id);
        cmisObject.updateProperties(updateproperties);

        object.setId(id);

        return object;
    }

    @Override
    public void save(final T... objects) {
        //TO DO
        return;
    }

    @Override
    public void delete(final T object)  {
        //TO DO
        return;
    }

    public void refresh(final T entity) {
        //TO DO
        return;
    }

    public void flushAndClear() {
        //TO DO
        return;
    }
}
