package pt.inevo.encontra.storage.test;

import junit.framework.TestCase;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.enums.*;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RepositoryCapabilitiesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RepositoryInfoImpl;
import org.apache.chemistry.opencmis.inmemory.ConfigConstants;
import org.apache.chemistry.opencmis.inmemory.RepositoryInfoCreator;
import org.apache.chemistry.opencmis.inmemory.server.InMemoryServiceFactoryImpl;
import org.apache.chemistry.opencmis.inmemory.types.DefaultTypeSystemCreator;
import org.junit.Test;
import pt.inevo.encontra.query.criteria.StorageCriteria;
import pt.inevo.encontra.storage.CMISObjectStorage;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.util.*;

public class CMISObjectStorageTest extends TestCase {

    private CMISObjectStorage<MyCMISObject> storage;

    public static class UnitTestRepositoryInfo implements RepositoryInfoCreator {

        public RepositoryInfo createRepositoryInfo() {
            RepositoryCapabilitiesImpl caps = new RepositoryCapabilitiesImpl();
            caps.setAllVersionsSearchable(false);
            caps.setCapabilityAcl(CapabilityAcl.NONE);
            caps.setCapabilityChanges(CapabilityChanges.NONE);
            caps.setCapabilityContentStreamUpdates(CapabilityContentStreamUpdates.ANYTIME);
            caps.setCapabilityJoin(CapabilityJoin.NONE);
            caps.setCapabilityQuery(CapabilityQuery.NONE);
            caps.setCapabilityRendition(CapabilityRenditions.NONE);
            caps.setIsPwcSearchable(false);
            caps.setIsPwcUpdatable(true);
            caps.setSupportsGetDescendants(true);
            caps.setSupportsGetFolderTree(true);
            caps.setSupportsMultifiling(false);
            caps.setSupportsUnfiling(true);
            caps.setSupportsVersionSpecificFiling(false);

            RepositoryInfoImpl repositoryInfo = new RepositoryInfoImpl();
            repositoryInfo.setId("A1");
            repositoryInfo.setName("InMemory Repository");
            repositoryInfo.setDescription("InMemory Test Repository");
            repositoryInfo.setCmisVersionSupported("0.7");
            repositoryInfo.setCapabilities(caps);
            repositoryInfo.setRootFolder("/");
            repositoryInfo.setAclCapabilities(null);
            repositoryInfo.setPrincipalAnonymous("anonymous");
            repositoryInfo.setPrincipalAnyone("anyone");
            repositoryInfo.setThinClientUri(null);
            repositoryInfo.setChangesIncomplete(Boolean.TRUE);
            repositoryInfo.setChangesOnType(null);
            repositoryInfo.setLatestChangeLogToken(null);
            repositoryInfo.setVendorName("OpenCMIS");
            repositoryInfo.setProductName("OpenCMIS Client");
            repositoryInfo.setProductVersion("0.1");
            return repositoryInfo;
        }
    }

    public class MyObjectStorage extends CMISObjectStorage<MyCMISObject> {

        MyObjectStorage(Map<String, String> parameters) {
            super(parameters);
        }

        @Override
        protected void init(Map<String, String> parameters) {
            SessionFactory factory = SessionFactoryImpl.newInstance();
            Map<String, String> parameter = new HashMap<String, String>();

            // user credentials
            parameter.put(SessionParameter.USER, "dummyuser");
            parameter.put(SessionParameter.PASSWORD, "dummysecret");

            // connection settings
            parameter.put(SessionParameter.BINDING_TYPE, BindingType.LOCAL.value());
            parameters.put(SessionParameter.BINDING_SPI_CLASS, SessionParameter.LOCAL_FACTORY);
            parameter.put(SessionParameter.LOCAL_FACTORY, InMemoryServiceFactoryImpl.class.getName());
            parameter.put(ConfigConstants.TYPE_CREATOR_CLASS, UnitTestRepositoryInfo.class.getName());
            parameter.put(ConfigConstants.REPOSITORY_INFO_CREATOR_CLASS, UnitTestRepositoryInfo.class.getName());
            parameter.put(SessionParameter.REPOSITORY_ID, "A1");

            // create session
            Session session = factory.createSession(parameter);
            System.out.println();
        }
    }

    public CMISObjectStorageTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Properties properties = new Properties();
        properties.load(new FileInputStream("src/test/resources/config.properties"));

        Map<String, String> parameter = new HashMap<String, String>();
        Enumeration e = properties.propertyNames();
        while (e.hasMoreElements()) {
            String propertyName = e.nextElement().toString();
            parameter.put(propertyName, properties.getProperty(propertyName));
        }

        storage = new MyObjectStorage(parameter);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testStored() {
        int max = 10;
        int test = new Random().nextInt(max);
        String id = null;
        MyCMISObject obj;
        for (int i = 0; i < max; i++) {
            obj = new MyCMISObject();
            obj.setName("A Test Object");
            obj.setDescription("Object" + i + " description");
            obj.setImage(new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB));
            obj = storage.save(obj);
            if (i == test)
                id = obj.getId();
        }

        obj = storage.get(id);
        assertNotNull(obj);
        assertEquals(id, obj.getId());

        obj.dumpObject();

        StorageCriteria criteria = new StorageCriteria("cmis:contentStreamFileName like '%Object%'");
        boolean valid = storage.validate(obj.getId(), criteria);
        assertTrue(valid);
    }
}
