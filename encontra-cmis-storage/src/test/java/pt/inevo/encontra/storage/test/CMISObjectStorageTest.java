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
import org.apache.chemistry.opencmis.client.bindings.CmisBindingFactory;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectParentData;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.BindingsObjectFactoryImpl;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.spi.AclService;
import org.apache.chemistry.opencmis.commons.spi.BindingsObjectFactory;
import org.apache.chemistry.opencmis.commons.spi.CmisBinding;
import org.apache.chemistry.opencmis.commons.spi.DiscoveryService;
import org.apache.chemistry.opencmis.commons.spi.MultiFilingService;
import org.apache.chemistry.opencmis.commons.spi.NavigationService;
import org.apache.chemistry.opencmis.commons.spi.ObjectService;
import org.apache.chemistry.opencmis.commons.spi.RepositoryService;
import org.apache.chemistry.opencmis.commons.spi.VersioningService;
import org.apache.chemistry.opencmis.inmemory.server.InMemoryServiceFactoryImpl;
import org.apache.chemistry.opencmis.inmemory.storedobj.impl.ContentStreamDataImpl;
import org.apache.chemistry.opencmis.inmemory.DummyCallContext;
import org.junit.Test;
import pt.inevo.encontra.query.criteria.StorageCriteria;
import pt.inevo.encontra.storage.CMISObjectStorage;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.util.*;

public class CMISObjectStorageTest extends TestCase {

    private CMISObjectStorage<MyCMISObject> storage;

    protected static final String REPOSITORY_ID = "UnitTestRepository";
    protected BindingsObjectFactory fFactory = new BindingsObjectFactoryImpl();
    protected String fRootFolderId;
    protected String fRepositoryId;
    protected ObjectService fObjSvc;
    protected NavigationService fNavSvc;
    protected RepositoryService fRepSvc;
    protected VersioningService fVerSvc;
    protected MultiFilingService fMultiSvc;
    protected DiscoveryService fDiscSvc;
    protected AclService fAclSvc;
    protected CallContext fTestCallContext;
    private String fTypeCreatorClassName;
    Map<String, String> parameters = new HashMap<String, String>();
    
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
            caps.setSupportsVersionSpecificFiling(true);

            RepositoryInfoImpl repositoryInfo = new RepositoryInfoImpl();
            repositoryInfo.setId(REPOSITORY_ID);
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
    
    }

    public CMISObjectStorageTest(String name) {
        super(name);
    }


    protected void initializeServices(Map<String, String> parameters) {
        
        // add parameters for local binding:
        parameters.put(SessionParameter.BINDING_SPI_CLASS, SessionParameter.LOCAL_FACTORY);
        parameters.put(SessionParameter.LOCAL_FACTORY, InMemoryServiceFactoryImpl.class.getName());
        parameters.put(ConfigConstants.OVERRIDE_CALL_CONTEXT, "true");
        InMemoryServiceFactoryImpl.setOverrideCallContext(fTestCallContext);
        

        // get factory and create binding
        CmisBindingFactory factory = CmisBindingFactory.newInstance();
        CmisBinding binding = factory.createCmisLocalBinding(parameters);
        assertNotNull(binding);
        fFactory = binding.getObjectFactory();
        fRepSvc = binding.getRepositoryService();
        fObjSvc = binding.getObjectService();
        fNavSvc = binding.getNavigationService();
        fVerSvc = binding.getVersioningService();
        fMultiSvc = binding.getMultiFilingService();
        fDiscSvc = binding.getDiscoveryService();
        fAclSvc = binding.getAclService();
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // 
        parameters.put(ConfigConstants.TYPE_CREATOR_CLASS, DefaultTypeSystemCreator.class.getName());
        parameters.put(ConfigConstants.REPOSITORY_ID, REPOSITORY_ID);
        parameters.put(ConfigConstants.REPOSITORY_INFO_CREATOR_CLASS, UnitTestRepositoryInfo.class.getName());

        parameters.put(SessionParameter.REPOSITORY_ID, REPOSITORY_ID);
         
        fTestCallContext = new DummyCallContext();
             
        initializeServices(parameters);
         
        assertNotNull(fRepSvc);
        assertNotNull(fObjSvc);
        assertNotNull(fNavSvc);

        RepositoryInfo rep = fRepSvc.getRepositoryInfo(REPOSITORY_ID, null);
        fRootFolderId = rep.getRootFolderId();
        fRepositoryId = rep.getId();

        assertNotNull(fRepositoryId);
        assertNotNull(fRootFolderId);

        storage = new MyObjectStorage(parameters);
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

        //obj.dumpObject();

        //StorageCriteria criteria = new StorageCriteria("cmis:contentStreamFileName like '%Object%'");
        //boolean valid = storage.validate(obj.getId(), criteria);
        //assertTrue(valid);
    }
}
